package com.tpjy.util;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

/** 通用小工具：精度位数换算、日期格式、数值解析。 */
public final class TpjyUtils {

    private TpjyUtils() {
    }

    /**
     * 由量程精度（分度值）换算「示值应显示的小数位数」。
     * <p>0.1mg → 0.0001 g → 4 位；0.01mg → 5 位；1mg → 3 位；0.1g → 1 位；1g → 0 位。
     *
     * @param precision 形如 0.1mg / 0.01mg / 0.1g / 1g
     * @return 应显示位数；无法解析返回 -1
     */
    public static int digitsOfPrecision(String precision) {
        BigDecimal grams = precisionInGrams(precision);
        if (grams == null) {
            return -1;
        }
        return Math.max(grams.stripTrailingZeros().scale(), 0);
    }

    /** 将 0.1mg / 0.01g 等分度值统一折算成克。 */
    public static BigDecimal precisionInGrams(String precision) {
        if (precision == null) {
            return null;
        }
        String p = precision.trim().toLowerCase().replace(" ", "");
        if (p.isEmpty()) {
            return null;
        }
        try {
            BigDecimal grams;
            if (p.endsWith("mg")) {
                grams = new BigDecimal(p.substring(0, p.length() - 2)).divide(new BigDecimal("1000"));
            } else if (p.endsWith("g")) {
                grams = new BigDecimal(p.substring(0, p.length() - 1));
            } else {
                grams = new BigDecimal(p);
            }
            if (grams.signum() <= 0) {
                return null;
            }
            return grams;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean samePrecision(String left, String right) {
        BigDecimal a = precisionInGrams(left);
        BigDecimal b = precisionInGrams(right);
        return a != null && b != null && a.compareTo(b) == 0;
    }

    /** 字符串里的小数位数（用于人工补录值的位数校验）。 */
    public static int digitsOfText(String text) {
        if (text == null) {
            return -1;
        }
        String t = text.trim();
        int dot = t.indexOf('.');
        if (dot < 0) {
            return 0;
        }
        return t.length() - dot - 1;
    }

    public static BigDecimal parseDecimal(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    public static String yyyyMMdd(Date date) {
        return new SimpleDateFormat("yyyyMMdd").format(date);
    }

    /** 记录号：JY + yyyyMMdd + "-" + 3 位当日流水（与界面原型 JY20260629-011 一致）。 */
    public static String recordNo(Date date, int dailySeq) {
        return "JY" + yyyyMMdd(date) + "-" + String.format("%03d", dailySeq);
    }

    /** 视频号：V-yyyyMMdd-4 位流水（与界面原型 V-20260629-0043 一致）。 */
    public static String videoNo(Date date, int dailySeq) {
        return "V-" + yyyyMMdd(date) + "-" + String.format("%04d", dailySeq);
    }

    /** 数量（克）去掉多余 0 的展示形式，如 200 / 200.5。 */
    public static String plain(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }
}
