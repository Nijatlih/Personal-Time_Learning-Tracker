package com.nijat.ptlt.repo;

import com.nijat.ptlt.entity.RoutineCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoutineCompletionRepository extends JpaRepository<RoutineCompletion, UUID> {
  List<RoutineCompletion> findByDailyRecordId(UUID dailyRecordId);
  Optional<RoutineCompletion> findByDailyRecordIdAndRoutineDefinitionId(UUID dailyRecordId, UUID routineDefinitionId);
}
