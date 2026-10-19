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

/**
 * 称重值曲线渲染（FR-016）。
 *
 * <p>横轴为示值序号 1~9；纵轴取「相对偏差 %」——因为 9 次示值分属 L1/L2/L3 三个不同标称值
 * （如 200 g / 50 g / 10 g），直接画原始示值无法同屏比较，故按各自标称值折算相对偏差，
 * 并叠加 ±0.05% 限度带，判定是否超限一目了然。
 * 只依赖 JDK 自带的 Java2D，不引入额外图表库（内网可运行）。
 */
public class CurveRenderer {

    private static final int W = 1000;
    private static final int H = 1040;
    private static final int LEFT = 90;
    private static final int RIGHT = W - 50;
    private static final int TOP = 130;
    private static final int BOTTOM = 700;

    /** 渲染为 PNG。 */
    public File render(CurveData data, File out) throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        Font base = new Font("SansSerif", Font.PLAIN, 15);
        Font bold = new Font("SansSerif", Font.BOLD, 18);
        Font small = new Font("SansSerif", Font.PLAIN, 13);

        // 标题
        g.setColor(new Color(30, 58, 95));
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("称重值曲线图", LEFT, 40);
        g.setFont(base);
        g.setColor(Color.DARK_GRAY);
        g.drawString("记录号：" + nvl(data.getRecordNo()) + "　天平：" + nvl(data.getBalanceCode())
                + "　校验日期：" + nvl(data.getCheckDate()) + "　结论：" + nvl(data.getResultText()), LEFT, 64);
        g.drawString("纵轴：相对标称值的偏差（%）；橙色虚线为 ±0.05% 限度", LEFT, 86);

        // 纵轴范围（单位：百分比 %）：±0.06% 起，比 ±0.05% 限度略宽，便于看出超限
        double yMax = 0.06;
        for (CurvePoint p : data.getPoints()) {
            Double dev = devPercent(p);
            if (dev != null && Math.abs(dev) > yMax) {
                yMax = Math.abs(dev) * 1.15;
            }
        }
        double yMin = -yMax;

        int plotW = RIGHT - LEFT;
        int plotH = BOTTOM - TOP;
        double xOf = plotW / 9.0;
        double yOf = plotH / (yMax - yMin);

        // 限度带（±0.05%）
        int bandTop = (int) (TOP + (yMax - 0.05) * yOf);
        int bandBottom = (int) (TOP + (yMax + 0.05) * yOf);
        g.setColor(new Color(226, 245, 230));
        g.fillRect(LEFT, bandTop, plotW, bandBottom - bandTop);

        // 网格 + 纵轴刻度
        g.setFont(small);
        for (int i = 0; i <= 6; i++) {
            double v = yMin + (yMax - yMin) * i / 6.0;
            int y = (int) (BOTTOM - (v - yMin) * yOf);
            g.setColor(new Color(230, 230, 230));
            g.drawLine(LEFT, y, RIGHT, y);
            g.setColor(Color.GRAY);
            String label = String.format("%.3f%%", v);
            g.drawString(label, LEFT - 62, y + 5);
        }

        // 坐标轴
        g.setColor(new Color(120, 120, 120));
        g.setStroke(new BasicStroke(1.4f));
        g.drawLine(LEFT, TOP, LEFT, BOTTOM);
        g.drawLine(LEFT, BOTTOM, RIGHT, BOTTOM);

