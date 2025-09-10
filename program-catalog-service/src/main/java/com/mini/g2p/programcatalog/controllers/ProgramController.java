package com.mini.g2p.programcatalog.controllers;

import com.mini.g2p.programcatalog.domain.Program;
import com.mini.g2p.programcatalog.domain.ProgramState;
import com.mini.g2p.programcatalog.repo.ProgramRepository;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class ProgramController {
  private final ProgramRepository programs;
  public ProgramController(ProgramRepository programs){ this.programs=programs; }

  @PostMapping("/programs")
  public ResponseEntity<?> create(@RequestBody Program p){ return ResponseEntity.ok(programs.save(p)); }

  @GetMapping("/programs")
  public java.util.List<Program> list() {
    return programs.findAll();
  }

  @GetMapping("/programs/{id}")
  public ResponseEntity<?> get(@PathVariable Long id){
    return programs.findById(id).<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  public record ChangeStateReq(ProgramState value){}
  @PatchMapping("/programs/{id}/state")
  public ResponseEntity<?> changeState(@PathVariable Long id, @RequestBody ChangeStateReq req){
    var p = programs.findById(id).orElse(null);
    if (p==null) return ResponseEntity.notFound().build();
    var from = p.getState(); var to = req.value();
    if (!allowed(from,to)) return ResponseEntity.status(409).body(Map.of("error","transition not allowed"));
    if (from==ProgramState.DRAFT && to==ProgramState.ACTIVE) {
      if (p.getRulesJson()==null || p.getRulesJson().isBlank()) return ResponseEntity.status(409).body(Map.of("error","rulesJson required to activate"));
    }
    p.setState(to); return ResponseEntity.ok(programs.save(p));
  }

  private boolean allowed(ProgramState from, ProgramState to){
    return switch (from){
      case DRAFT -> to==ProgramState.ACTIVE || to==ProgramState.ARCHIVED;
      case ACTIVE -> to==ProgramState.INACTIVE || to==ProgramState.ARCHIVED;
      case INACTIVE -> to==ProgramState.ACTIVE || to==ProgramState.ARCHIVED;
      case ARCHIVED -> false;
    };
  }
}
