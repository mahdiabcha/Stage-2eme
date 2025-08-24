package com.mini.g2p.programcatalog.controllers;

import com.mini.g2p.programcatalog.domain.Entitlement;
import com.mini.g2p.programcatalog.domain.EntitlementState;
import com.mini.g2p.programcatalog.dto.EntitlementDtos;
import com.mini.g2p.programcatalog.repo.*;
import com.mini.g2p.programcatalog.service.EntitlementService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
public class EntitlementController {

  private final EntitlementService service;
  private final EntitlementRepository entRepo;
  private final ProgramCycleRepository cycleRepo;

  public EntitlementController(EntitlementService service, EntitlementRepository entRepo, ProgramCycleRepository cycleRepo) {
    this.service = service; this.entRepo = entRepo; this.cycleRepo = cycleRepo;
  }

  @PostMapping({"/cycles/{cycleId}/entitlements/generate",
                "/programs/cycles/{cycleId}/entitlements/generate"})
  public List<Entitlement> generate(@PathVariable Long cycleId,
                                    @RequestBody EntitlementDtos.GenerateReq req) {
    return service.generate(cycleId, req);
  }

  @GetMapping({"/cycles/{cycleId}/entitlements",
               "/programs/cycles/{cycleId}/entitlements"})
  public List<Entitlement> list(@PathVariable Long cycleId) {
    return entRepo.findByCycleIdOrderByIdAsc(cycleId);
  }

  // Internal API for PaymentService
  @GetMapping("/internal/entitlements/approved")
  public List<EntitlementDtos.ApprovedEntitlement> approved(@RequestParam Long cycleId) {
    var cycle = cycleRepo.findById(cycleId).orElseThrow();
    var list = entRepo.findByCycleIdAndState(cycle.getId(), EntitlementState.APPROVED);
    return list.stream()
      .map(e -> new EntitlementDtos.ApprovedEntitlement(
          e.getId(), e.getBeneficiaryUsername(), e.getAmount(), e.getCurrency()))
      .toList();
  }
}
