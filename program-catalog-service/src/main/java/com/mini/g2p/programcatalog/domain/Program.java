package com.mini.g2p.programcatalog.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "programs")
public class Program {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(length = 2000)
  private String description;

  @Column(length = 8000)
  private String rulesJson;

  @Enumerated(EnumType.STRING)
  @Column(name = "state")
  private ProgramState state = ProgramState.DRAFT;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  // getters/setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getRulesJson() { return rulesJson; }
  public void setRulesJson(String rulesJson) { this.rulesJson = rulesJson; }

  public ProgramState getState() { return state; }
  public void setState(ProgramState state) { this.state = state; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
