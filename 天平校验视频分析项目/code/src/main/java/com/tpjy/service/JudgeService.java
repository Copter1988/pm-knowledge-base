package com.tpjy.service;

import com.tpjy.common.Constants;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 校验判定引擎（核心业务规则，SRS §3.1.5）。
 * 全部使用 BigDecimal，限度实时计算，绝不引用写死值。
 */
@Service
public class JudgeService {

    /** 示值归组：seq 1~3→L1、4~6→L2、7~9→L3（与量程无关，规则 4）。 */
    public String groupLevel(int seq) {
        if (seq <= 3) {
            return "L1";
        }
        if (seq <= 6) {
            return "L2";
        }
        return "L3";
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
        for (int i = 0; i < values.size(); i++) {
            if (!within(values.get(i), nominals.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 总体判定：前置条件一票否决；否则逐示值判定（规则 2/3）。
     * cleanOk / levelOk 1=通过；internalCalibOk "否" 即不通过。
     */
    public boolean judge(boolean cleanOk, boolean levelOk, String internalCalibOk,
                         List<BigDecimal> values, List<BigDecimal> nominals) {
        if (!cleanOk || !levelOk || "否".equals(internalCalibOk)) {
            return false;
        }
        return allReadingsPass(values, nominals);
    }

    /** 较大误差预填（阈值 1%），仅提示、不参与 ±0.05% 判定（规则 7）。 */
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
}
