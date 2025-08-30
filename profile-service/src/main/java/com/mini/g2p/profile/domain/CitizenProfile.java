package com.mini.g2p.profile.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "citizen_profiles",
       indexes = {
         @Index(name = "ix_profile_username", columnList = "username", unique = true)
       })
public class CitizenProfile {

  public enum PaymentMethod { NONE, BANK, WALLET }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  private String firstName;
  private String lastName;
  private String gender; // "M" / "F" / autre
  private LocalDate dateOfBirth;
  private String governorate;
  private String district;
  private Integer householdSize;
  private Integer incomeMonthly;
  private Boolean kycVerified;

  @Enumerated(EnumType.STRING)
  @Column(length = 16)
  private PaymentMethod paymentMethod = PaymentMethod.NONE;

  // BANK
  private String bankName;
  private String iban;
  private String accountHolder;

  // WALLET
  private String walletProvider;
  private String walletNumber;

  // getters/setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }

  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }

  public String getGender() { return gender; }
  public void setGender(String gender) { this.gender = gender; }

  public LocalDate getDateOfBirth() { return dateOfBirth; }
  public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

  public String getGovernorate() { return governorate; }
  public void setGovernorate(String governorate) { this.governorate = governorate; }

  public String getDistrict() { return district; }
  public void setDistrict(String district) { this.district = district; }

  public Integer getHouseholdSize() { return householdSize; }
  public void setHouseholdSize(Integer householdSize) { this.householdSize = householdSize; }

  public Integer getIncomeMonthly() { return incomeMonthly; }
  public void setIncomeMonthly(Integer incomeMonthly) { this.incomeMonthly = incomeMonthly; }

  public Boolean getKycVerified() { return kycVerified; }
  public void setKycVerified(Boolean kycVerified) { this.kycVerified = kycVerified; }

  public PaymentMethod getPaymentMethod() { return paymentMethod; }
  public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

  public String getBankName() { return bankName; }
  public void setBankName(String bankName) { this.bankName = bankName; }

  public String getIban() { return iban; }
  public void setIban(String iban) { this.iban = iban; }

  public String getAccountHolder() { return accountHolder; }
  public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }

  public String getWalletProvider() { return walletProvider; }
  public void setWalletProvider(String walletProvider) { this.walletProvider = walletProvider; }

  public String getWalletNumber() { return walletNumber; }
  public void setWalletNumber(String walletNumber) { this.walletNumber = walletNumber; }
}
