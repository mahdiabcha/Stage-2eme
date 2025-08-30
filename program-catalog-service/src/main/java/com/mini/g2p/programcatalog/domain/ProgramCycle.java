package com.mini.g2p.programcatalog.domain;

import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="program_cycles", indexes=@Index(name="ix_cycle_program", columnList="programId"))
public class ProgramCycle {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  private Long programId;
  private String name;
  private LocalDate startDate;
  private LocalDate endDate;
  @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private CycleState state = CycleState.DRAFT;
  @Column(updatable=false) private Instant createdAt;
  @PrePersist void pre(){ if(createdAt==null) createdAt=Instant.now(); }
  // getters/setters
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public Long getProgramId(){return programId;} public void setProgramId(Long programId){this.programId=programId;}
  public String getName(){return name;} public void setName(String name){this.name=name;}
  public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate startDate){this.startDate=startDate;}
  public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate endDate){this.endDate=endDate;}
  public CycleState getState(){return state;} public void setState(CycleState state){this.state=state;}
  public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
}
