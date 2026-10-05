package com.tpjy.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 校验判定引擎单元测试（对应 SRS §3.1.5 核心业务规则）。
 * 纯单元测试，不加载 Spring 上下文、不依赖数据库。
 */
class JudgeServiceTest {

    private final JudgeService judge = new JudgeService();

    @Test
    void 归组_按顺序L1到L3() {
        assertEquals("L1", judge.groupLevel(1));
        assertEquals("L1", judge.groupLevel(3));
        assertEquals("L2", judge.groupLevel(4));
        assertEquals("L2", judge.groupLevel(6));
        assertEquals("L3", judge.groupLevel(7));
        assertEquals("L3", judge.groupLevel(9));
    }

    @Test
    void 限度_200g_应为1999000到2001000() {
        BigDecimal n = new BigDecimal("200");
        assertEquals(0, new BigDecimal("199.9000").compareTo(judge.lowerLimit(n).setScale(4)));
        assertEquals(0, new BigDecimal("200.1000").compareTo(judge.upperLimit(n).setScale(4)));
    }

    @Test
    void 限度_50g_模板写死值不可信() {
        // SRS 特别指出：xlsx 中 50g 限度写成 19.9750~20.0250 是错的，正确应为 49.9750~50.0250
        BigDecimal n = new BigDecimal("50");
        assertEquals(0, new BigDecimal("49.9750").compareTo(judge.lowerLimit(n).setScale(4)));
        assertEquals(0, new BigDecimal("50.0250").compareTo(judge.upperLimit(n).setScale(4)));
    }

    @Test
    void 逐示值_边界值含等于() {
        assertTrue(judge.within(new BigDecimal("200.1000"), new BigDecimal("200")));
        assertTrue(judge.within(new BigDecimal("199.9000"), new BigDecimal("200")));
        assertFalse(judge.within(new BigDecimal("200.1001"), new BigDecimal("200")));
        assertFalse(judge.within(new BigDecimal("199.8999"), new BigDecimal("200")));
    }

    @Test
    void 总体判定_前置条件一票否决() {
        // 示值全部合格，但清洁不过 → 不通过
        boolean pass = judge.judge(false, true, "是",
                Arrays.asList(v("200.0001"), v("199.9999"), v("200.0002")),
                Arrays.asList(v("200"), v("200"), v("200")));
        assertFalse(pass);
    }

    @Test
    void 总体判定_任一示值超限即不通过() {
        boolean pass = judge.judge(true, true, "是",
                Arrays.asList(v("200.0001"), v("200.5000"), v("200.0002")),
                Arrays.asList(v("200"), v("200"), v("200")));
        assertFalse(pass);
    }

    @Test
    void 总体判定_全部合格() {
        boolean pass = judge.judge(true, true, "是",
                Arrays.asList(v("200.0001"), v("199.9999"), v("200.0002")),
                Arrays.asList(v("200"), v("200"), v("200")));
        assertTrue(pass);
    }

    @Test
    void 较大误差_超阈值才预填() {
        // 200g 的 1% = 2g；偏差 2.1g 超阈值
        assertNotNull(judge.bigDeviationRemark(1, new BigDecimal("202.1"), new BigDecimal("200")));
        // 偏差 1.9g 未超 1%
        assertNull(judge.bigDeviationRemark(1, new BigDecimal("201.9"), new BigDecimal("200")));
    }

    private static BigDecimal v(String s) {
        return new BigDecimal(s);
    }
}
