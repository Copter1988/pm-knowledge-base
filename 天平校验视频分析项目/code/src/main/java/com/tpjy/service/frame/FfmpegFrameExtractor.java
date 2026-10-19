package com.tpjy.service.frame;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于 ffmpeg 命令行的抽帧实现（默认）。
 *
 * <p>命令形如：
 * <pre>ffmpeg -y -hide_banner -loglevel error -i 视频 -vf fps=1 -q:v 2 输出目录/frame_%05d.jpg</pre>
 * 抽帧间隔 &gt; 1 秒时 vf 使用 {@code fps=1/间隔}。
 */
public class FfmpegFrameExtractor implements FrameExtractor {

    private final String ffmpegPath;
    private final int intervalSeconds;
    private final int timeoutSeconds;

    public FfmpegFrameExtractor(String ffmpegPath, int intervalSeconds, int timeoutSeconds) {
        this.ffmpegPath = resolveExecutable(ffmpegPath);
        this.intervalSeconds = intervalSeconds <= 0 ? 1 : intervalSeconds;
        this.timeoutSeconds = timeoutSeconds <= 0 ? 120 : timeoutSeconds;
    }

    /**
     * 定位 ffmpeg 可执行文件。
     * 传了绝对/相对路径就用它；传空或裸名 ffmpeg 时，先扫常见安装位置，再交给 PATH。
     */
    private static String resolveExecutable(String configured) {
        String p = configured == null ? "" : configured.trim();
        if (!p.isEmpty() && !"ffmpeg".equalsIgnoreCase(p)) {
            File f = new File(p);
            return f.isFile() ? f.getAbsolutePath() : p;
        }
        List<String> candidates = Arrays.asList(
                "E:/ffmpeg/bin/ffmpeg.exe",
                "C:/ffmpeg/bin/ffmpeg.exe",
                "D:/ffmpeg/bin/ffmpeg.exe",
                "./ffmpeg/bin/ffmpeg.exe",
                "./tools/ffmpeg/bin/ffmpeg.exe",
                "/usr/bin/ffmpeg",
                "/usr/local/bin/ffmpeg");
        for (String c : candidates) {
            File f = new File(c);
            if (f.isFile()) {
                return f.getAbsolutePath();
            }
        }
        return "ffmpeg";
    }

    @Override
    public String name() {
        return "ffmpeg";
    }

    @Override
    public List<Frame> extract(File video, File outputDir) throws Exception {
        if (video == null || !video.isFile()) {
            throw new IllegalArgumentException("视频文件不存在：" + video);
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IllegalStateException("无法创建抽帧目录：" + outputDir.getAbsolutePath());
        }
        clearFrames(outputDir);

        String fpsFilter = intervalSeconds == 1 ? "1" : ("1/" + intervalSeconds);
        List<String> cmd = new ArrayList<String>(Arrays.asList(
                ffmpegPath,
                "-y",
                "-hide_banner",
                "-loglevel", "error",
                "-i", video.getAbsolutePath(),
                "-vf", "fps=" + fpsFilter,
                "-q:v", "2",
                new File(outputDir, "frame_%05d.jpg").getAbsolutePath()
        ));

        Process process;
        try {
            process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        } catch (Exception e) {
            throw new IllegalStateException("无法启动 ffmpeg（路径：" + ffmpegPath + "）。"
                    + "已自动扫描 E:/ffmpeg/bin/ffmpeg.exe、C:/ffmpeg/bin/ffmpeg.exe 等常见位置未找到，"
                    + "请在 application.yml 配置 app.ffmpeg-path 指向 ffmpeg.exe 的绝对路径。原始错误：" + e.getMessage(), e);
        }

        StringBuilder log = new StringBuilder();
        InputStream in = process.getInputStream();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                log.append(line).append('\n');
            }
        } finally {
            try {
                in.close();
            } catch (Exception ignore) {
                // 关闭流失败不影响主流程
            }
        }

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("抽帧超时（超过 " + timeoutSeconds + " 秒），视频可能过大或 ffmpeg 卡住。");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("ffmpeg 抽帧失败（exit=" + process.exitValue() + "）：" + log.toString().trim());
        }

        List<Frame> frames = new ArrayList<Frame>();
        File[] files = outputDir.listFiles(new java.io.FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("frame_") && name.toLowerCase().endsWith(".jpg");
            }
        });
        if (files != null) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    return a.getName().compareTo(b.getName());
                }
            });
            int index = 0;
            for (File f : files) {
                frames.add(new Frame(index * intervalSeconds, f));
                index++;
            }
        }
        if (frames.isEmpty()) {
            throw new IllegalStateException("抽帧结果为空：视频可能损坏或编码不被支持。" + log.toString().trim());
        }
        return frames;
    }

    private void clearFrames(File dir) {
        File[] olds = dir.listFiles(new java.io.FilenameFilter() {
            @Override
            public boolean accept(File d, String name) {
                return name.startsWith("frame_") && name.toLowerCase().endsWith(".jpg");
            }
        });
        if (olds != null) {
            for (File f : olds) {
                if (!f.delete()) {
                    f.deleteOnExit();
                }
            }
        }
    }
}
