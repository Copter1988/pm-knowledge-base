package com.tpjy.service.video;

import com.tpjy.config.AppProps;
import com.tpjy.dto.StoredVideoInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** 视频导入落盘（WBS 1.3.2）：日期分目录、随机文件名、扩展名白名单。 */
@Service
public class VideoStorageService {

    private static final long MAX_VIDEO_BYTES = 500L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<String>(
            Arrays.asList(".mp4", ".mov", ".avi", ".mkv", ".webm", ".m4v"));

    private final AppProps props;

    public VideoStorageService(AppProps props) {
        this.props = props;
    }

    public StoredVideoInfo store(MultipartFile upload) throws IOException {
        if (upload == null || upload.isEmpty()) {
            throw new IllegalArgumentException("请选择要导入的视频文件");
        }
        if (upload.getSize() > MAX_VIDEO_BYTES) {
            throw new IllegalArgumentException("视频超过 500MB 上限");
        }

        String originalName = safeOriginalName(upload.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的视频格式：" + extension);
        }

        Date now = new Date();
        String day = new SimpleDateFormat("yyyyMMdd").format(now);
        String storedName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path base = Paths.get(props.getVideoDir()).toAbsolutePath().normalize();
        Path folder = base.resolve(day).normalize();
        Path target = folder.resolve(storedName).normalize();
        if (!target.startsWith(base)) {
            throw new IllegalArgumentException("非法视频存储路径");
        }

        Files.createDirectories(folder);
        InputStream input = upload.getInputStream();
        try {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            input.close();
        }

        String relativePath = new File(props.getVideoDir(), day + File.separator + storedName)
                .getPath().replace('\\', '/');
        return new StoredVideoInfo(target.toFile(), relativePath, originalName, now);
    }

    private static String safeOriginalName(String submittedName) {
        if (submittedName == null || submittedName.trim().isEmpty()) {
            throw new IllegalArgumentException("视频文件缺少文件名");
        }
        String normalized = submittedName.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        name = name.trim();
        if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
            throw new IllegalArgumentException("视频文件名非法");
        }
        return name;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            throw new IllegalArgumentException("视频文件缺少扩展名");
        }
        return name.substring(dot).toLowerCase(Locale.ROOT);
    }
}
