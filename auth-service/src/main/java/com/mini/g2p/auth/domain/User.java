package com.mini.g2p.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="users",
  uniqueConstraints={
    @UniqueConstraint(name="uk_users_username", columnNames="username"),
    @UniqueConstraint(name="uk_users_national_id", columnNames="national_id")
  })
public class User {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false,length=100) private String username;
  @Column(name="national_id",nullable=false,length=20) private String nationalId;
  @Column(name="password_hash",nullable=false,length=120) private String passwordHash;

  @ElementCollection(fetch=FetchType.EAGER)
  @CollectionTable(name="user_roles", joinColumns=@JoinColumn(name="user_id"))
  @Column(name="role", length=40, nullable=false)
  private Set<String> roles = new HashSet<>();

  @Column(name="created_at", updatable=false) private Instant createdAt;

  public Long getId(){return id;}
  public String getUsername(){return username;}
  public void setUsername(String username){this.username=username;}
  public String getNationalId(){return nationalId;}
  public void setNationalId(String nationalId){this.nationalId=nationalId;}
  public String getPasswordHash(){return passwordHash;}
  public void setPasswordHash(String passwordHash){this.passwordHash=passwordHash;}
  public Set<String> getRoles(){return roles;}
  public void setRoles(Set<String> roles){this.roles=roles;}
  public Instant getCreatedAt(){return createdAt;}
  public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
}
