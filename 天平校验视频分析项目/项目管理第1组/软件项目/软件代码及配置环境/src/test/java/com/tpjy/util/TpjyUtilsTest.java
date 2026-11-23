package com.tpjy.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 工具类测试：精度→位数换算（规则 5，只约束读数位数）、记录号/视频号格式。 */
class TpjyUtilsTest {

    @Test
    @DisplayName("精度换算位数：0.1mg→4 位、0.01mg→5 位、1mg→3 位、0.1g→1 位、1g→0 位")
    void digits() {
        assertEquals(4, TpjyUtils.digitsOfPrecision("0.1mg"));
        assertEquals(5, TpjyUtils.digitsOfPrecision("0.01mg"));
        assertEquals(3, TpjyUtils.digitsOfPrecision("1mg"));
        assertEquals(1, TpjyUtils.digitsOfPrecision("0.1g"));
        assertEquals(0, TpjyUtils.digitsOfPrecision("1g"));
        assertEquals(2, TpjyUtils.digitsOfPrecision("0.01g"));
        assertEquals(-1, TpjyUtils.digitsOfPrecision(null));
        assertEquals(-1, TpjyUtils.digitsOfPrecision("abc"));
        assertTrue(TpjyUtils.samePrecision("0.1mg", "0.0001g"));
        assertTrue(TpjyUtils.samePrecision("0.01mg", "0.010mg"));
        assertFalse(TpjyUtils.samePrecision("0.1mg", "0.01mg"));
    }

    @Test
    @DisplayName("文本小数位数：10.001 → 3 位")
    void textDigits() {
        assertEquals(3, TpjyUtils.digitsOfText("10.001"));
        assertEquals(4, TpjyUtils.digitsOfText("200.0006"));
        assertEquals(0, TpjyUtils.digitsOfText("200"));
        assertEquals(-1, TpjyUtils.digitsOfText(null));
    }

    @Test
    @DisplayName("记录号/视频号格式：JY20260629-011、V-20260629-0043")
    void numbers() throws Exception {
        Date d = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2026-06-29 10:13");
        assertEquals("JY20260629-011", TpjyUtils.recordNo(d, 11));
        assertEquals("V-20260629-0043", TpjyUtils.videoNo(d, 43));
    }
}
