package com.tpjy.dto;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 天平实际显示值：保留显示数值与单位，同时提供统一换算后的克值供判定使用。
 *
 * <p>例如 `20000.2 mg` 和 `20.0002 g` 的克值相同，但记录表必须原样保留实际显示。
 */
public class MassReading {

    private static final Pattern VALUE = Pattern.compile(
            "^\\s*([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+))\\s*(mg|g)?\\s*$",
            Pattern.CASE_INSENSITIVE);

    private final BigDecimal displayValue;
    private final String unit;
    private final BigDecimal grams;
    private final boolean stable;

    public MassReading(BigDecimal displayValue, String unit, boolean stable) {
        if (displayValue == null) {
            throw new IllegalArgumentException("示值不能为空");
        }
        String normalizedUnit = unit == null ? "g" : unit.trim().toLowerCase(Locale.ROOT);
        if (!"g".equals(normalizedUnit) && !"mg".equals(normalizedUnit)) {
            throw new IllegalArgumentException("只支持 g / mg 单位：" + unit);
        }
        this.displayValue = displayValue;
        this.unit = normalizedUnit;
        this.grams = "mg".equals(normalizedUnit)
                ? displayValue.movePointLeft(3) : displayValue;
        this.stable = stable;
    }

    /** 人工补录值默认视为已稳定；OCR 适配器应按帧序列/颜色状态设置 stable。 */
    public static MassReading parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("示值为空");
        }
        Matcher matcher = VALUE.matcher(text);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("示值格式应类似 200.0008g 或 20000.8mg：" + text);
        }
        String unit = matcher.group(2) == null ? "g" : matcher.group(2);
        return new MassReading(new BigDecimal(matcher.group(1)), unit, true);
    }

    public static MassReading ofGrams(BigDecimal grams) {
        return new MassReading(grams, "g", true);
    }

    public BigDecimal getDisplayValue() {
        return displayValue;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getGrams() {
        return grams;
    }

    public boolean isStable() {
        return stable;
    }

    public boolean isStandbyZero() {
        return displayValue.signum() == 0;
    }

    /** 可用于校验记录的读数：必须已稳定、为正值，且不能是待机零点。 */
    public boolean isUsableMeasurement() {
        return stable && grams.signum() > 0;
    }

    public String getDisplayText() {
        return displayValue.toPlainString() + " " + unit;
    }
}
