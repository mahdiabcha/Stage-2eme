package com.mini.g2p.programcatalog.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="entitlements",
  uniqueConstraints = {
    @UniqueConstraint(name="ux_entitlement_code", columnNames="code"),
    @UniqueConstraint(name="ux_cycle_beneficiary", columnNames={"cycleId","beneficiaryUsername"})
  }
)
public class Entitlement {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private Long cycleId;
  private Long programId;
  private String beneficiaryUsername;
  private Double amount;
  private String currency;
  private LocalDate validFrom;
  private LocalDate validUntil;
  @Column(nullable=false, unique=true) private String code = UUID.randomUUID().toString();
  @Enumerated(EnumType.STRING) private EntitlementState state = EntitlementState.DRAFT;
  private Instant createdAt;

  @PrePersist void pre(){ if(createdAt==null) createdAt=Instant.now(); if(code==null) code=UUID.randomUUID().toString(); }

  // getters/setters omitted for brevity
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public Long getCycleId(){return cycleId;} public void setCycleId(Long cycleId){this.cycleId=cycleId;}
  public Long getProgramId(){return programId;} public void setProgramId(Long programId){this.programId=programId;}
  public String getBeneficiaryUsername(){return beneficiaryUsername;} public void setBeneficiaryUsername(String v){this.beneficiaryUsername=v;}
  public Double getAmount(){return amount;} public void setAmount(Double v){this.amount=v;}
  public String getCurrency(){return currency;} public void setCurrency(String v){this.currency=v;}
  public LocalDate getValidFrom(){return validFrom;} public void setValidFrom(LocalDate v){this.validFrom=v;}
  public LocalDate getValidUntil(){return validUntil;} public void setValidUntil(LocalDate v){this.validUntil=v;}
  public String getCode(){return code;} public void setCode(String code){this.code=code;}
  public EntitlementState getState(){return state;} public void setState(EntitlementState state){this.state=state;}
  public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){this.createdAt=v;}
}
