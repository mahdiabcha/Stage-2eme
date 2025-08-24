package com.mini.g2p.programcatalog.service;

import com.mini.g2p.programcatalog.domain.*;
import com.mini.g2p.programcatalog.dto.EntitlementDtos;
import com.mini.g2p.programcatalog.repo.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntitlementService {

  private final ProgramCycleRepository cycles;
  private final EntitlementRepository entitlements;

  public EntitlementService(ProgramCycleRepository cycles, EntitlementRepository entitlements) {
    this.cycles = cycles;
    this.entitlements = entitlements;
  }

  @Transactional
  public List<Entitlement> generate(Long cycleId, EntitlementDtos.GenerateReq req) {
    var cycle = cycles.findById(cycleId).orElseThrow();
    var list = new ArrayList<Entitlement>();

    for (var it : req.items()) {
      var e = new Entitlement();
      e.setCycleId(cycle.getId());
      e.setProgramId(cycle.getProgramId());
      e.setBeneficiaryUsername(it.username());
      e.setAmount(it.amount());     // Double ou BigDecimal selon ton entity
      e.setCurrency(it.currency());
      e.setValidFrom(it.validFrom());
      e.setValidUntil(it.validUntil());
      e.setCode(UUID.randomUUID().toString());
      e.setCreatedAt(Instant.now());
      e.setState(cycle.getState() == CycleState.APPROVED
          ? EntitlementState.APPROVED
          : EntitlementState.DRAFT);
      list.add(entitlements.save(e));
    }
    return list;
  }
}
