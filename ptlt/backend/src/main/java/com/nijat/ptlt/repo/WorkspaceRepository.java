package com.nijat.ptlt.repo;

import com.nijat.ptlt.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
  List<Workspace> findByOwnerId(UUID ownerId);
}
