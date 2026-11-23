package com.tpjy.service;

import com.tpjy.config.AppProps;
import com.tpjy.dto.StoredVideoInfo;
import com.tpjy.service.video.VideoStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoStorageServiceTest {

    @Test
    @DisplayName("视频按日期目录落盘，并使用随机安全文件名")
    void storesVideoSafely(@TempDir Path tmp) throws Exception {
        AppProps props = AppProps.defaults();
        props.setVideoDir(tmp.toString());
        VideoStorageService service = new VideoStorageService(props);
        MockMultipartFile upload = new MockMultipartFile(
                "video", "../my-check.MP4", "video/mp4", "demo-video".getBytes(StandardCharsets.UTF_8));

        StoredVideoInfo stored = service.store(upload);

        assertTrue(stored.getFile().isFile());
        assertEquals("my-check.MP4", stored.getOriginalName());
        assertEquals("demo-video", new String(Files.readAllBytes(stored.getFile().toPath()), StandardCharsets.UTF_8));
        assertTrue(stored.getRelativePath().endsWith(".mp4"));
        assertTrue(stored.getFile().toPath().normalize().startsWith(tmp.toAbsolutePath().normalize()));
    }

    @Test
    @DisplayName("非视频扩展名与空文件被拒绝")
    void rejectsInvalidUpload(@TempDir Path tmp) {
        AppProps props = AppProps.defaults();
        props.setVideoDir(tmp.toString());
        VideoStorageService service = new VideoStorageService(props);

        assertThrows(IllegalArgumentException.class, () -> service.store(
                new MockMultipartFile("video", "payload.exe", "application/octet-stream", new byte[]{1})));
        assertThrows(IllegalArgumentException.class, () -> service.store(
                new MockMultipartFile("video", "empty.mp4", "video/mp4", new byte[0])));
    }
}
