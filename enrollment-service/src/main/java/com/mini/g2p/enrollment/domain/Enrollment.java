package com.mini.g2p.enrollment.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Enrollment {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long programId;
  private String citizenUsername;

  @Enumerated(EnumType.STRING)
  private Status status = Status.PENDING;

  private Boolean eligibilityPassed;
  @Column(columnDefinition="TEXT") private String eligibilityReasonsJson;
  @Column(columnDefinition="TEXT") private String profileSnapshotJson;

  private Instant createdAt = Instant.now();
  private Instant decidedAt;
  private String decidedBy;
  @Column(length = 1000) private String decisionNote;

  public enum Status { PENDING, AUTO_REJECTED, APPROVED, REJECTED }

  // getters/setters...
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public Long getProgramId(){return programId;} public void setProgramId(Long p){this.programId=p;}
  public String getCitizenUsername(){return citizenUsername;} public void setCitizenUsername(String u){this.citizenUsername=u;}
  public Status getStatus(){return status;} public void setStatus(Status s){this.status=s;}
  public Boolean getEligibilityPassed(){return eligibilityPassed;} public void setEligibilityPassed(Boolean e){this.eligibilityPassed=e;}
  public String getEligibilityReasonsJson(){return eligibilityReasonsJson;} public void setEligibilityReasonsJson(String j){this.eligibilityReasonsJson=j;}
  public String getProfileSnapshotJson(){return profileSnapshotJson;} public void setProfileSnapshotJson(String j){this.profileSnapshotJson=j;}
  public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant i){this.createdAt=i;}
  public Instant getDecidedAt(){return decidedAt;} public void setDecidedAt(Instant i){this.decidedAt=i;}
  public String getDecidedBy(){return decidedBy;} public void setDecidedBy(String d){this.decidedBy=d;}
  public String getDecisionNote(){return decisionNote;} public void setDecisionNote(String n){this.decisionNote=n;}
}
