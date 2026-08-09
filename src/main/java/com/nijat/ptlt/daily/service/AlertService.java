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
    
    /**
     * 检查并生成Gray Alert
     * 当连续2天GRAY时生成告警
     */
    @Transactional
    public void checkAndGenerateGrayAlert(UUID workspaceId) {
        // 获取最近的记录
        List<DailyRecord> recentRecords = dailyRecordRepository
                .findRecentByWorkspaceId(workspaceId);
        
        // 检查最近是否有连续GRAY
        int grayCount = 0;
        LocalDate checkDate = LocalDate.now();
        
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
