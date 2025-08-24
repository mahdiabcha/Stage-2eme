package com.mini.g2p.programcatalog.controllers;

import com.mini.g2p.programcatalog.domain.Program;
import com.mini.g2p.programcatalog.domain.ProgramState;
import com.mini.g2p.programcatalog.dto.ProgramDtos.*;
import com.mini.g2p.programcatalog.repo.ProgramRepository;
import java.util.EnumSet;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProgramController {

  private final ProgramRepository programs;

  public ProgramController(ProgramRepository programs) { this.programs = programs; }

  @PostMapping("/programs")
  public ResponseEntity<Program> create(@RequestBody CreateReq req) {
    var p = new Program();
    p.setName(req.name());
    p.setDescription(req.description());
    p.setRulesJson(req.rulesJson());
    p.setState(ProgramState.DRAFT);
    return ResponseEntity.ok(programs.save(p));
  }

  @GetMapping("/programs")
  public List<Program> list() { return programs.findAll(); }

  @GetMapping("/programs/{id}")
  public ResponseEntity<Program> get(@PathVariable Long id) {
    return programs.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @PatchMapping("/programs/{id}")
  public ResponseEntity<Program> update(@PathVariable Long id, @RequestBody UpdateReq req) {
    var p = programs.findById(id).orElse(null);
    if (p == null) return ResponseEntity.notFound().build();
    if (req.name()!=null) p.setName(req.name());
    if (req.description()!=null) p.setDescription(req.description());
    return ResponseEntity.ok(programs.save(p));
  }

  @PatchMapping("/programs/{id}/rules")
  public ResponseEntity<Program> updateRules(@PathVariable Long id, @RequestBody UpdateRulesReq req) {
    var p = programs.findById(id).orElse(null);
    if (p == null) return ResponseEntity.notFound().build();
    p.setRulesJson(req.rulesJson());
    return ResponseEntity.ok(programs.save(p));
  }

  @PatchMapping("/programs/{id}/state")
  public ResponseEntity<?> changeState(@PathVariable Long id,
                                       @RequestParam(name="value", required=false) ProgramState valueFromQuery,
                                       @RequestBody(required=false) ChangeStateReq body) {

    var p = programs.findById(id).orElse(null);
    if (p == null) return ResponseEntity.notFound().build();

    var current = p.getState()==null ? ProgramState.DRAFT : p.getState();
    var next = valueFromQuery != null ? valueFromQuery : (body != null ? body.value() : null);
    if (next == null) return ResponseEntity.badRequest().body("Missing state value");

    if (!allowed(current, next)) return ResponseEntity.unprocessableEntity().body("Invalid program state transition");

    p.setState(next);
    return ResponseEntity.ok(programs.save(p));
  }

  private boolean allowed(ProgramState from, ProgramState to) {
    return switch (from) {
      case DRAFT -> EnumSet.of(ProgramState.ACTIVE, ProgramState.ARCHIVED).contains(to);
      case ACTIVE -> EnumSet.of(ProgramState.INACTIVE).contains(to);
      case INACTIVE -> EnumSet.of(ProgramState.ACTIVE, ProgramState.ARCHIVED).contains(to);
      case ARCHIVED -> false; // terminal
    };
  }
}
