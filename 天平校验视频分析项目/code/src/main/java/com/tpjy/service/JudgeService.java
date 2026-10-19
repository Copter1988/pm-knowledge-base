package com.tpjy.service;

import com.tpjy.common.Constants;
import com.tpjy.dto.JudgeResult;
import com.tpjy.dto.ReadingJudge;
import com.tpjy.dto.RecognizedReading;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 校验判定引擎（核心业务规则，SRS §3.1.5）。
 * 全部使用 BigDecimal，限度实时计算，绝不引用写死值（如 xlsx 中的限度栏）。
 *
 * <p>本类为纯函数式实现（无数据库依赖），可单测、可被命令行直接调用。
 */
@Service
public class JudgeService {

    /** 示值归组：seq 1~3→L1、4~6→L2、7~9→L3（与量程无关，规则 4）。 */
    public String groupLevel(int seq) {
        if (seq < 1 || seq > 9) {
            throw new IllegalArgumentException("示值序号必须在 1~9 之间：" + seq);
        }
        if (seq <= 3) {
            return Constants.LEVEL_L1;
        }
        if (seq <= 6) {
            return Constants.LEVEL_L2;
        }
        return Constants.LEVEL_L3;
    }

    /** 限度下界 = 标称值 × (1 - 0.05%)。 */
    public BigDecimal lowerLimit(BigDecimal nominal) {
        return nominal.multiply(BigDecimal.ONE.subtract(new BigDecimal(Constants.LIMIT_RATE)));
    }

    /** 限度上界 = 标称值 × (1 + 0.05%)。 */
    public BigDecimal upperLimit(BigDecimal nominal) {
        return nominal.multiply(BigDecimal.ONE.add(new BigDecimal(Constants.LIMIT_RATE)));
    }

    /** 单次示值是否在限度内。 */
    public boolean within(BigDecimal value, BigDecimal nominal) {
        if (value == null || nominal == null) {
            return false;
        }
        return value.compareTo(lowerLimit(nominal)) >= 0
                && value.compareTo(upperLimit(nominal)) <= 0;
    }

