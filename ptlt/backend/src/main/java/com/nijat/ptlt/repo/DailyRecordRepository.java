package com.nijat.ptlt.repo;

import com.nijat.ptlt.entity.DailyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyRecordRepository extends JpaRepository<DailyRecord, UUID> {
  Optional<DailyRecord> findByWorkspaceIdAndDate(UUID workspaceId, LocalDate date);
  List<DailyRecord> findByWorkspaceIdAndDateBetween(UUID workspaceId, LocalDate from, LocalDate to);
}
