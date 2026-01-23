package com.nijat.ptlt.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_records", uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "date"}))
public class DailyRecord extends BaseEntity {

  public enum CognitiveEnergy { LOW, MEDIUM, HIGH }
  public enum DailyStatus { GREEN, YELLOW, GRAY }

  @Id
  @GeneratedValue
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "workspace_id", nullable = false)
  private Workspace workspace;

  @Column(nullable = false)
  private LocalDate date;

  @Enumerated(EnumType.STRING)
  private CognitiveEnergy cognitiveEnergy;

  private Boolean alignedWithGoals;
  private Boolean coreTaskCompleted;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(nullable = false, precision = 5, scale = 2)
  private BigDecimal morningCompletionRate = BigDecimal.ZERO;

  @Column(nullable = false, precision = 5, scale = 2)
  private BigDecimal eveningCompletionRate = BigDecimal.ZERO;

  @Column(nullable = false)
  private int stabilityScore = 0;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DailyStatus dailyStatus = DailyStatus.GRAY;

  @Column(nullable = false)
  private int energyBudgetStart = 100;

  @Column(nullable = false)
  private int energyBudgetCurrent = 100;

  @Column(nullable = false)
  private int energyBudgetConsumed = 0;

  @Version
  private long version;

  public UUID getId() { return id; }
  public Workspace getWorkspace() { return workspace; }
  public void setWorkspace(Workspace workspace) { this.workspace = workspace; }
  public LocalDate getDate() { return date; }
  public void setDate(LocalDate date) { this.date = date; }
  public CognitiveEnergy getCognitiveEnergy() { return cognitiveEnergy; }
  public void setCognitiveEnergy(CognitiveEnergy cognitiveEnergy) { this.cognitiveEnergy = cognitiveEnergy; }
  public Boolean getAlignedWithGoals() { return alignedWithGoals; }
  public void setAlignedWithGoals(Boolean alignedWithGoals) { this.alignedWithGoals = alignedWithGoals; }
  public Boolean getCoreTaskCompleted() { return coreTaskCompleted; }
  public void setCoreTaskCompleted(Boolean coreTaskCompleted) { this.coreTaskCompleted = coreTaskCompleted; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public BigDecimal getMorningCompletionRate() { return morningCompletionRate; }
  public void setMorningCompletionRate(BigDecimal morningCompletionRate) { this.morningCompletionRate = morningCompletionRate; }
  public BigDecimal getEveningCompletionRate() { return eveningCompletionRate; }
  public void setEveningCompletionRate(BigDecimal eveningCompletionRate) { this.eveningCompletionRate = eveningCompletionRate; }
  public int getStabilityScore() { return stabilityScore; }
  public void setStabilityScore(int stabilityScore) { this.stabilityScore = stabilityScore; }
  public DailyStatus getDailyStatus() { return dailyStatus; }
  public void setDailyStatus(DailyStatus dailyStatus) { this.dailyStatus = dailyStatus; }
  public int getEnergyBudgetStart() { return energyBudgetStart; }
  public void setEnergyBudgetStart(int energyBudgetStart) { this.energyBudgetStart = energyBudgetStart; }
  public int getEnergyBudgetCurrent() { return energyBudgetCurrent; }
  public void setEnergyBudgetCurrent(int energyBudgetCurrent) { this.energyBudgetCurrent = energyBudgetCurrent; }
  public int getEnergyBudgetConsumed() { return energyBudgetConsumed; }
  public void setEnergyBudgetConsumed(int energyBudgetConsumed) { this.energyBudgetConsumed = energyBudgetConsumed; }
  public long getVersion() { return version; }
}
