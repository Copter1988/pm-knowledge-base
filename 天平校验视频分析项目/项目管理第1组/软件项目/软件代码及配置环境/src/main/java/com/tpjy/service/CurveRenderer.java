package com.tpjy.service;

import com.tpjy.dto.CurveData;
import com.tpjy.dto.CurvePoint;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 称重值趋势图：L1/L2/L3 三个独立图。
 *
 * <p>每组只有 3 个测量值。图中不把 9 个点混成一条线，而是在各自砝码面板中显示 3 个点，
 * 并用竖线连接该组的最小值与最大值。纵轴默认放大到标称值的 ±0.2%，便于看清微小差异；
 * 绿色区域仍为实际判定用的 ±0.05% 限度带。
 */
public class CurveRenderer {

    private static final int W = 1260;
    private static final int H = 760;
    private static final int LEFT = 70;
    private static final int RIGHT = W - 40;
    private static final int TOP = 150;
    private static final int BOTTOM = 640;
    private static final double DEFAULT_PERCENT_RANGE = 0.2;

    public File render(CurveData data, File out) throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        Font title = font(Font.BOLD, 24);
        Font sub = font(Font.PLAIN, 14);
        Font panelTitle = font(Font.BOLD, 17);
        Font axis = font(Font.PLAIN, 12);

        g.setColor(new Color(30, 58, 95));
        g.setFont(title);
        g.drawString("天平称重值趋势图", LEFT, 38);
        g.setColor(new Color(75, 85, 99));
        g.setFont(sub);
        g.drawString("记录号：" + nvl(data.getRecordNo()) + "　天平：" + nvl(data.getBalanceCode())
                + "　校验日期：" + nvl(data.getCheckDate()) + "　结论：" + nvl(data.getResultText()), LEFT, 66);
        g.drawString("每组 3 次示值独立成图；竖线连接该组最小值与最大值。纵轴默认放大到标称值 ±0.2%，绿色带为 ±0.05% 限度。", LEFT, 88);

        String[] levels = new String[]{"L1", "L2", "L3"};
        int panelGap = 24;
        int panelW = (RIGHT - LEFT - panelGap * 2) / 3;
        for (int i = 0; i < levels.length; i++) {
            int panelLeft = LEFT + i * (panelW + panelGap);
            drawPanel(g, data, levels[i], panelLeft, panelLeft + panelW, panelTitle, axis);
        }

