package com.tpjy.service;

import com.tpjy.dto.MassReading;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MassReadingTest {

    @Test
    @DisplayName("mg 保留实际显示，同时换算为 g 用于判定")
    void convertsMilligramsToGrams() {
        MassReading reading = MassReading.parse("20000.2 mg");

        assertEquals(new BigDecimal("20000.2"), reading.getDisplayValue());
        assertEquals("mg", reading.getUnit());
        assertEquals(0, new BigDecimal("20.0002").compareTo(reading.getGrams()));
        assertEquals("20000.2 mg", reading.getDisplayText());
    }

    @Test
    @DisplayName("g 值原样保留小数位，无单位时按 g 处理")
    void keepsGramDisplay() {
        MassReading reading = MassReading.parse("200.0008");
        assertEquals("g", reading.getUnit());
        assertEquals("200.0008 g", reading.getDisplayText());
        assertEquals(0, new BigDecimal("200.0008").compareTo(reading.getGrams()));
    }

    @Test
    @DisplayName("待机零点与未稳定读数不可作为测量值")
    void rejectsStandbyAndUnstable() {
        assertTrue(MassReading.parse("0.0000g").isStandbyZero());
        assertFalse(MassReading.parse("0.0mg").isUsableMeasurement());
        assertFalse(new MassReading(new BigDecimal("20.0002"), "g", false).isUsableMeasurement());
        assertFalse(MassReading.parse("-0.0002g").isUsableMeasurement());
        assertTrue(MassReading.parse("20000.2mg").isUsableMeasurement());
    }

    @Test
    @DisplayName("不支持的单位直接报错")
    void rejectsUnknownUnit() {
        assertThrows(IllegalArgumentException.class, () -> MassReading.parse("2.5kg"));
    }
}
