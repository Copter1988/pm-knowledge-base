package com.tpjy.service;

import com.tpjy.common.Constants;
import com.tpjy.dto.JudgeResult;
import com.tpjy.dto.RecognizedReading;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 判定引擎测试（对应 AC-04：判定与业务规则一致）。
 * 覆盖：限度实时计算、逐示值判定、一票否决、较大误差预填、双量程归组。
 */
class JudgeServiceTest {

    private final JudgeService judge = new JudgeService();

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    @DisplayName("限度实时计算：200g → 199.9000 ~ 200.1000；50g → 49.9750 ~ 50.0250")
    void limits() {
        assertEquals(0, bd("199.9000").compareTo(judge.lowerLimit(bd("200"))));
        assertEquals(0, bd("200.1000").compareTo(judge.upperLimit(bd("200"))));
        // 界面原型里的 50 g 行：49.9750 ~ 50.0250（不是 xlsx 里写错的 19.9750~20.0250）
        assertEquals(0, bd("49.9750").compareTo(judge.lowerLimit(bd("50"))));
        assertEquals(0, bd("50.0250").compareTo(judge.upperLimit(bd("50"))));
    }

    @Test
    @DisplayName("边界值：恰好等于上下限算合格")
    void boundary() {
        assertTrue(judge.within(bd("199.9000"), bd("200")));
        assertTrue(judge.within(bd("200.1000"), bd("200")));
        assertFalse(judge.within(bd("199.8999"), bd("200")));
        assertFalse(judge.within(bd("200.1001"), bd("200")));
    }

    @Test
    @DisplayName("归组：seq 1~3→L1、4~6→L2、7~9→L3（与量程无关）")
    void grouping() {
        assertEquals(Constants.LEVEL_L1, judge.groupLevel(1));
        assertEquals(Constants.LEVEL_L1, judge.groupLevel(3));
        assertEquals(Constants.LEVEL_L2, judge.groupLevel(4));
        assertEquals(Constants.LEVEL_L2, judge.groupLevel(6));
        assertEquals(Constants.LEVEL_L3, judge.groupLevel(7));
        assertEquals(Constants.LEVEL_L3, judge.groupLevel(9));
    }

    @Test
    @DisplayName("前置条件一票否决：清洁不通过 → 不合格，且注明原因")
    void veto() {
        List<RecognizedReading> readings = goodReadings();
        JudgeResult r = judge.judgeRecord(readings, bd("200"), bd("50"), bd("10"),
                false, true, Constants.CALIB_YES);
        assertFalse(r.isQualified());
        assertTrue(r.isVetoedByPrecondition());
        assertTrue(r.getFailReasons().get(0).contains("清洁"));
        assertEquals("不合格", r.getResultText());
    }

    @Test
    @DisplayName("内部校正 NA 不算不通过（可以合格）")
    void calibNaIsNotFail() {
        List<RecognizedReading> readings = goodReadings();
        JudgeResult r = judge.judgeRecord(readings, bd("200"), bd("50"), bd("10"),
                true, true, Constants.CALIB_NA);
        assertTrue(r.isQualified());
    }

    @Test
    @DisplayName("逐示值判定：L3 第 8 次超限 → 不合格，并注明 L3 砝码不合格")
    void oneBadReading() {
        List<RecognizedReading> readings = goodReadings();
        readings.get(7).setValue(bd("10.0051")); // L3 限度 9.9950~10.0050
        JudgeResult r = judge.judgeRecord(readings, bd("200"), bd("50"), bd("10"),
                true, true, Constants.CALIB_YES);
        assertFalse(r.isQualified());
        boolean mentionsL3 = false;
        for (String s : r.getFailReasons()) {
            if (s.contains("L3")) {
                mentionsL3 = true;
            }
        }
        assertTrue(mentionsL3, "应注明是哪个砝码不合格：" + r.getFailReasons());
    }

    @Test
    @DisplayName("较大误差：偏差 > 1% 预填备注，且不影响 ±0.05% 判定结论")
    void bigDeviation() {
        String remark = judge.bigDeviationRemark(3, bd("202.5"), bd("200"));
        assertNotNull(remark);
        assertTrue(remark.contains("1.25%"));
        assertTrue(remark.contains("必要时报修"));
    }

    @Test
    @DisplayName("全合格：结论合格、备注默认无")
    void allPass() {
        JudgeResult r = judge.judgeRecord(goodReadings(), bd("200"), bd("50"), bd("10"),
                true, true, Constants.CALIB_YES);
        assertTrue(r.isQualified());
        assertEquals("合格", r.getResultText());
        assertEquals(Constants.DEFAULT_REMARK, r.getRemarkPrefill());
        assertEquals(9, r.getDetails().size());
    }

    @Test
    @DisplayName("示值缺失不通过：未识别到应提示补录，不能算合格")
    void missingValue() {
        List<RecognizedReading> readings = goodReadings();
        readings.get(0).setValue(null);
        JudgeResult r = judge.judgeRecord(readings, bd("200"), bd("50"), bd("10"),
                true, true, Constants.CALIB_YES);
        assertFalse(r.isQualified());
    }

    @Test
    @DisplayName("必须恰好提交 9 条示值：空列表不能被判为合格")
    void emptyReadingsNeverQualified() {
        JudgeResult r = judge.judgeRecord(new ArrayList<RecognizedReading>(),
                bd("200"), bd("50"), bd("10"), true, true, Constants.CALIB_YES);
        assertFalse(r.isQualified());
        assertTrue(r.getFailReasons().get(0).contains("9 条"));
    }

    @Test
    @DisplayName("内部校正未填写不能默认通过")
    void missingCalibrationNeverQualified() {
        JudgeResult r = judge.judgeRecord(goodReadings(), bd("200"), bd("50"), bd("10"),
                true, true, null);
        assertFalse(r.isQualified());
        assertTrue(r.isVetoedByPrecondition());
        assertTrue(r.getFailReasons().get(0).contains("内部校正"));
    }

    @Test
    @DisplayName("示值编号超出 1~9 时拒绝归组")
    void invalidSequenceRejected() {
        assertThrows(IllegalArgumentException.class, () -> judge.groupLevel(0));
        assertThrows(IllegalArgumentException.class, () -> judge.groupLevel(10));
    }

    /** 9 次全在限度内的样例（与界面原型图 3 的数据风格一致）。 */
    private List<RecognizedReading> goodReadings() {
        List<String> values = Arrays.asList(
                "200.0006", "199.9998", "200.0011",
                "50.0003", "49.9997", "50.0002",
                "10.0001", "10.0000", "9.9999");
        List<RecognizedReading> list = new ArrayList<RecognizedReading>();
        for (int i = 0; i < values.size(); i++) {
            RecognizedReading r = new RecognizedReading();
            r.setSeq(i + 1);
            r.setLevel(judge.groupLevel(i + 1));
            r.setRawValue(values.get(i));
            r.setValue(bd(values.get(i)));
            list.add(r);
        }
        return list;
    }
}
