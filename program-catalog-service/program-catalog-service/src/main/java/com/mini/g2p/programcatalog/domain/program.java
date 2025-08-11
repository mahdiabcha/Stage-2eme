package com.mini.g2p.programcatalog.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Program {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String name;
  @Column(length = 2000) private String description;
  private LocalDate startDate;
  private LocalDate endDate;
  private boolean active = true;

  @Column(columnDefinition = "TEXT")
  private String rulesJson;

  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public String getName(){return name;} public void setName(String name){this.name=name;}
  public String getDescription(){return description;} public void setDescription(String d){this.description=d;}
  public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate s){this.startDate=s;}
  public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate e){this.endDate=e;}
  public boolean isActive(){return active;} public void setActive(boolean a){this.active=a;}
  public String getRulesJson(){return rulesJson;} public void setRulesJson(String r){this.rulesJson=r;}
}
