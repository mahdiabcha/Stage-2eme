package com.mini.g2p.payment.web;

import com.mini.g2p.payment.clients.ProgramClient;
import com.mini.g2p.payment.domain.*;
import com.mini.g2p.payment.repo.*;
import com.mini.g2p.payment.amqp.RabbitConfig;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

  private final PaymentBatchRepository batches;
  private final PaymentInstructionRepository instr;
  private final AmqpTemplate amqp;
  private final ProgramClient programClient;

  public PaymentController(PaymentBatchRepository b, PaymentInstructionRepository i, AmqpTemplate a, ProgramClient pc) {
    this.batches = b; this.instr = i; this.amqp = a; this.programClient = pc;
  }

  // ===== Existing endpoints kept (manual beneficiaries) =====
  public record CreateBatchReq(Long programId, Double amount, String currency, List<String> beneficiaries) {}

  @PostMapping("/batches")
  public Map<String, Object> createManual(@RequestBody CreateBatchReq req) {
    var batch = new PaymentBatch();
    batch.setProgramId(req.programId());
    batches.save(batch);
    for (String u : req.beneficiaries()) {
      var pi = new PaymentInstruction();
      pi.setBatchId(batch.getId()); pi.setBeneficiaryUsername(u);
      pi.setAmount(req.amount()); pi.setCurrency(req.currency());
      instr.save(pi);
    }
    batch.setTotalCount(req.beneficiaries().size());
    batches.save(batch);
    return Map.of("totalCount", batch.getTotalCount(), "batchId", batch.getId());
  }

  @PostMapping("/batches/{id}/dispatch")
  public ResponseEntity<?> dispatch(@PathVariable Long id) {
    var b = batches.findById(id).orElseThrow();
    b.setStatus(PaymentBatch.Status.PROCESSING); batches.save(b);
    instr.findByBatchId(id).forEach(pi -> {
      var msg = new com.mini.g2p.payment.dto.PaymentInstructionMsg(
        pi.getId(), b.getProgramId(), pi.getBeneficiaryUsername(), pi.getAmount(), pi.getCurrency()
      );
      amqp.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_INSTR, msg);
      pi.setStatus(PaymentInstruction.Status.SENT); instr.save(pi);
    });
    return ResponseEntity.ok(Map.of("status","DISPATCHED"));
  }

  @GetMapping("/batches/{id}")
  public Map<String,Object> get(@PathVariable Long id) {
    var b = batches.findById(id).orElseThrow();
    var list = instr.findByBatchId(id);
    return Map.of("batch", b, "instructions", list);
  }

  @GetMapping("/batches")
  public List<PaymentBatch> list() { return batches.findAll(); }

  // ===== NEW: Create batch from APPROVED entitlements of a cycle =====
  @PostMapping("/batches/from-cycle")
  public Map<String, Object> createFromCycle(@RequestParam Long cycleId, @RequestParam Long programId) {
    var approved = programClient.getApprovedEntitlements(cycleId);
    var batch = new PaymentBatch();
    batch.setProgramId(programId);
    batches.save(batch);

    approved.forEach(e -> {
      var pi = new PaymentInstruction();
      pi.setBatchId(batch.getId());
      pi.setBeneficiaryUsername(e.username());
      pi.setAmount(e.amount());
      pi.setCurrency(e.currency());
      instr.save(pi);
    });

    batch.setTotalCount(approved.size());
    batches.save(batch);
    return Map.of("totalCount", batch.getTotalCount(), "batchId", batch.getId());
  }
}
