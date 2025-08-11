package com.mini.g2p.enrollment.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.g2p.enrollment.domain.Enrollment;
import com.mini.g2p.enrollment.repo.EnrollmentRepository;
import com.mini.g2p.enrollment.service.EligibilityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;

@RestController
public class EnrollmentController {
  private final EnrollmentRepository enrollments;
  private final WebClient programClient, profileClient;
  private final EligibilityService eligibility;
  private final ObjectMapper om = new ObjectMapper();
  private final String programBase, profileBase;

  public EnrollmentController(EnrollmentRepository repo,
                              WebClient programCatalogClient,
                              WebClient profileClient,
                              EligibilityService eligibility,
                              @Value("${clients.programCatalogBaseUrl}") String programBase,
                              @Value("${clients.profileBaseUrl}") String profileBase) {
    this.enrollments = repo; this.programClient=programCatalogClient; this.profileClient=profileClient;
    this.eligibility=eligibility; this.programBase=programBase; this.profileBase=profileBase;
  }

  @GetMapping("/enrollments/my")
  public ResponseEntity<?> my(@RequestHeader HttpHeaders h){
    String user = SecurityHelpers.currentUser(h);
    if (user==null) return ResponseEntity.status(401).body("No identity");
    return ResponseEntity.ok(enrollments.findByCitizenUsername(user));
  }

  record EligRes(boolean eligible, List<String> reasons) {}

  @PostMapping("/programs/{id}/eligibility/check")
  public ResponseEntity<?> check(@RequestHeader HttpHeaders h, @PathVariable Long id) {
    String user = SecurityHelpers.currentUser(h);
    if (user==null) return ResponseEntity.status(401).build();

    JsonNode program = programClient.get().uri("/programs/{id}", id)
      .accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(JsonNode.class).block();

    JsonNode profile = profileClient.get().uri("/profiles/me")
      .header("X-Auth-User", user).header("X-Auth-Roles", String.join(",", SecurityHelpers.roles(h)))
      .accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(JsonNode.class).block();

    if (profile==null || profile.isNull()) return ResponseEntity.badRequest().body(new EligRes(false, List.of("Profile not found")));

    String rulesJson = program.path("rulesJson").asText(null);
    var ctx = eligibility.contextFromProfile(profile);
    var res = eligibility.evaluate(rulesJson, ctx);
    return ResponseEntity.ok(new EligRes(res.eligible(), res.reasons()));
  }

  record EnrollResponse(Long enrollmentId, Enrollment.Status status, boolean eligibilityPassed, List<String> reasons) {}
  record DecisionRequest(String note){}

  @PostMapping("/programs/{id}/enroll")
  public ResponseEntity<?> enroll(@RequestHeader HttpHeaders h, @PathVariable Long id) {
    String user = SecurityHelpers.currentUser(h);
    if (user==null) return ResponseEntity.status(401).body("No identity");
    if (!SecurityHelpers.hasRole(h,"CITIZEN")) return ResponseEntity.status(403).body("CITIZEN only");

    JsonNode program = programClient.get().uri("/programs/{id}", id).retrieve().bodyToMono(JsonNode.class).block();
    JsonNode profile = profileClient.get().uri("/profiles/me")
      .header("X-Auth-User", user).header("X-Auth-Roles", String.join(",", SecurityHelpers.roles(h)))
      .retrieve().bodyToMono(JsonNode.class).block();

    if (profile==null || profile.isNull()) return ResponseEntity.badRequest().body("Profile not found");

    String rulesJson = program.path("rulesJson").asText(null);
    var ctx = eligibility.contextFromProfile(profile);
    var res = eligibility.evaluate(rulesJson, ctx);

    var e = new Enrollment();
    e.setProgramId(id);
    e.setCitizenUsername(user);
    e.setEligibilityPassed(res.eligible());
    try { e.setProfileSnapshotJson(om.writeValueAsString(profile)); } catch(Exception ignored){}
    try { e.setEligibilityReasonsJson(om.writeValueAsString(res.reasons())); } catch(Exception ignored){}
    e.setStatus(res.eligible() ? Enrollment.Status.PENDING : Enrollment.Status.AUTO_REJECTED);
    enrollments.save(e);

    return ResponseEntity.ok(new EnrollResponse(e.getId(), e.getStatus(), Boolean.TRUE.equals(e.getEligibilityPassed()), res.reasons()));
  }

  @PatchMapping("/enrollments/{id}/status")
  public ResponseEntity<?> decide(@RequestHeader HttpHeaders h, @PathVariable Long id, @RequestParam String value, @RequestBody(required=false) DecisionRequest req) {
    if (!(SecurityHelpers.hasRole(h,"ADMIN") || SecurityHelpers.hasRole(h,"AGENT")))
      return ResponseEntity.status(403).body("AGENT or ADMIN required");
    var e = enrollments.findById(id).orElseThrow();
    if ("APPROVED".equalsIgnoreCase(value)) e.setStatus(Enrollment.Status.APPROVED);
    else if ("REJECTED".equalsIgnoreCase(value)) e.setStatus(Enrollment.Status.REJECTED);
    else return ResponseEntity.badRequest().body("value must be APPROVED or REJECTED");
    e.setDecidedAt(Instant.now());
    e.setDecidedBy(SecurityHelpers.currentUser(h));
    if (req!=null) e.setDecisionNote(req.note());
    enrollments.save(e);
    return ResponseEntity.ok(e);
  }
}
