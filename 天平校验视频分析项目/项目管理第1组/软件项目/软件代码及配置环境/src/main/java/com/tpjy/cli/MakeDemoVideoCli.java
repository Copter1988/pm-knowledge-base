package com.tpjy.cli;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 生成演示/测试视频（可选工具）。
 *
 * <p>作用：造一个「带人员二维码卡 + 天平二维码卡 + 示值画面」的短视频，
 * 用于在装好 ffmpeg 的机器上自测识别链，也用于向甲方说明二维码卡片的制卡规范。
 *
 * <p>用法（IDEA 里右键 Run，Program arguments）：
 * <pre>
 * --out=videos --name=demo --person=李一 --balance=F001 --seconds=12
 * </pre>
 * 生成 videos/demo.mp4（若本机找不到 ffmpeg，则只生成图片帧并打印组装命令）。
 */
public final class MakeDemoVideoCli {

    private MakeDemoVideoCli() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> a = parse(args);
        String outDir = a.containsKey("out") ? a.get("out") : "videos";
        String name = a.containsKey("name") ? a.get("name") : "demo";
        String person = a.containsKey("person") ? a.get("person") : "李一";
        String balance = a.containsKey("balance") ? a.get("balance") : "F001";
        int seconds = a.containsKey("seconds") ? Integer.parseInt(a.get("seconds")) : 12;
        String ffmpeg = a.containsKey("ffmpeg") ? a.get("ffmpeg") : findFfmpeg();

        File out = new File(outDir);
        File frames = new File(out, "_frames/" + name);
        if (!frames.exists() && !frames.mkdirs()) {
            throw new IllegalStateException("无法创建目录：" + frames.getAbsolutePath());
        }

        // 9 次示值（从大到小：L1×3、L2×3、L3×3），画面上的数字随时间推进变化
        String[] values = new String[]{
                "200.0006", "199.9998", "200.0011",
                "50.0003", "49.9997", "50.0002",
                "10.0001", "10.0000", "9.9999"};

        for (int i = 0; i < seconds; i++) {
            String shown = values[Math.min(values.length - 1, i * values.length / seconds)];
            File png = new File(frames, String.format("frame_%04d.png", i + 1));
            writeFrame(png, "PERSON:" + person, "BALANCE:" + balance, shown);
        }
        System.out.println("已生成 " + seconds + " 张帧图：" + frames.getAbsolutePath());

        File mp4 = new File(out, name + ".mp4");
        List<String> cmd = new ArrayList<String>(Arrays.asList(
                ffmpeg, "-y", "-hide_banner", "-loglevel", "error",
                "-framerate", "1",
                "-i", new File(frames, "frame_%04d.png").getAbsolutePath(),
                "-c:v", "libx264", "-pix_fmt", "yuv420p", "-r", "1",
                mp4.getAbsolutePath()));
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            StringBuilder log = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) {
                log.append(line).append('\n');
            }
            r.close();
            int code = p.waitFor();
            if (code == 0 && mp4.isFile()) {
                System.out.println("已生成视频：" + mp4.getAbsolutePath() + "（" + mp4.length() / 1024 + " KB）");
                System.out.println();
                System.out.println("接下来用命令行识别它：");
                System.out.println("  AnalyzeCli 参数：--video=" + mp4.getPath()
                        + " --person=" + person + " --balance=" + balance
                        + " --weights=200,50,10 --precision=0.1mg --known-balances=" + balance
                        + " --values=200.0006,199.9998,200.0011,50.0003,49.9997,50.0002,10.0001,10.0000,9.9999");
            } else {
                System.out.println("ffmpeg 组装失败（exit=" + code + "）：" + log.toString().trim());
                printManual(cmd, frames, mp4);
            }
        } catch (Exception e) {
            System.out.println("没找到可用的 ffmpeg（" + ffmpeg + "），已只生成帧图。");
            printManual(cmd, frames, mp4);
        }
    }

    /** 自动定位 ffmpeg：先扫常见安装位置，再交给 PATH。 */
    private static String findFfmpeg() {
        String[] candidates = new String[]{
                "E:/ffmpeg/bin/ffmpeg.exe", "C:/ffmpeg/bin/ffmpeg.exe", "D:/ffmpeg/bin/ffmpeg.exe",
                "./ffmpeg/bin/ffmpeg.exe", "./tools/ffmpeg/bin/ffmpeg.exe",
                "/usr/bin/ffmpeg", "/usr/local/bin/ffmpeg"};
        for (String c : candidates) {
            File f = new File(c);
            if (f.isFile()) {
                return f.getAbsolutePath();
            }
        }
        return "ffmpeg";
    }

    private static void printManual(List<String> cmd, File frames, File mp4) {
        System.out.println();
        System.out.println("装好 ffmpeg 后，手动执行下面这条命令即可把帧图合成视频：");
        System.out.println(String.join(" ", cmd));
        System.out.println("（帧图目录：" + frames.getAbsolutePath() + "，目标：" + mp4.getAbsolutePath() + "）");
    }

    /** 画一帧：左上人员卡、右侧天平卡、下方模拟天平示值。 */
    private static void writeFrame(File file, String personText, String balanceText, String shown) throws Exception {
        int w = 1280;
        int h = 720;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);

        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, w, 60);
        g.setColor(new Color(30, 58, 95));
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("天平校验演示视频（含二维码卡片）", 30, 40);

        g.drawImage(qr(personText, 300), 60, 100, null);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString(personText, 60, 440);

        g.drawImage(qr(balanceText, 300), 480, 100, null);
        g.drawString(balanceText, 480, 440);

        g.setColor(new Color(30, 58, 95));
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("天平示值", 880, 160);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Monospaced", Font.BOLD, 64));
        g.drawString(shown, 880, 250);
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.setColor(Color.GRAY);
        g.drawString("g", 1160, 250);

        g.setColor(new Color(120, 120, 120));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString("二维码卡片规范：人员卡 PERSON:姓名　天平卡 BALANCE:天平编号", 60, 560);
        g.dispose();

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        ImageIO.write(img, "png", file);
    }

    private static BufferedImage qr(String text, int size) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix m = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
        return MatrixToImageWriter.toBufferedImage(m);
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> map = new java.util.HashMap<String, String>();
        if (args == null) {
            return map;
        }
        for (String arg : args) {
            if (arg == null) {
                continue;
            }
            String s = arg.trim();
            while (s.startsWith("-")) {
                s = s.substring(1);
            }
            int eq = s.indexOf('=');
            if (eq > 0) {
                map.put(s.substring(0, eq).toLowerCase(), s.substring(eq + 1));
            }
        }
        return map;
    }
}
