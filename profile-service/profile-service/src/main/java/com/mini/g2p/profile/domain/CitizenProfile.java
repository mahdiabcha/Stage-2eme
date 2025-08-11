package com.mini.g2p.profile.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class CitizenProfile {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false, unique = true) private String username;
  private LocalDate birthDate;
  private String gender;
  private String governorate;
  private Integer householdSize;
  private Double incomeMonthly;
  private Boolean kycVerified;

  // getters/setters...
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public String getUsername(){return username;} public void setUsername(String u){this.username=u;}
  public LocalDate getBirthDate(){return birthDate;} public void setBirthDate(LocalDate b){this.birthDate=b;}
  public String getGender(){return gender;} public void setGender(String g){this.gender=g;}
  public String getGovernorate(){return governorate;} public void setGovernorate(String g){this.governorate=g;}
  public Integer getHouseholdSize(){return householdSize;} public void setHouseholdSize(Integer h){this.householdSize=h;}
  public Double getIncomeMonthly(){return incomeMonthly;} public void setIncomeMonthly(Double i){this.incomeMonthly=i;}
  public Boolean getKycVerified(){return kycVerified;} public void setKycVerified(Boolean k){this.kycVerified=k;}
}
