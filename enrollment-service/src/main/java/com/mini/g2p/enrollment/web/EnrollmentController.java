package com.mini.g2p.enrollment.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mini.g2p.enrollment.domain.Enrollment;
import com.mini.g2p.enrollment.dto.BeneficiaryDtos.BeneficiaryItem;
import com.mini.g2p.enrollment.repo.EnrollmentRepository;
import com.mini.g2p.enrollment.service.EligibilityService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping
public class EnrollmentController {

  private final WebClient profileClient;
  private final WebClient programClient;
  private final EnrollmentRepository repo;
  private final EligibilityService eligibility;
  private final ObjectMapper om;

  public EnrollmentController(WebClient profileClient,
                              WebClient programCatalogClient,
                              EnrollmentRepository repo,
                              EligibilityService eligibility,
                              ObjectMapper objectMapper) {
    this.profileClient = profileClient;
    this.programClient = programCatalogClient;
    this.repo = repo;
    this.eligibility = eligibility;
    this.om = objectMapper;
  }

  @PostMapping("/programs/{programId}/eligibility/check")
  public Map<String, Object> check(@RequestHeader HttpHeaders headers,
                                   @PathVariable long programId) {
    String user = SecurityHelpers.currentUser(headers);

    JsonNode profile = safeFetchProfile(user, headers);
    JsonNode program = fetchProgram(programId, headers);
    String rulesJson = program.path("rulesJson").isMissingNode() ? "{}" : program.get("rulesJson").asText("{}");

    var ctx = eligibility.contextFromProfile(profile);
    var res = eligibility.evaluate(ctx, rulesJson);

    return Map.of("eligible", res.eligible, "reason", res.reason);
  }

  @PostMapping("/programs/{programId}/enroll")
  public ResponseEntity<?> enroll(@RequestHeader HttpHeaders headers,
                                  @PathVariable long programId,
                                  @RequestBody(required = false) Map<String, Object> body) {
    String user = SecurityHelpers.currentUser(headers);

    JsonNode profile = safeFetchProfile(user, headers);
    JsonNode program = fetchProgram(programId, headers);

    String state = program.path("state").asText("DRAFT");
    if (!"ACTIVE".equalsIgnoreCase(state)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Program is not ACTIVE");
    }

    String rulesJson = program.path("rulesJson").isMissingNode() ? "{}" : program.get("rulesJson").asText("{}");
    var ctx = eligibility.contextFromProfile(profile);
    var res = eligibility.evaluate(ctx, rulesJson);
    if (!res.eligible) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Not eligible: " + res.reason);
    }

    Enrollment e = new Enrollment();
    e.setProgramId(programId);
    e.setCitizenUsername(user);
    e.setUsername(user); // satisfy legacy NOT NULL "username"
    e.setStatus(Enrollment.Status.PENDING);
    if (body != null && body.get("note") instanceof String n) e.setNote(n);

    e.setEligibilityPassed(true);
    e.setEligibilityReason(res.reason);
    e.setEligibilityCheckedAt(Instant.now());

    e = repo.save(e);

    Map<String,Object> out = Map.of(
        "id", e.getId(),
        "programId", e.getProgramId(),
        "username", e.getCitizenUsername(),
        "status", e.getStatus().name(),
        "note", e.getNote(),
        "eligibilityPassed", e.isEligibilityPassed(),
        "eligibilityReason", e.getEligibilityReason(),
        "createdAt", e.getCreatedAt()
    );
    return ResponseEntity.created(URI.create("/enrollments/" + e.getId())).body(out);
  }

  @PatchMapping("/enrollments/{id}/status")
  public Map<String, Object> updateStatus(@RequestHeader HttpHeaders headers,
                                          @PathVariable long id,
                                          @RequestParam("value") String value,
                                          @RequestBody(required = false) Map<String, Object> body) {
    if (!SecurityHelpers.hasRole(headers, "ADMIN") && !SecurityHelpers.hasRole(headers, "AGENT")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requires ADMIN or AGENT");
    }
    Enrollment e = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));

    Enrollment.Status newStatus;
    try {
      newStatus = Enrollment.Status.valueOf(value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status value");
    }
    e.setStatus(newStatus);
    if (body != null && body.get("note") instanceof String n) e.setNote(n);
    repo.save(e);

    return Map.of(
        "id", e.getId(),
        "programId", e.getProgramId(),
        "username", e.getCitizenUsername(),
        "status", e.getStatus().name(),
        "note", e.getNote(),
        "createdAt", e.getCreatedAt()
    );
  }

  @GetMapping("/programs/{programId}/beneficiaries")
  public List<BeneficiaryItem> listBeneficiaries(@RequestHeader HttpHeaders headers,
                                                 @PathVariable long programId,
                                                 @RequestParam(name = "status", required = false) String status) {
    if (!SecurityHelpers.hasRole(headers, "ADMIN") && !SecurityHelpers.hasRole(headers, "AGENT")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requires ADMIN or AGENT");
    }

    List<Enrollment> list;
    if (status == null || status.isBlank()) {
      list = repo.findByProgramIdOrderByCreatedAtDesc(programId);
    } else {
      Enrollment.Status st;
      try { st = Enrollment.Status.valueOf(status.toUpperCase(Locale.ROOT)); }
      catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status"); }
      list = repo.findByProgramIdAndStatusOrderByCreatedAtDesc(programId, st);
    }

    return list.stream()
        .map(e -> new BeneficiaryItem(
            e.getId(),
            e.getProgramId(),
            e.getCitizenUsername(),
            e.getStatus().name(),
            e.getNote(),
            e.getCreatedAt()))
        .collect(Collectors.toList());
  }

  private JsonNode safeFetchProfile(String username, HttpHeaders incoming) {
    return profileClient.get()
        .uri("/profiles/{u}", username)
        .headers(h -> forwardAuthHeaders(incoming, h))
        .exchangeToMono(resp -> handleProfileResponse(resp, username))
        .block();
  }

  private Mono<JsonNode> handleProfileResponse(ClientResponse resp, String username) {
    if (resp.statusCode().is2xxSuccessful()) {
      return resp.bodyToMono(JsonNode.class);
    }
    if (resp.rawStatusCode() == 404) {
      ObjectNode minimal = om.createObjectNode();
      minimal.put("username", username);
      minimal.put("kycVerified", true);
      return Mono.just(minimal);
    }
    return resp.createException().flatMap(Mono::error);
  }

  private JsonNode fetchProgram(long programId, HttpHeaders incoming) {
    return programClient.get()
        .uri("/programs/{id}", programId)
        .headers(h -> forwardAuthHeaders(incoming, h))
        .retrieve()
        .bodyToMono(JsonNode.class)
        .blockOptional()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));
  }

  private void forwardAuthHeaders(HttpHeaders from, HttpHeaders to) {
    String u = from.getFirst("X-Auth-User");
    String r = from.getFirst("X-Auth-Roles");
    String auth = from.getFirst(HttpHeaders.AUTHORIZATION);
    if (u != null) to.set("X-Auth-User", u);
    if (r != null) to.set("X-Auth-Roles", r);
    if (auth != null) to.set(HttpHeaders.AUTHORIZATION, auth);
  }
}
