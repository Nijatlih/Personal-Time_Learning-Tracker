package com.nijat.ptlt.daily.repository;

import com.nijat.ptlt.daily.entity.Alert;
import com.nijat.ptlt.daily.entity.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Alert Repository
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    /**
     * 查找workspace的所有未读告警
     */
    List<Alert> findByWorkspaceIdAndIsReadFalseAndDeletedAtIsNullOrderByCreatedAtDesc(UUID workspaceId);
    
    /**
     * 查找workspace的所有告警
     */
    List<Alert> findByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID workspaceId);
    
    /**
     * 查找特定类型的未读告警
     */
    List<Alert> findByWorkspaceIdAndTypeAndIsReadFalseAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID workspaceId, AlertType type);
}
