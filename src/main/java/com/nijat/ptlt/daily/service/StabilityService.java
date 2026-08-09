package com.nijat.ptlt.daily.service;

import com.nijat.ptlt.daily.entity.CognitiveEnergy;
import com.nijat.ptlt.daily.entity.DailyRecord;
import com.nijat.ptlt.daily.entity.StabilityStatus;
import com.nijat.ptlt.routine.entity.RoutineCompletion;
import com.nijat.ptlt.routine.entity.RoutineDefinition;
import com.nijat.ptlt.routine.entity.RoutineType;
import com.nijat.ptlt.routine.repository.RoutineCompletionRepository;
import com.nijat.ptlt.routine.repository.RoutineDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 稳定度计算服务
 * 
 * 稳定度评分规则（严格按PRD-STRUCT实现）：
 * - Morning routine completion: 0-20
 * - Evening routine completion: 0-20
 * - Core task completed: 0 或 30
 * - Cognitive energy: low=0, medium=10, high=15
 * - Aligned with goals: 0 或 15
 * 
 * 状态阈值：
 * - GREEN ≥ 80
 * - YELLOW 50-79
 * - GRAY < 50
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StabilityService {
    private final RoutineDefinitionRepository routineDefinitionRepository;
    private final RoutineCompletionRepository routineCompletionRepository;
    
    /**
     * 计算并设置daily record的稳定度
     * 
     * 计算逻辑：
     * 1. 从routine_completions表获取完成情况
     * 2. 如果不存在routine_definitions，完成率视为0
     * 3. 按PRD规则计算各项得分
     * 4. 计算总分并确定状态
     */
    @Transactional(readOnly = true)
    public void calculateAndSetStability(DailyRecord dailyRecord, UUID workspaceId, LocalDate date) {
        int score = 0;
        
        // 1. Morning routine completion: 0-20
        int morningScore = calculateRoutineScore(workspaceId, date, RoutineType.MORNING);
        score += morningScore;
        dailyRecord.setMorningRoutineCompleted(morningScore > 0);
        
        // 2. Evening routine completion: 0-20
        int eveningScore = calculateRoutineScore(workspaceId, date, RoutineType.EVENING);
        score += eveningScore;
        dailyRecord.setEveningRoutineCompleted(eveningScore > 0);
        
        // 3. Core task completed: 0 或 30
        if (Boolean.TRUE.equals(dailyRecord.getCoreTaskCompleted())) {
            score += 30;
        }
        
        // 4. Cognitive energy: low=0, medium=10, high=15
        if (dailyRecord.getCognitiveEnergy() != null) {
            switch (dailyRecord.getCognitiveEnergy()) {
                case LOW -> score += 0;
                case MEDIUM -> score += 10;
                case HIGH -> score += 15;
            }
        }
        
        // 5. Aligned with goals: 0 或 15
        if (Boolean.TRUE.equals(dailyRecord.getAlignedWithGoals())) {
            score += 15;
        }
        
        // 设置稳定度评分和状态
        dailyRecord.setStabilityScore(score);
        dailyRecord.setStabilityStatus(determineStatus(score));
        
        log.debug("Stability calculated for workspace {} date {}: score={}, status={}",
                workspaceId, date, score, dailyRecord.getStabilityStatus());
    }
    
    /**
     * 计算routine完成得分（0-20）
     * 
     * 规则：
     * - 如果不存在routine_definition，返回0
     * - 如果存在routine_definition但未完成，返回0
     * - 如果完成，返回20
     */
    private int calculateRoutineScore(UUID workspaceId, LocalDate date, RoutineType type) {
        // 查找routine定义
        Optional<RoutineDefinition> definitionOpt = routineDefinitionRepository
                .findByWorkspaceIdAndTypeAndDeletedAtIsNull(workspaceId, type);
        
        // 如果不存在routine定义，完成率视为0（PRD规则）
        if (definitionOpt.isEmpty()) {
            return 0;
        }
        
        RoutineDefinition definition = definitionOpt.get();
        
        // 查找完成记录
        Optional<RoutineCompletion> completionOpt = routineCompletionRepository
                .findByWorkspaceIdAndDateAndRoutineDefinitionIdAndDeletedAtIsNull(
                        workspaceId, date, definition.getId());
        
        // 如果完成，返回20分；否则返回0分
        if (completionOpt.isPresent() && Boolean.TRUE.equals(completionOpt.get().getCompleted())) {
            return 20;
        }
        
        return 0;
    }
    
    /**
     * 根据评分确定状态
     * 
     * GREEN ≥ 80
     * YELLOW 50-79
     * GRAY < 50
     */
    private StabilityStatus determineStatus(int score) {
        if (score >= 80) {
            return StabilityStatus.GREEN;
        } else if (score >= 50) {
            return StabilityStatus.YELLOW;
        } else {
            return StabilityStatus.GRAY;
        }
    }
}