    /** 逐示值判定：任一示值超出限度即不通过（规则 2）。 */
    public boolean allReadingsPass(List<BigDecimal> values, List<BigDecimal> nominals) {
        if (values == null || nominals == null || values.isEmpty()
                || values.size() != nominals.size()) {
            return false;
        }
        for (int i = 0; i < values.size(); i++) {
            if (!within(values.get(i), nominals.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 总体判定：前置条件一票否决；否则逐示值判定（规则 2/3/6）。
     *
     * @param cleanOk        清洁是否符合要求
     * @param levelOk        水平调节是否到位
     * @param internalCalibOk 内部校正是否通过：是 / 否 / NA（NA 不算不通过）
     */
    public boolean judge(boolean cleanOk, boolean levelOk, String internalCalibOk,
                         List<BigDecimal> values, List<BigDecimal> nominals) {
        if (!cleanOk || !levelOk || !calibrationPasses(internalCalibOk)) {
            return false;
        }
        return allReadingsPass(values, nominals);
    }

    /**
     * 较大误差预填（阈值 1%），仅提示、不参与 ±0.05% 判定（规则 7）。
     * 返回 null 表示无需预填。
     */
    public String bigDeviationRemark(int seq, BigDecimal value, BigDecimal nominal) {
        if (value == null || nominal == null || nominal.signum() == 0) {
            return null;
        }
        BigDecimal dev = value.subtract(nominal).abs()
                .divide(nominal, 6, RoundingMode.HALF_UP);
        if (dev.compareTo(new BigDecimal(Constants.BIG_DEVIATION_RATE)) > 0) {
            return "第" + seq + "次示值偏差 "
                    + dev.movePointRight(2).setScale(2, RoundingMode.HALF_UP)
                    + "%，必要时报修";
        }
        return null;
    }

    /** 偏差百分比（%），保留 2 位；null 表示无法计算。 */
    public BigDecimal deviationPercent(BigDecimal value, BigDecimal nominal) {
        if (value == null || nominal == null || nominal.signum() == 0) {
            return null;
        }
        return value.subtract(nominal).abs()
                .divide(nominal, 6, RoundingMode.HALF_UP)
                .movePointRight(2).setScale(2, RoundingMode.HALF_UP);
    }

    /** 按分组取标称值：L1/L2/L3 → 天平主数据里的三个测量砝码。 */
    public BigDecimal nominalOf(String level, BigDecimal w1, BigDecimal w2, BigDecimal w3) {
        if (Constants.LEVEL_L1.equals(level)) {
            return w1;
        }
        if (Constants.LEVEL_L2.equals(level)) {
            return w2;
        }
        if (Constants.LEVEL_L3.equals(level)) {
            return w3;
        }
        throw new IllegalArgumentException("未知砝码分组：" + level);
    }

    /**
     * 整条记录的完整判定（M5 主入口）：逐示值判定 + 前置条件一票否决 + 较大误差备注预填。
     *
     * @param readings 9 次示值（允许 value 为 null，表示尚未识别/补录）
     * @param w1/w2/w3 天平主数据的 L1/L2/L3 砝码标称值
     * @param cleanOk      清洁是否符合要求
     * @param levelOk      水平调节是否到位
     * @param internalCalibOk 内部校正：是 / 否 / NA（NA 不算不通过）
     */
    public JudgeResult judgeRecord(List<RecognizedReading> readings,
                                   BigDecimal w1, BigDecimal w2, BigDecimal w3,
                                   boolean cleanOk, boolean levelOk, String internalCalibOk) {
        JudgeResult result = new JudgeResult();
        boolean masterDataOk = w1 != null && w2 != null && w3 != null;
        if (!masterDataOk) {
            result.getFailReasons().add("天平主数据缺少 L1/L2/L3 砝码标称值，无法判定，请先补全天平信息");
        }

        // 1. 前置条件一票否决
        boolean vetoed = false;
        if (!cleanOk) {
            vetoed = true;
            result.getFailReasons().add("前置条件不通过：清洁不符合要求（一票否决，不再判示值）");
        }
        if (!levelOk) {
            vetoed = true;
            result.getFailReasons().add("前置条件不通过：水平调节未到位（一票否决，不再判示值）");
        }
        if (!calibrationPasses(internalCalibOk)) {
            vetoed = true;
            String reason = Constants.CALIB_NO.equals(internalCalibOk)
                    ? "内部校正未通过"
                    : "内部校正未选择或取值非法（只允许 是 / 否 / NA）";
            result.getFailReasons().add("前置条件不通过：" + reason + "（一票否决，不再判示值）");
        }
        result.setVetoedByPrecondition(vetoed);

        // 2. 逐示值判定（即使被一票否决也把明细算出来，便于页面同时展示）
        List<ReadingJudge> details = new ArrayList<ReadingJudge>();
        List<String> badLevels = new ArrayList<String>();
        StringBuilder remark = new StringBuilder();
        boolean allOk = true;
        boolean complete = readings != null && readings.size() == 9;
        boolean missingValue = false;
        boolean[] seen = new boolean[10];

        if (readings != null) {
            for (RecognizedReading r : readings) {
                if (r == null || r.getSeq() < 1 || r.getSeq() > 9 || seen[r.getSeq()]) {
                    complete = false;
                    allOk = false;
                    continue;
                }
                seen[r.getSeq()] = true;
                ReadingJudge d = new ReadingJudge();
                d.setSeq(r.getSeq());
                String level = r.getLevel() != null ? r.getLevel() : groupLevel(r.getSeq());
                d.setLevel(level);
                BigDecimal nominal = r.getNominal() != null ? r.getNominal() : nominalOf(level, w1, w2, w3);
                d.setNominal(nominal);
                d.setValue(r.getValue());

                if (nominal == null || r.getValue() == null) {
                    d.setQualified(false);
                    allOk = false;
                    missingValue = true;
                    details.add(d);
                    continue;
                }
                d.setLower(lowerLimit(nominal));
                d.setUpper(upperLimit(nominal));
                boolean ok = within(r.getValue(), nominal);
                d.setQualified(ok);
                details.add(d);

                if (!ok) {
                    allOk = false;
                    if (!badLevels.contains(level)) {
                        badLevels.add(level);
                    }
                }
                String big = bigDeviationRemark(r.getSeq(), r.getValue(), nominal);
                if (big != null) {
                    if (remark.length() > 0) {
                        remark.append("；");
                    }
                    remark.append(big);
                }
            }
        }
        if (readings != null) {
            for (int seq = 1; seq <= 9; seq++) {
                if (!seen[seq]) {
                    complete = false;
                    allOk = false;
                }
            }
        }
        result.setDetails(details);

        if (!complete && !vetoed) {
            result.getFailReasons().add("示值必须为完整的 9 条且序号为 1~9，请补齐或修正后再提交");
        } else if (missingValue && !vetoed) {
            result.getFailReasons().add("存在尚未识别/补录的示值，请补齐 9 次示值后再提交");
        }
        if (!vetoed) {
            for (ReadingJudge d : details) {
                if (!d.isQualified() && d.getValue() != null && d.getNominal() != null) {
                    result.getFailReasons().add("第 " + d.getSeq() + " 次示值（" + d.getLevel() + " = "
                            + d.getNominal().stripTrailingZeros().toPlainString() + " g，限度 "
                            + d.getLower().toPlainString() + " ~ " + d.getUpper().toPlainString()
                            + " g）超出限度");
                }
            }
            // 对应 SRS §3.1.7 双量程流程：注明是哪个砝码不合格
            for (String lv : badLevels) {
                result.getFailReasons().add(lv + " 砝码对应的示值不合格");
            }
        }

        boolean qualified = !vetoed && masterDataOk && complete && !missingValue && allOk;
        result.setQualified(qualified);
        result.setResultText(qualified ? "合格" : "不合格");
        result.setRemarkPrefill(remark.length() == 0 ? Constants.DEFAULT_REMARK : remark.toString());
        return result;
    }

    /** 内部校正只有「是」或「NA」可继续；未填写、非法值或「否」均不通过。 */
    private boolean calibrationPasses(String internalCalibOk) {
        if (internalCalibOk == null) {
            return false;
        }
        String value = internalCalibOk.trim();
        return Constants.CALIB_YES.equals(value) || Constants.CALIB_NA.equals(value);
    }
}
