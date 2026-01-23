package com.nijat.ptlt.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "routine_completions", uniqueConstraints = @UniqueConstraint(columnNames = {"daily_record_id", "routine_definition_id"}))
public class RoutineCompletion extends BaseEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "daily_record_id", nullable = false)
  private DailyRecord dailyRecord;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "routine_definition_id", nullable = false)
  private RoutineDefinition routineDefinition;

  @Column(nullable = false)
  private boolean completed = false;

  private Instant completedAt;

  @Column(columnDefinition = "text")
  private String notes;

  public UUID getId() { return id; }
  public DailyRecord getDailyRecord() { return dailyRecord; }
  public void setDailyRecord(DailyRecord dailyRecord) { this.dailyRecord = dailyRecord; }
  public RoutineDefinition getRoutineDefinition() { return routineDefinition; }
  public void setRoutineDefinition(RoutineDefinition routineDefinition) { this.routineDefinition = routineDefinition; }
  public boolean isCompleted() { return completed; }
  public void setCompleted(boolean completed) { this.completed = completed; }
  public Instant getCompletedAt() { return completedAt; }
  public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
}
