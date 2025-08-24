package com.mini.g2p.programcatalog.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
public class Entitlement {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long cycleId;
  private Long programId;

  private String beneficiaryUsername;

  private Double amount;
  private String currency;

  private LocalDate validFrom;
  private LocalDate validUntil;

  private String code = UUID.randomUUID().toString();

  @Enumerated(EnumType.STRING)
  private EntitlementState state = EntitlementState.DRAFT;

  private Instant createdAt = Instant.now();

  // getters/setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getCycleId() { return cycleId; }
  public void setCycleId(Long cycleId) { this.cycleId = cycleId; }
  public Long getProgramId() { return programId; }
  public void setProgramId(Long programId) { this.programId = programId; }
  public String getBeneficiaryUsername() { return beneficiaryUsername; }
  public void setBeneficiaryUsername(String beneficiaryUsername) { this.beneficiaryUsername = beneficiaryUsername; }
  public Double getAmount() { return amount; }
  public void setAmount(Double amount) { this.amount = amount; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public LocalDate getValidFrom() { return validFrom; }
  public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
  public LocalDate getValidUntil() { return validUntil; }
  public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public EntitlementState getState() { return state; }
  public void setState(EntitlementState state) { this.state = state; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
