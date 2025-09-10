package com.mini.g2p.programcatalog.controllers;

import com.mini.g2p.programcatalog.domain.*;
import com.mini.g2p.programcatalog.repo.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.EnumSet;
import java.util.Map;

@RestController
public class ProgramCycleController {
  private final ProgramRepository programs;
  private final ProgramCycleRepository cycles;
  private final EntitlementRepository entRepo;

  public ProgramCycleController(ProgramRepository programs, ProgramCycleRepository cycles, EntitlementRepository entRepo) {
    this.programs=programs; this.cycles=cycles; this.entRepo=entRepo;
  }

  public record CreateCycleReq(String name, java.time.LocalDate startDate, java.time.LocalDate endDate){}

  @GetMapping("/programs/{programId}/cycles")
  public java.util.List<ProgramCycle> listByProgram(@PathVariable Long programId) {
    return cycles.findByProgramIdOrderByIdDesc(programId);
  }
  @PostMapping("/programs/{programId}/cycles")
  public ResponseEntity<?> create(@PathVariable Long programId, @RequestBody CreateCycleReq req) {
    var p = programs.findById(programId).orElse(null);
    if (p==null) return ResponseEntity.notFound().build();
    if (p.getState()!=ProgramState.ACTIVE) return ResponseEntity.status(409).body(Map.of("error","Program must be ACTIVE"));

    var c = new ProgramCycle();
    c.setProgramId(programId); c.setName(req.name()); c.setStartDate(req.startDate()); c.setEndDate(req.endDate());
    c.setState(CycleState.DRAFT);
    return ResponseEntity.ok(cycles.save(c));
  }

  @GetMapping("/cycles/{id}")
  public ResponseEntity<?> get(@PathVariable Long id) {
    return cycles.findById(id).<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  public record ChangeStateReq(CycleState value){}
  @PatchMapping("/cycles/{id}/state")
  public ResponseEntity<?> changeState(@PathVariable Long id, @RequestBody ChangeStateReq req){
    var c = cycles.findById(id).orElse(null);
    if (c==null) return ResponseEntity.notFound().build();
    var from = c.getState(); var to = req.value();
    if (!allowed(from,to)) return ResponseEntity.status(409).body(Map.of("error","transition not allowed"));

    if (to==CycleState.DISTRIBUTED) {
      long total = entRepo.countByCycleId(c.getId());
      long drafts = entRepo.countByCycleIdAndState(c.getId(), EntitlementState.DRAFT);
      long approved = entRepo.countByCycleIdAndState(c.getId(), EntitlementState.APPROVED);
      if (total==0 || approved==0 || drafts>0)
        return ResponseEntity.status(409).body(Map.of("error","Need APPROVED entitlements and no DRAFT","total",total,"approved",approved,"drafts",drafts));
    }
    c.setState(to); return ResponseEntity.ok(cycles.save(c));
  }

  private boolean allowed(CycleState from, CycleState to){
    return switch (from){
      case DRAFT -> EnumSet.of(CycleState.TO_APPROVE).contains(to);
      case TO_APPROVE -> EnumSet.of(CycleState.APPROVED).contains(to);
      case APPROVED -> EnumSet.of(CycleState.DISTRIBUTED).contains(to);
      case DISTRIBUTED -> EnumSet.of(CycleState.ENDED).contains(to);
      case CANCELED, ENDED -> false;
    };
  }
}
