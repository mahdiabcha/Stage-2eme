package com.mini.g2p.payment.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class PaymentBatch {

  public enum Status { PENDING, PROCESSING, COMPLETED, FAILED }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long programId;
  private String createdBy;
  private Instant createdAt = Instant.now();

  @Enumerated(EnumType.STRING)
  private Status status = Status.PENDING;

  private Integer totalCount = 0;
  private Integer successCount = 0;
  private Integer failedCount = 0;

  // Getters / Setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public Long getProgramId() { return programId; }
  public void setProgramId(Long programId) { this.programId = programId; }

  public String getCreatedBy() { return createdBy; }
  public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public Status getStatus() { return status; }
  public void setStatus(Status status) { this.status = status; }

  public Integer getTotalCount() { return totalCount; }
  public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }

  public Integer getSuccessCount() { return successCount; }
  public void setSuccessCount(Integer successCount) { this.successCount = successCount; }

  public Integer getFailedCount() { return failedCount; }
  public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }
}
