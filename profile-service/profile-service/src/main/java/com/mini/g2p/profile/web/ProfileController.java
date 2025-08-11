package com.mini.g2p.profile.web;

import com.mini.g2p.profile.domain.CitizenProfile;
import com.mini.g2p.profile.repo.CitizenProfileRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
public class ProfileController {
  private final CitizenProfileRepository repo;
  public ProfileController(CitizenProfileRepository repo){this.repo=repo;}

  @GetMapping("/profiles/me")
  public ResponseEntity<?> me(@RequestHeader HttpHeaders headers) {
    String user = SecurityHelpers.currentUser(headers);
    if (user==null) return ResponseEntity.status(401).body("No identity");
    return ResponseEntity.ok(repo.findByUsername(user).orElse(null));
  }

  public record Upsert(LocalDate birthDate, String gender, String governorate, Integer householdSize, Double incomeMonthly, Boolean kycVerified){}
  @PostMapping("/profiles/me")
  public ResponseEntity<?> upsert(@RequestHeader HttpHeaders headers, @RequestBody @Valid Upsert d) {
    String user = SecurityHelpers.currentUser(headers);
    if (user==null) return ResponseEntity.status(401).body("No identity");
    var p = repo.findByUsername(user).orElseGet(CitizenProfile::new);
    p.setUsername(user);
    p.setBirthDate(d.birthDate()); p.setGender(d.gender());
    p.setGovernorate(d.governorate()); p.setHouseholdSize(d.householdSize());
    p.setIncomeMonthly(d.incomeMonthly()); p.setKycVerified(d.kycVerified());
    repo.save(p);
    return ResponseEntity.ok(p);
  }
}
