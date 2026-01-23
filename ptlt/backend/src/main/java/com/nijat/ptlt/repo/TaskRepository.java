package com.nijat.ptlt.repo;

import com.nijat.ptlt.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
  List<Task> findByWorkspaceId(UUID workspaceId);
  List<Task> findByWorkspaceIdAndQuadrant(UUID workspaceId, Task.Quadrant quadrant);
  List<Task> findByWorkspaceIdAndStatus(UUID workspaceId, Task.Status status);
}