        g.dispose();
        File parent = out.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        ImageIO.write(img, "png", out);
        return out;
    }

    private void drawPanel(Graphics2D g, CurveData data, String level,
                           int left, int right, Font panelTitle, Font axisFont) {
        List<CurvePoint> points = pointsOf(data, level);
        BigDecimal nominal = points.isEmpty() ? null : points.get(0).getNominal();
        g.setColor(new Color(30, 58, 95));
        g.setFont(panelTitle);
        g.drawString(level + (nominal == null ? "" : " · " + plain(nominal) + " g"), left, TOP - 28);

        if (points.isEmpty() || nominal == null || nominal.signum() == 0) {
            g.setColor(Color.GRAY);
            g.setFont(axisFont);
            g.drawString("暂无数据", left + 80, TOP + 100);
            return;
        }

        double percentRange = DEFAULT_PERCENT_RANGE;
        for (CurvePoint p : points) {
            Double dev = deviationPercent(p);
            if (dev != null) {
                percentRange = Math.max(percentRange, Math.abs(dev) * 1.15);
            }
        }
        int plotLeft = left + 46;
        int plotRight = right - 10;
        int plotW = plotRight - plotLeft;
        int plotH = BOTTOM - TOP;
        double yScale = plotH / (percentRange * 2.0);

        int limitTop = yFor(0.05, percentRange, yScale);
        int limitBottom = yFor(-0.05, percentRange, yScale);
        g.setColor(new Color(231, 247, 234));
        g.fillRect(plotLeft, limitTop, plotW, Math.max(1, limitBottom - limitTop));

        g.setFont(axisFont);
        for (int tick = 0; tick <= 4; tick++) {
            double percent = -percentRange + (percentRange * 2.0) * tick / 4.0;
            int y = yFor(percent, percentRange, yScale);
            g.setColor(new Color(229, 231, 235));
            g.drawLine(plotLeft, y, plotRight, y);
            g.setColor(new Color(100, 116, 139));
            BigDecimal grams = nominal.multiply(
                    BigDecimal.ONE.add(BigDecimal.valueOf(percent).movePointLeft(2)));
            g.drawString(String.format("%.4f", grams), left, y + 4);
        }

        g.setColor(new Color(100, 116, 139));
        g.setStroke(new BasicStroke(1.3f));
        g.drawLine(plotLeft, TOP, plotLeft, BOTTOM);
        g.drawLine(plotLeft, BOTTOM, plotRight, BOTTOM);

        g.setColor(new Color(215, 130, 20));
        g.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                new float[]{6f, 4f}, 0f));
        g.drawLine(plotLeft, limitTop, plotRight, limitTop);
        g.drawLine(plotLeft, limitBottom, plotRight, limitBottom);
        g.setFont(axisFont);
        g.drawString("+0.05%", plotRight - 48, limitTop - 5);
        g.drawString("-0.05%", plotRight - 48, limitBottom + 14);
        g.setStroke(new BasicStroke(1f));

        double[] devs = new double[points.size()];
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        int minIndex = -1;
        int maxIndex = -1;
        int[] xs = new int[points.size()];
        int[] ys = new int[points.size()];
        for (int i = 0; i < points.size(); i++) {
            CurvePoint p = points.get(i);
            Double dev = deviationPercent(p);
            xs[i] = plotLeft + (int) ((i + 0.5) * plotW / 3.0);
            if (dev == null) {
                ys[i] = -1;
                continue;
            }
            devs[i] = dev;
            ys[i] = yFor(dev, percentRange, yScale);
            if (dev < min) {
                min = dev;
                minIndex = i;
            }
            if (dev > max) {
                max = dev;
                maxIndex = i;
            }
        }

        if (minIndex >= 0 && maxIndex >= 0 && minIndex != maxIndex) {
            g.setColor(new Color(30, 90, 170));
            g.setStroke(new BasicStroke(2.4f));
            g.drawLine(xs[maxIndex], ys[maxIndex], xs[minIndex], ys[minIndex]);
        }

        for (int i = 0; i < points.size(); i++) {
            CurvePoint p = points.get(i);
            if (ys[i] < 0) {
                continue;
            }
            g.setColor(p.isQualified() ? new Color(30, 140, 70) : new Color(200, 40, 40));
            g.fill(new Ellipse2D.Double(xs[i] - 6, ys[i] - 6, 12, 12));
            g.setFont(axisFont);
            g.drawString(p.getSeq() % 3 == 0 ? "3" : String.valueOf(p.getSeq() % 3), xs[i] - 3, BOTTOM + 20);
            g.drawString(String.format("%.3f%%", devs[i]), xs[i] - 22, ys[i] - 10);
        }

        g.setColor(new Color(71, 85, 105));
        g.setFont(axisFont);
        g.drawString("测量次序", left + panelW(plotLeft, plotRight) / 2 - 24, BOTTOM + 43);
        if (points.size() < 3 || ys[0] < 0 || ys[1] < 0 || ys[2] < 0) {
            g.setColor(new Color(180, 83, 9));
            g.drawString("存在待补录示值", plotLeft, TOP + 20);
        }
    }

    private static int panelW(int left, int right) {
        return right - left;
    }

    private static int yFor(double percent, double range, double scale) {
        return (int) (TOP + (range - percent) * scale);
    }

    private static List<CurvePoint> pointsOf(CurveData data, String level) {
        List<CurvePoint> list = new ArrayList<CurvePoint>();
        if (data == null) {
            return list;
        }
        for (CurvePoint p : data.getPoints()) {
            if (level.equals(p.getLevel())) {
                list.add(p);
            }
        }
        return list;
    }

    private static Double deviationPercent(CurvePoint p) {
        if (p.getValue() == null || p.getNominal() == null || p.getNominal().signum() == 0) {
            return null;
        }
        return p.getValue().subtract(p.getNominal())
                .divide(p.getNominal(), 8, RoundingMode.HALF_UP)
                .movePointRight(2)
                .doubleValue();
    }

    private static Font font(int style, int size) {
        String[] candidates = new String[]{"Microsoft YaHei", "SimHei", "Noto Sans CJK SC", "SansSerif"};
        for (String name : candidates) {
            Font f = new Font(name, style, size);
            if (f.canDisplay('天')) {
                return f;
            }
        }
        return new Font("SansSerif", style, size);
    }

    private static String plain(BigDecimal v) {
        return v == null ? "-" : v.stripTrailingZeros().toPlainString();
    }

    private static String nvl(String s) {
        return s == null ? "-" : s;
    }
}
