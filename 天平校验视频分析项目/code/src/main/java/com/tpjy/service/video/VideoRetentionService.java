package com.tpjy.service.video;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tpjy.config.AppProps;
import com.tpjy.dto.Operator;
import com.tpjy.entity.Video;
import com.tpjy.mapper.VideoMapper;
import com.tpjy.service.AuditService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

/** 视频 7 天保留与删除留痕（WBS 1.3.8 / FR-022）。 */
@Service
public class VideoRetentionService {

    private final VideoMapper videoMapper;
    private final AuditService auditService;
    private final AppProps props;

    public VideoRetentionService(VideoMapper videoMapper, AuditService auditService, AppProps props) {
        this.videoMapper = videoMapper;
        this.auditService = auditService;
        this.props = props;
    }

    /** 每天凌晨 2 点清理到期视频；物理文件删除后软删除数据库记录并写审计。 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupDaily() {
        try {
            cleanupExpired(new Date());
        } catch (Exception e) {
            System.err.println("视频到期清理失败：" + e.getMessage());
        }
    }

    /** 可独立调用的清理入口，返回成功删除的文件数。 */
    public int cleanupExpired(Date now) {
        if (now == null || props.getVideoKeepDays() <= 0) {
            return 0;
        }
        long keepMillis = props.getVideoKeepDays() * 24L * 60L * 60L * 1000L;
        Date cutoff = new Date(now.getTime() - keepMillis);
        QueryWrapper<Video> query = new QueryWrapper<Video>();
        query.isNull("deleted_at").le("uploaded_at", cutoff);
        List<Video> expired = videoMapper.selectList(query);
        int deleted = 0;
        for (Video video : expired) {
            try {
                Path path = resolvePath(video.getFilePath());
                if (path != null && Files.exists(path)) {
                    Files.delete(path);
                }
                video.setDeletedAt(now);
                videoMapper.updateById(video);
                auditService.logVideoDelete(Operator.system(), video.getId(),
                        video.getOriginalName(), "超过 " + props.getVideoKeepDays() + " 天自动清理");
                deleted++;
            } catch (Exception e) {
                System.err.println("视频清理失败（id=" + video.getId() + "）：" + e.getMessage());
            }
        }
        return deleted;
    }

    private Path resolvePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return null;
        }
        Path base = Paths.get(props.getVideoDir()).toAbsolutePath().normalize();
        Path candidate = Paths.get(filePath);
        if (!candidate.isAbsolute()) {
            candidate = Paths.get("").toAbsolutePath().resolve(candidate);
        }
        candidate = candidate.normalize();
        if (!candidate.startsWith(base)) {
            throw new IllegalArgumentException("拒绝删除视频目录之外的路径：" + candidate);
        }
        return candidate;
    }
}
