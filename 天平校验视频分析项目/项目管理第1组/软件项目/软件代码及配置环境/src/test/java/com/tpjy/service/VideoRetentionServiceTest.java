package com.tpjy.service;

import com.tpjy.config.AppProps;
import com.tpjy.dto.Operator;
import com.tpjy.entity.AuditLog;
import com.tpjy.entity.Video;
import com.tpjy.mapper.VideoMapper;
import com.tpjy.service.audit.AuditSink;
import com.tpjy.service.video.VideoRetentionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VideoRetentionServiceTest {

    @Test
    @DisplayName("到期视频删除物理文件、软删除记录并写 VIDEO_DELETE 审计")
    void cleansExpiredVideo(@TempDir Path tmp) throws Exception {
        Path videoFile = tmp.resolve("old.mp4");
        Files.write(videoFile, new byte[]{1, 2, 3});

        Video video = new Video();
        video.setId(9L);
        video.setOriginalName("old.mp4");
        video.setFilePath(videoFile.toAbsolutePath().toString());

        VideoMapper mapper = mock(VideoMapper.class);
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(video));

        List<AuditLog> logs = new ArrayList<AuditLog>();
        AuditService audit = new AuditService().addSink(new AuditSink() {
            @Override
            public void write(Operator operator, AuditLog log) {
                logs.add(log);
            }
        });
        AppProps props = AppProps.defaults();
        props.setVideoDir(tmp.toString());
        props.setVideoKeepDays(7);

        int count = new VideoRetentionService(mapper, audit, props).cleanupExpired(new Date());

        assertEquals(1, count);
        assertFalse(Files.exists(videoFile));
        assertNotNull(video.getDeletedAt());
        verify(mapper).updateById(video);
        assertEquals(1, logs.size());
        assertEquals("VIDEO_DELETE", logs.get(0).getAction());
    }
}
