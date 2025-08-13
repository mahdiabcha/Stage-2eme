package com.mini.g2p.payment.domain;

import jakarta.persistence.*;

@Entity
public class PaymentInstruction {

  public enum Status { PENDING, SENT, SUCCESS, FAILED }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long batchId;
  private Long enrollmentId;           // optional for now
  private String beneficiaryUsername;

  private Double amount;
  private String currency;

  @Enumerated(EnumType.STRING)
  private Status status = Status.PENDING;

  private String bankRef;

  @Column(length = 500)
  private String failReason;

  // Getters / Setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public Long getBatchId() { return batchId; }
  public void setBatchId(Long batchId) { this.batchId = batchId; }

  public Long getEnrollmentId() { return enrollmentId; }
  public void setEnrollmentId(Long enrollmentId) { this.enrollmentId = enrollmentId; }

  public String getBeneficiaryUsername() { return beneficiaryUsername; }
  public void setBeneficiaryUsername(String beneficiaryUsername) { this.beneficiaryUsername = beneficiaryUsername; }

  public Double getAmount() { return amount; }
  public void setAmount(Double amount) { this.amount = amount; }

  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }

  public Status getStatus() { return status; }
  public void setStatus(Status status) { this.status = status; }

  public String getBankRef() { return bankRef; }
  public void setBankRef(String bankRef) { this.bankRef = bankRef; }

  public String getFailReason() { return failReason; }
  public void setFailReason(String failReason) { this.failReason = failReason; }
}
