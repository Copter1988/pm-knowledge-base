package com.tpjy.service;

import com.lowagie.text.pdf.BaseFont;
import com.tpjy.dto.RecordDetail;
import com.tpjy.dto.ReadingJudge;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * 导出 PDF（FR-017）：HTML → PDF（Flying Saucer）。
 *
 * <p>为什么不用模版引擎：Thymeleaf 独立渲染需要额外的 ognl 依赖，为减少依赖与出错面，
 * 这里直接用 Java 拼 HTML（样式放在 resources/templates/pdf/record.css，可改）；
 * resources/templates/pdf/record.html 是一份静态版式参考，供页面（乙）参考复用。
 *
 * <p>中文字体：PDF 必须内嵌字体才能显示中文，解析顺序见 resources/fonts/README.txt；
 * 找不到字体时 PDF 仍会生成，但中文可能显示为方框，控制台打印警告。
 *
 * <p>按 SRS §3.3.2：仅复核通过的记录可导出；测量时间存库但**不印在 PDF 里**。
 */
public class PdfExportService {

    /** 注册给 PDF 的字体族名，CSS 里用 font-family: 'TPJYFont'。 */
    private static final String FONT_FAMILY = "TPJYFont";

    private static final String FALLBACK_CSS =
            "body{font-family:'TPJYFont';font-size:10pt;color:#222}"
                    + "h1{font-size:16pt;text-align:center;color:#1E3A5F}"
                    + "table{width:100%;border-collapse:collapse}"
                    + "th,td{border:0.6pt solid #999;padding:3pt 4pt;font-size:9pt}"
                    + "th{background:#EEF3FA}"
                    + ".pass{color:#1E8C46;font-weight:bold}.fail{color:#C82828;font-weight:bold}";

    private final String fontPathConfig;
    private final String css;
    private final File fontFile;

    public PdfExportService(String fontPathConfig) {
        this.fontPathConfig = fontPathConfig;
        this.fontFile = resolveFont();
        this.css = loadCss() + fontFamilyRule(this.fontFile);
    }

    /** 渲染记录详情为 PDF。 */
    public File export(RecordDetail detail, File out) throws Exception {
        if (detail == null || detail.getJudge() == null) {
            throw new IllegalArgumentException("记录或判定结果为空，无法导出 PDF（请先完成识别与判定）");
        }
        if (!com.tpjy.common.Constants.STATUS_REVIEWED.equals(detail.getStatus())) {
            throw new IllegalArgumentException("仅「已复核」记录可以导出 PDF");
        }
        String html = buildHtml(detail);

        File parent = out.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        ITextRenderer renderer = new ITextRenderer();
        if (fontFile != null) {
            registerFont(renderer, fontFile);
        } else {
            System.err.println("警告：未找到中文字体，PDF 中的中文可能显示为方框。"
                    + "请把 simhei.ttf 放到 src/main/resources/fonts/（见该目录 README.txt），"
                    + "或在 application.yml 的 app.pdf-font-path 指向一个 .ttf 的绝对路径。");
        }

        renderer.setDocumentFromString(html);
        renderer.layout();
        OutputStream os = new FileOutputStream(out);
        try {
            renderer.createPDF(os);
        } finally {
            os.close();
        }
        return out;
    }

    /** 生成导出用的 HTML（XHTML，严格闭合）。 */
    public String buildHtml(RecordDetail d) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><title>天平校验记录</title><style>")
                .append(css)
                .append("</style></head><body>");

        sb.append("<h1>电子天平日常校验记录</h1>");
        sb.append("<div class=\"sub\">天平校验视频分析系统 导出</div>");

        sb.append("<div class=\"sec\">一、天平与校验信息</div>");
        sb.append("<table class=\"info\"><tr>");
        sb.append(info("记录号", e(d.getRecordNo())));
        sb.append(info("天平编号", e(d.getBalanceCode())));
        sb.append(info("视频号", e(d.getVideoNo())));
        sb.append("</tr><tr>");
        sb.append(info("科室", e(d.getDept())));
        sb.append(info("天平唯一号", e(d.getUniqueNo())));
        sb.append(info("量程 / 精度", e(d.getRangeText())));
        sb.append("</tr><tr>");
        sb.append(info("校验日期", e(d.getCheckDate())));
        sb.append(info("检验人员", e(d.getPerson())));
        sb.append(info("记录状态", e(d.getStatus())));
        sb.append("</tr></table>");

