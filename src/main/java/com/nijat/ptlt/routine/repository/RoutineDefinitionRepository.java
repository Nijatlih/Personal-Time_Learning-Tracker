package com.nijat.ptlt.routine.repository;

import com.nijat.ptlt.routine.entity.RoutineDefinition;
import com.nijat.ptlt.routine.entity.RoutineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Routine定义Repository
 */
@Repository
public interface RoutineDefinitionRepository extends JpaRepository<RoutineDefinition, UUID> {
    /**
     * 根据workspace_id和type查找（用于获取早晨/晚上routine）
     */
    Optional<RoutineDefinition> findByWorkspaceIdAndTypeAndDeletedAtIsNull(
            UUID workspaceId, RoutineType type);
    
    /**
     * 查找workspace的所有routine定义
     */
    List<RoutineDefinition> findByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);
    
    /**
     * 根据ID和workspace_id查找（用于多租户隔离）
     */
    Optional<RoutineDefinition> findByIdAndWorkspaceIdAndDeletedAtIsNull(UUID id, UUID workspaceId);
}
