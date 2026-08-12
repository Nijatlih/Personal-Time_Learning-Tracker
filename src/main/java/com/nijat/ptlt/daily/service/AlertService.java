package com.nijat.ptlt.daily.service;

import com.nijat.ptlt.common.exception.ForbiddenException;
import com.nijat.ptlt.daily.entity.Alert;
import com.nijat.ptlt.daily.entity.AlertType;
import com.nijat.ptlt.daily.entity.DailyRecord;
import com.nijat.ptlt.daily.entity.StabilityStatus;
import com.nijat.ptlt.daily.repository.AlertRepository;
import com.nijat.ptlt.daily.repository.DailyRecordRepository;
import com.nijat.ptlt.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Alert服务
 * 负责生成和管理系统告警
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {
    private final AlertRepository alertRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    
    private static final int GRAY_ALERT_THRESHOLD = 2; // 连续2天GRAY触发告警

    /** 向前扫多少天找连续GRAY。只需要够判阈值，取一个宽松但有界的值。 */
    private static final int GRAY_SCAN_WINDOW_DAYS = 30;
    
    /**
     * 检查并生成Gray Alert
     * 当连续2天GRAY时生成告警
     *
     * anchorDate 是刚刚被写入的那条记录的日期，不是"今天"。此前锚在 LocalDate.now()：
     * 只要最近一条记录的日期不是今天就立刻 break，grayCount 恒为 0，不产生任何告警。
     * 而 DailyRecordService 是无条件调用本方法的，不管提交上来的 date 是哪天 ——
     * 所以补录上周两个 GRAY 天会静默 no-op，全系统唯一的告警功能只在当天录入时可用。
     */
    @Transactional
    public void checkAndGenerateGrayAlert(UUID workspaceId, LocalDate anchorDate) {
        // 从锚点当天起向前找连续GRAY。窗口只要够判阈值即可。
        List<DailyRecord> recentRecords = dailyRecordRepository
                .findOnOrBeforeWithLimit(workspaceId, anchorDate, GRAY_SCAN_WINDOW_DAYS);

        int grayCount = 0;
        LocalDate checkDate = anchorDate;

        for (DailyRecord record : recentRecords) {
            if (!record.getDate().equals(checkDate)) {
                break;
            }

            if (record.getStabilityStatus() == StabilityStatus.GRAY) {
                grayCount++;
            } else {
                break;
            }

            checkDate = checkDate.minusDays(1);
        }

        // 如果连续GRAY天数达到阈值，生成告警
        if (grayCount >= GRAY_ALERT_THRESHOLD) {
            // 检查是否已有未读的Gray Alert
            List<Alert> existingAlerts = alertRepository
                    .findByWorkspaceIdAndTypeAndIsReadFalseAndDeletedAtIsNullOrderByCreatedAtDesc(
                            workspaceId, AlertType.GRAY_ALERT);
            
            // 如果已有未读告警，不重复生成
            if (existingAlerts.isEmpty()) {
                Alert alert = Alert.builder()
                        .workspaceId(workspaceId)
                        .type(AlertType.GRAY_ALERT)
                        .message(String.format("连续%d天稳定度为GRAY，请注意调整", grayCount))
                        .isRead(false)
                        .build();
                
                alertRepository.save(alert);
                log.info("Gray alert generated: workspace={}, grayCount={}", workspaceId, grayCount);
            }
        }
    }
    
    /**
     * 获取workspace的所有告警
     */
    @Transactional(readOnly = true)
    public List<Alert> getAlerts(UUID workspaceId, UUID userId, Boolean unreadOnly) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        if (Boolean.TRUE.equals(unreadOnly)) {
            return alertRepository.findByWorkspaceIdAndIsReadFalseAndDeletedAtIsNullOrderByCreatedAtDesc(
                    workspaceId);
        } else {
            return alertRepository.findByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(workspaceId);
        }
    }
    
    /**
     * 标记告警为已读
     */
    @Transactional
    public void markAsRead(UUID alertId, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new com.nijat.ptlt.common.exception.NotFoundException("Alert not found"));
        
        // 校验workspace归属
        if (!alert.getWorkspaceId().equals(workspaceId)) {
            throw new ForbiddenException("Alert does not belong to this workspace");
        }
        
        if (!alert.getIsRead()) {
            alert.setIsRead(true);
            alert.setReadAt(java.time.OffsetDateTime.now());
            alertRepository.save(alert);
            log.info("Alert marked as read: workspace={}, alertId={}", workspaceId, alertId);
        }
    }
    
    /**
     * 校验用户对workspace的访问权限
     */
    private void validateWorkspaceAccess(UUID workspaceId, UUID userId) {
        boolean isMember = workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndDeletedAtIsNull(
                workspaceId, userId);
        if (!isMember) {
            throw new ForbiddenException("User is not a member of this workspace");
        }
    }
}