        sb.append("<div class=\"sec\">二、校验前置条件</div>");
        sb.append("<table><tr>");
        sb.append("<th>清洁符合要求</th><th>水平调节到位</th><th>内部校正是否通过</th><th>一票否决</th>");
        sb.append("</tr><tr class=\"center\">");
        sb.append("<td>").append(e(d.getCleanText())).append("</td>");
        sb.append("<td>").append(e(d.getLevelText())).append("</td>");
        sb.append("<td>").append(e(d.getInternalCalibOk())).append("</td>");
        if (d.getJudge().isVetoedByPrecondition()) {
            sb.append("<td class=\"fail\">是（前置条件不通过，判定不通过）</td>");
        } else {
            sb.append("<td>否</td>");
        }
        sb.append("</tr></table>");

        sb.append("<div class=\"sec\">三、9 次示值与判定</div>");
        sb.append("<table><tr>")
                .append("<th style=\"width:8%\">序号</th>")
                .append("<th style=\"width:8%\">分组</th>")
                .append("<th style=\"width:14%\">标称值 / g</th>")
                .append("<th style=\"width:24%\">限度 (1±0.05%) / g</th>")
                .append("<th style=\"width:16%\">示值 / g</th>")
                .append("<th style=\"width:13%\">偏差</th>")
                .append("<th style=\"width:17%\">判定</th>")
                .append("</tr>");
        for (ReadingJudge r : d.getJudge().getDetails()) {
            sb.append("<tr>");
            sb.append("<td class=\"center\">").append(r.getSeq()).append("</td>");
            sb.append("<td class=\"center\">").append(e(r.getLevel())).append("</td>");
            sb.append("<td class=\"right\">").append(e(plain(r.getNominal()))).append("</td>");
            sb.append("<td class=\"center\">").append(e(plain(r.getLower()))).append(" ~ ")
                    .append(e(plain(r.getUpper()))).append("</td>");
            sb.append("<td class=\"right\">")
                    .append(r.getValue() == null ? "未识别" : e(r.getValue().toPlainString()))
                    .append("</td>");
            sb.append("<td class=\"center\">").append(e(r.getDeviationText())).append("</td>");
            if (r.getValue() == null) {
                sb.append("<td class=\"center\">待补录</td>");
            } else if (r.isQualified()) {
                sb.append("<td class=\"center pass\">合格</td>");
            } else {
                sb.append("<td class=\"center fail\">不合格</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");

        sb.append("<div class=\"sec\">四、结论与备注</div>");
        sb.append("<table><tr><th style=\"width:20%\">外部校验结论</th>");
        sb.append("<td style=\"width:30%\" class=\"")
                .append(d.getJudge().isQualified() ? "pass" : "fail")
                .append("\"><b>").append(e(d.getJudge().getResultText())).append("</b></td>");
        sb.append("<th style=\"width:20%\">判定规则</th>");
        sb.append("<td style=\"width:30%\">任一示值超出（1±0.05%）L，或前置条件任一项不通过 → 不通过</td>");
        sb.append("</tr><tr><th>不合格原因</th><td colspan=\"3\">");
        if (d.getJudge().getFailReasons().isEmpty()) {
            sb.append("无");
        } else {
            for (String r : d.getJudge().getFailReasons()) {
                sb.append("<div>").append(e(r)).append("</div>");
            }
        }
        sb.append("</td></tr></table>");

        sb.append("<div class=\"label\">备注（默认「无」；异常情况在此说明，如数值显示异常、无法稳定等）</div>");
        sb.append("<div class=\"remark\">").append(e(d.getRemark())).append("</div>");

        if (d.getTraces() != null && !d.getTraces().isEmpty()) {
            sb.append("<div class=\"sec\">五、识别值改动留痕</div>");
            sb.append("<table><tr><th style=\"width:22%\">项目</th><th>改动内容</th></tr>");
            for (String t : d.getTraces()) {
                sb.append("<tr><td class=\"center\">识别值改动</td><td>").append(e(t)).append("</td></tr>");
            }
            sb.append("</table>");
        }

        sb.append("<table class=\"sign\"><tr>");
        sb.append("<td style=\"width:50%\">检验人员签字：").append(e(d.getPerson())).append("</td>");
        sb.append("<td style=\"width:50%\">复核确认人签字：").append(e(d.getReviewerName())).append("</td>");
        sb.append("</tr></table>");

        sb.append("<div class=\"foot\">注：本记录由系统按 ±0.05% 规则实时计算限度后判定，未引用任何写死的限度值；")
                .append("测量时间已存入数据库，按需求约定不在本导出件中打印。视频保留 7 天。</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private String info(String label, String value) {
        return "<td class=\"label\" style=\"width:13%\">" + label + "</td>"
                + "<td style=\"width:20%\"><b>" + value + "</b></td>";
    }

    private static String plain(java.math.BigDecimal v) {
        return v == null ? "-" : v.stripTrailingZeros().toPlainString();
    }

    private static String e(String s) {
        if (s == null) {
            return "-";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /** 注册中文字体：优先用显式族名覆盖，失败则按字体自带族名注册。 */
    private void registerFont(ITextRenderer renderer, File font) {
        String path = font.getAbsolutePath();
        try {
            // 签名：addFont(path, fontFamilyNameOverride, embedded, encoding)
            renderer.getFontResolver().addFont(path, FONT_FAMILY, true, BaseFont.IDENTITY_H);
            return;
        } catch (Exception e) {
            // 继续尝试下一种注册方式
        }
        try {
            renderer.getFontResolver().addFont(path, BaseFont.IDENTITY_H, true);
        } catch (Exception e) {
            try {
                renderer.getFontResolver().addFont(path, true);
            } catch (Exception ex) {
                System.err.println("警告：注册中文字体失败（" + path + "）：" + ex.getMessage());
            }
        }
    }

    /**
     * 生成字体族名规则，追加在原 CSS 之后（后者覆盖前者）。
     * 不同字体文件内部族名不同（SimHei / 黑体 / …），候选列全一点可以避免
     * 「字体已注册但 CSS 找不到族名 → 退回 Times-Roman 显示方框」这个坑。
     */
    private String fontFamilyRule(File font) {
        StringBuilder fam = new StringBuilder("'" + FONT_FAMILY
                + "', 'SimHei', 'SimSun', 'Microsoft YaHei', '微软雅黑',"
                + " 'Noto Sans CJK SC', 'Source Han Sans SC', '黑体', '宋体', sans-serif");
        if (font != null) {
            try {
                java.awt.Font f = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, font);
                if (f.getName() != null && f.getName().length() > 0) {
                    fam.insert(0, "'" + f.getName() + "', ");
                }
                if (f.getFamily() != null && f.getFamily().length() > 0
                        && !f.getFamily().equals(f.getName())) {
                    fam.insert(0, "'" + f.getFamily() + "', ");
                }
            } catch (Exception ignore) {
                // 探测不到族名就只用候选列表
            }
        }
        return "body, h1, .sub, .sec, table, th, td, .label, .remark, .sign, .foot { font-family: "
                + fam + "; }";
    }

    private String loadCss() {
        InputStream in = PdfExportService.class.getResourceAsStream("/templates/pdf/record.css");
        if (in == null) {
            return FALLBACK_CSS;
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return new String(bos.toByteArray(), "UTF-8");
        } catch (Exception e) {
            return FALLBACK_CSS;
        } finally {
            try {
                in.close();
            } catch (Exception ignore) {
                // 关闭失败不影响导出
            }
        }
    }

    /** 找中文字体：配置路径（classpath 或绝对路径）→ Windows 常见字体 → null。 */
    private File resolveFont() {
        File fromConfig = tryLoad(fontPathConfig);
        if (fromConfig != null) {
            return fromConfig;
        }
        List<String> windows = Arrays.asList(
                "C:/Windows/Fonts/simhei.ttf",
                "C:/Windows/Fonts/simkai.ttf",
                "C:/Windows/Fonts/msyh.ttf",
                "C:/Windows/Fonts/STZHONGS.TTF");
        for (String p : windows) {
            File f = new File(p);
            if (f.isFile()) {
                return f;
            }
        }
        return null;
    }

    private File tryLoad(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        String p = path.trim();
        if (p.startsWith("classpath:")) {
            p = p.substring("classpath:".length());
        }
        if (p.startsWith("/") || p.startsWith("\\")) {
            InputStream in = PdfExportService.class.getResourceAsStream(p);
            if (in == null) {
                return null;
            }
            try {
                File tmp = File.createTempFile("tpjy-font", ".ttf");
                tmp.deleteOnExit();
                OutputStream os = new FileOutputStream(tmp);
                try {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        os.write(buf, 0, n);
                    }
                } finally {
                    os.close();
                    in.close();
                }
                return tmp;
            } catch (Exception e) {
                return null;
            }
        }
        File f = new File(p);
        return f.isFile() ? f : null;
    }
}