        // 限度参考线
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 4f}, 0f));
        g.setColor(new Color(215, 130, 20));
        int up = (int) (TOP + (yMax - 0.05) * yOf);
        int dn = (int) (TOP + (yMax + 0.05) * yOf);
        g.drawLine(LEFT, up, RIGHT, up);
        g.drawLine(LEFT, dn, RIGHT, dn);
        g.setStroke(new BasicStroke(1f));
        g.setFont(small);
        g.drawString("+0.05%", RIGHT - 60, up - 6);
        g.drawString("-0.05%", RIGHT - 60, dn + 16);

        // 横轴刻度（1~9）
        g.setColor(Color.GRAY);
        String[] levels = new String[]{"L1", "L1", "L1", "L2", "L2", "L2", "L3", "L3", "L3"};
        for (int i = 1; i <= 9; i++) {
            int x = (int) (LEFT + (i - 0.5) * xOf);
            g.drawLine(x, BOTTOM, x, BOTTOM + 5);
            g.setColor(new Color(90, 90, 90));
            g.setFont(small);
            g.drawString(String.valueOf(i), x - 4, BOTTOM + 22);
            g.setColor(new Color(150, 150, 150));
            g.drawString(levels[i - 1], x - 9, BOTTOM + 40);
            g.setColor(Color.GRAY);
        }
        g.setColor(new Color(60, 60, 60));
        g.setFont(base);
        g.drawString("示值序号（1~9，第 1~3 次属 L1、4~6 属 L2、7~9 属 L3）", LEFT + plotW / 2 - 190, BOTTOM + 70);

        // 折线 + 数据点
        g.setStroke(new BasicStroke(2.2f));
        g.setColor(new Color(30, 90, 170));
        int prevX = -1;
        int prevY = -1;
        for (CurvePoint p : data.getPoints()) {
            Double dev = devPercent(p);
            if (dev == null) {
                continue;
            }
            int x = (int) (LEFT + (p.getSeq() - 0.5) * xOf);
            int y = (int) (TOP + (yMax - dev) * yOf);
            if (prevX >= 0) {
                g.drawLine(prevX, prevY, x, y);
            }
            prevX = x;
            prevY = y;
            // 合格绿、不合格红
            g.setColor(p.isQualified() ? new Color(30, 140, 70) : new Color(200, 40, 40));
            g.fill(new Ellipse2D.Double(x - 5, y - 5, 10, 10));
            g.setFont(small);
            String txt = String.format("%.3f%%", dev);
            if ("-0.000%".equals(txt)) {
                txt = "0.000%";
            }
            g.drawString(txt, x - 26, y - 12);
            g.setColor(new Color(30, 90, 170));
        }

        // 底部数据表
        g.setColor(Color.BLACK);
        g.setFont(small);
        int ty = BOTTOM + 110;
        g.setFont(bold);
        g.drawString("明细（标称值 / 限度 / 示值）", LEFT - 30, ty);
        g.setFont(small);
        ty += 20;
        for (CurvePoint p : data.getPoints()) {
            String line = "第 " + p.getSeq() + " 次（" + p.getLevel() + "）：标称 "
                    + plain(p.getNominal()) + " g　限度 "
                    + plain(p.getLower()) + " ~ " + plain(p.getUpper()) + " g　示值 "
                    + (p.getValue() == null ? "未识别" : p.getValue().toPlainString())
                    + "　" + (p.getValue() == null ? "待补录" : (p.isQualified() ? "合格" : "不合格"));
            if (ty > H - 12) {
                break;
            }
            g.drawString(line, LEFT - 30, ty);
            ty += 18;
        }

        g.dispose();

        File parent = out.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        ImageIO.write(img, "png", out);
        return out;
    }

    /** 相对标称值的偏差（单位：百分比数值），如 0.03 表示 0.03%。 */
    private static Double devPercent(CurvePoint p) {
        if (p.getValue() == null || p.getNominal() == null || p.getNominal().signum() == 0) {
            return null;
        }
        return p.getValue().subtract(p.getNominal())
                .divide(p.getNominal(), 8, RoundingMode.HALF_UP)
                .movePointRight(2)
                .doubleValue();
    }

    private static String plain(BigDecimal v) {
        return v == null ? "-" : v.stripTrailingZeros().toPlainString();
    }

    private static String nvl(String s) {
        return s == null ? "-" : s;
    }
}
