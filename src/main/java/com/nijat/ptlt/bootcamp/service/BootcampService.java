package com.nijat.ptlt.bootcamp.service;

import com.nijat.ptlt.bootcamp.dto.BootcampModuleResponse;
import com.nijat.ptlt.bootcamp.dto.DailyModuleEntryRequest;
import com.nijat.ptlt.bootcamp.dto.DailyModuleEntryResponse;
import com.nijat.ptlt.bootcamp.entity.BootcampModule;
import com.nijat.ptlt.bootcamp.entity.DailyModuleEntry;
import com.nijat.ptlt.bootcamp.repository.BootcampModuleRepository;
import com.nijat.ptlt.bootcamp.repository.DailyModuleEntryRepository;
import com.nijat.ptlt.common.exception.ForbiddenException;
import com.nijat.ptlt.common.exception.NotFoundException;
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
 * Bootcamp服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BootcampService {
    private final BootcampModuleRepository bootcampModuleRepository;
    private final DailyModuleEntryRepository dailyModuleEntryRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    
    /**
     * 获取所有bootcamp模块（系统级数据）
     */
    @Transactional(readOnly = true)
    public List<BootcampModuleResponse> listModules() {
        List<BootcampModule> modules = bootcampModuleRepository.findAllByOrderByOrderIndexAsc();
        return modules.stream()
                .map(this::toModuleResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 记录每日模块完成情况
     */
    @Transactional
    public DailyModuleEntryResponse recordEntry(
            DailyModuleEntryRequest request, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        // 校验模块存在
        BootcampModule module = bootcampModuleRepository.findById(request.getModuleId())
                .orElseThrow(() -> new NotFoundException("Bootcamp module not found"));
        
        // 查找或创建entry
        Optional<DailyModuleEntry> existingOpt = dailyModuleEntryRepository
                .findByWorkspaceIdAndDateAndModuleIdAndDeletedAtIsNull(
                        workspaceId, request.getDate(), request.getModuleId());
        
        DailyModuleEntry entry;
        if (existingOpt.isPresent()) {
            entry = existingOpt.get();
            entry.setCompleted(request.getCompleted());
            if (request.getNotes() != null) {
                entry.setNotes(request.getNotes());
            }
        } else {
            entry = DailyModuleEntry.builder()
                    .workspaceId(workspaceId)
                    .moduleId(request.getModuleId())
                    .date(request.getDate())
                    .completed(request.getCompleted())
                    .notes(request.getNotes())
                    .build();
        }
        
        entry = dailyModuleEntryRepository.save(entry);
        
        log.info("Daily module entry recorded: workspace={}, module={}, date={}, completed={}",
                workspaceId, request.getModuleId(), request.getDate(), request.getCompleted());
        
        return toEntryResponse(entry);
    }
    
    /**
     * 获取某一天的模块记录
     */
    @Transactional(readOnly = true)
    public List<DailyModuleEntryResponse> getEntriesByDate(
            LocalDate date, UUID workspaceId, UUID userId) {
        // 校验workspace成员身份
        validateWorkspaceAccess(workspaceId, userId);
        
        List<DailyModuleEntry> entries = dailyModuleEntryRepository
                .findByWorkspaceIdAndDateAndDeletedAtIsNull(workspaceId, date);
        
        return entries.stream()
                .map(this::toEntryResponse)
                .collect(Collectors.toList());
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
    
    /**
     * 转换为模块响应DTO
     */
    private BootcampModuleResponse toModuleResponse(BootcampModule module) {
        return BootcampModuleResponse.builder()
                .id(module.getId())
                .code(module.getCode())
                .name(module.getName())
                .description(module.getDescription())
                .orderIndex(module.getOrderIndex())
                .createdAt(module.getCreatedAt())
                .updatedAt(module.getUpdatedAt())
                .build();
    }
    
    /**
     * 转换为entry响应DTO
     */
    private DailyModuleEntryResponse toEntryResponse(DailyModuleEntry entry) {
        return DailyModuleEntryResponse.builder()
                .id(entry.getId())
                .workspaceId(entry.getWorkspaceId())
                .moduleId(entry.getModuleId())
                .date(entry.getDate())
                .completed(entry.getCompleted())
                .notes(entry.getNotes())
                .version(entry.getVersion())
                .build();
    }
}
