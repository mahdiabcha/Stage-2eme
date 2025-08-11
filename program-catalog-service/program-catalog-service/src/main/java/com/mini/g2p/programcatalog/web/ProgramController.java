package com.mini.g2p.programcatalog.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.g2p.programcatalog.domain.Program;
import com.mini.g2p.programcatalog.repo.ProgramRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
public class ProgramController {
  private final ProgramRepository repo;
  private final ObjectMapper om = new ObjectMapper();

  public ProgramController(ProgramRepository repo){this.repo=repo;}

  @GetMapping("/programs")
  public List<Program> list(){ return repo.findAll(); }

  @GetMapping("/programs/{id}")
  public Program get(@PathVariable Long id){ return repo.findById(id).orElseThrow(); }

  public record CreateDto(@NotBlank String name, String description, LocalDate startDate, LocalDate endDate, Boolean active, String rulesJson){}
  @PostMapping("/programs")
  public ResponseEntity<?> create(@RequestBody CreateDto d) {
    Program p=new Program();
    p.setName(d.name()); p.setDescription(d.description());
    p.setStartDate(d.startDate()); p.setEndDate(d.endDate());
    p.setActive(d.active()!=null? d.active(): true);
    if (d.rulesJson()!=null && !d.rulesJson().isBlank()) { try { om.readTree(d.rulesJson()); } catch(Exception e){ return ResponseEntity.badRequest().body("Invalid rulesJson"); } }
    p.setRulesJson(d.rulesJson());
    return ResponseEntity.ok(repo.save(p));
  }

  public record RulesDto(@NotBlank String rulesJson){}
  @PutMapping("/programs/{id}/rules")
  public ResponseEntity<?> updateRules(@PathVariable Long id, @RequestBody RulesDto d) {
    try { om.readTree(d.rulesJson()); } catch (Exception e){ return ResponseEntity.badRequest().body("Invalid rulesJson"); }
    var p = repo.findById(id).orElseThrow();
    p.setRulesJson(d.rulesJson());
    repo.save(p);
    return ResponseEntity.ok().build();
  }
}
