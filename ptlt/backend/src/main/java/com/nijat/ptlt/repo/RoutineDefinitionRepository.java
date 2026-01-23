package com.nijat.ptlt.repo;

import com.nijat.ptlt.entity.RoutineDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoutineDefinitionRepository extends JpaRepository<RoutineDefinition, UUID> {
  List<RoutineDefinition> findByWorkspaceIdOrderByRoutineTypeAscSortOrderAsc(UUID workspaceId);
  Optional<RoutineDefinition> findByWorkspaceIdAndItemKey(UUID workspaceId, String itemKey);
}
