package com.nijat.ptlt.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task extends BaseEntity {

  public enum Quadrant { Q1, Q2, Q3, Q4 }
  public enum Priority { P0, P1, P2, P3 }
  public enum EnergyRequired { HIGH, MEDIUM, LOW }
  public enum TaskType { ONE_OFF, RECURRING, PROJECT, HABIT }
  public enum Status { TODO, IN_PROGRESS, PAUSED, COMPLETED, CANCELLED }

  @Id
  @GeneratedValue
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "workspace_id", nullable = false)
  private Workspace workspace;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Quadrant quadrant;

  @Enumerated(EnumType.STRING)
  private Priority priority;

  @Enumerated(EnumType.STRING)
  private EnergyRequired energyRequired;

  @Enumerated(EnumType.STRING)
  private TaskType taskType = TaskType.ONE_OFF;

  private Integer estimateMinutes;
  private Instant deadline;

  // stored as comma-separated lists for simplicity
  @Column(columnDefinition = "text")
  private String moduleIds;

  @Column(columnDefinition = "text")
  private String tags;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.TODO;

  @Column(nullable = false)
  private int completionPercent = 0;

  private Integer actualMinutes;
  private Instant completedAt;

  @Version
  private long version;

  public UUID getId() { return id; }
  public Workspace getWorkspace() { return workspace; }
  public void setWorkspace(Workspace workspace) { this.workspace = workspace; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Quadrant getQuadrant() { return quadrant; }
  public void setQuadrant(Quadrant quadrant) { this.quadrant = quadrant; }
  public Priority getPriority() { return priority; }
  public void setPriority(Priority priority) { this.priority = priority; }
  public EnergyRequired getEnergyRequired() { return energyRequired; }
  public void setEnergyRequired(EnergyRequired energyRequired) { this.energyRequired = energyRequired; }
  public TaskType getTaskType() { return taskType; }
  public void setTaskType(TaskType taskType) { this.taskType = taskType; }
  public Integer getEstimateMinutes() { return estimateMinutes; }
  public void setEstimateMinutes(Integer estimateMinutes) { this.estimateMinutes = estimateMinutes; }
  public Instant getDeadline() { return deadline; }
  public void setDeadline(Instant deadline) { this.deadline = deadline; }
  public String getModuleIds() { return moduleIds; }
  public void setModuleIds(String moduleIds) { this.moduleIds = moduleIds; }
  public String getTags() { return tags; }
  public void setTags(String tags) { this.tags = tags; }
  public Status getStatus() { return status; }
  public void setStatus(Status status) { this.status = status; }
  public int getCompletionPercent() { return completionPercent; }
  public void setCompletionPercent(int completionPercent) { this.completionPercent = completionPercent; }
  public Integer getActualMinutes() { return actualMinutes; }
  public void setActualMinutes(Integer actualMinutes) { this.actualMinutes = actualMinutes; }
  public Instant getCompletedAt() { return completedAt; }
  public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
  public long getVersion() { return version; }
}
