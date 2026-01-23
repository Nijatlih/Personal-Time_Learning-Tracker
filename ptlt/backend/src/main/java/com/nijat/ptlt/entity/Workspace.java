package com.nijat.ptlt.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
public class Workspace extends BaseEntity {

  public enum Type { PERSONAL, TEAM }

  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Type type = Type.PERSONAL;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  public UUID getId() { return id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public Type getType() { return type; }
  public void setType(Type type) { this.type = type; }
  public User getOwner() { return owner; }
  public void setOwner(User owner) { this.owner = owner; }
}
