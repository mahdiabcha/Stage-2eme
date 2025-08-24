package com.mini.g2p.programcatalog.controllers;

import com.mini.g2p.programcatalog.domain.CycleState;
import com.mini.g2p.programcatalog.domain.ProgramCycle;
import com.mini.g2p.programcatalog.domain.ProgramState;
import com.mini.g2p.programcatalog.dto.CycleDtos.ChangeStateReq;
import com.mini.g2p.programcatalog.dto.CycleDtos.CreateCycleReq;
import com.mini.g2p.programcatalog.repo.ProgramCycleRepository;
import com.mini.g2p.programcatalog.repo.ProgramRepository;
import java.util.EnumSet;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProgramCycleController {

  private final ProgramCycleRepository cycles;
  private final ProgramRepository programs;

  public ProgramCycleController(ProgramCycleRepository cycles, ProgramRepository programs) {
    this.cycles = cycles; this.programs = programs;
  }

@PostMapping("/programs/{programId}/cycles")
  public ResponseEntity<?> create(@PathVariable Long programId, @RequestBody CreateCycleReq req) {

    var p = programs.findById(programId).orElse(null);
    if (p == null) return ResponseEntity.notFound().build();

    var state = p.getState() == null ? ProgramState.DRAFT : p.getState();
    if (state != ProgramState.ACTIVE) {
      // 409 Conflict is a good fit here
      return ResponseEntity.status(409).body("Program must be ACTIVE to create cycles");
    }

    var c = new ProgramCycle();
    c.setProgramId(programId);
    c.setName(req.name());
    c.setStartDate(req.startDate());
    c.setEndDate(req.endDate());
    c.setState(CycleState.DRAFT);
    return ResponseEntity.ok(cycles.save(c));
  }

  @GetMapping("/programs/{programId}/cycles")
  public List<ProgramCycle> list(@PathVariable Long programId) {
    return cycles.findByProgramIdOrderByIdDesc(programId);
  }

  @PatchMapping({"/cycles/{id}/state", "/programs/cycles/{id}/state"})
  public ResponseEntity<?> changeState(
      @PathVariable Long id,
      @RequestParam(name = "value", required = false) CycleState valueFromQuery,
      @RequestBody(required = false) ChangeStateReq body) {

    var c = cycles.findById(id).orElse(null);
    if (c == null) return ResponseEntity.notFound().build();

    var newState = valueFromQuery != null ? valueFromQuery : (body != null ? body.value() : null);
    if (newState == null) return ResponseEntity.badRequest().body("Missing state value");

    if (!allowed(c.getState(), newState)) return ResponseEntity.unprocessableEntity().body("Invalid transition");

    c.setState(newState);
    return ResponseEntity.ok(cycles.save(c));
  }

  private boolean allowed(CycleState from, CycleState to) {
    return switch (from) {
      case DRAFT -> EnumSet.of(CycleState.TO_APPROVE).contains(to);
      case TO_APPROVE -> EnumSet.of(CycleState.APPROVED).contains(to);
      case APPROVED -> EnumSet.of(CycleState.DISTRIBUTED).contains(to);
      case DISTRIBUTED -> EnumSet.of(CycleState.ENDED).contains(to);
      case CANCELED, ENDED -> false;
    };
  }
}
