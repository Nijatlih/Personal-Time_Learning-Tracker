package com.nijat.ptlt.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "routine_definitions")
public class RoutineDefinition extends BaseEntity {

  public enum RoutineType { MORNING, DAYTIME, EVENING }

  @Id
  @GeneratedValue
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "workspace_id", nullable = false)
  private Workspace workspace;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RoutineType routineType;

  @Column(nullable = false)
  private String itemKey;

  @Column(nullable = false)
  private String label;

  @Column(columnDefinition = "text")
  private String description;

  @Column(nullable = false)
  private int sortOrder;

  @Column(nullable = false)
  private boolean active = true;

  public UUID getId() { return id; }
  public Workspace getWorkspace() { return workspace; }
  public void setWorkspace(Workspace workspace) { this.workspace = workspace; }
  public RoutineType getRoutineType() { return routineType; }
  public void setRoutineType(RoutineType routineType) { this.routineType = routineType; }
  public String getItemKey() { return itemKey; }
  public void setItemKey(String itemKey) { this.itemKey = itemKey; }
  public String getLabel() { return label; }
  public void setLabel(String label) { this.label = label; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public int getSortOrder() { return sortOrder; }
  public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
}
