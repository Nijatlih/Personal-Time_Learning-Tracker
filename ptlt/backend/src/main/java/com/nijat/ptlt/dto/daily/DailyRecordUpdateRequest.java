package com.nijat.ptlt.dto.daily;

import com.nijat.ptlt.entity.DailyRecord;
import java.util.Map;

public class DailyRecordUpdateRequest {
  private DailyRecord.CognitiveEnergy cognitiveEnergy;
  private Boolean alignedWithGoals;
  private Boolean coreTaskCompleted;
  private String notes;
  private Map<String, Boolean> routineCompletions;
  private Long version;

  public DailyRecord.CognitiveEnergy getCognitiveEnergy() { return cognitiveEnergy; }
  public void setCognitiveEnergy(DailyRecord.CognitiveEnergy cognitiveEnergy) { this.cognitiveEnergy = cognitiveEnergy; }
  public Boolean getAlignedWithGoals() { return alignedWithGoals; }
  public void setAlignedWithGoals(Boolean alignedWithGoals) { this.alignedWithGoals = alignedWithGoals; }
  public Boolean getCoreTaskCompleted() { return coreTaskCompleted; }
  public void setCoreTaskCompleted(Boolean coreTaskCompleted) { this.coreTaskCompleted = coreTaskCompleted; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public Map<String, Boolean> getRoutineCompletions() { return routineCompletions; }
  public void setRoutineCompletions(Map<String, Boolean> routineCompletions) { this.routineCompletions = routineCompletions; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}
