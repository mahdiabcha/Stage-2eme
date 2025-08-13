package com.mini.g2p.payment.web;

import com.mini.g2p.payment.amqp.RabbitConfig;
import com.mini.g2p.payment.domain.PaymentBatch;
import com.mini.g2p.payment.domain.PaymentInstruction;
import com.mini.g2p.payment.dto.PaymentInstructionMsg;
import com.mini.g2p.payment.dto.PaymentStatusMsg;
import com.mini.g2p.payment.repo.PaymentBatchRepository;
import com.mini.g2p.payment.repo.PaymentInstructionRepository;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

  private final PaymentBatchRepository batches;
  private final PaymentInstructionRepository instr;
  private final AmqpTemplate amqp;

  public PaymentController(PaymentBatchRepository b, PaymentInstructionRepository i, AmqpTemplate a) {
    this.batches = b; this.instr = i; this.amqp = a;
  }

  public record CreateBatchReq(Long programId, Double amount, String currency, List<String> beneficiaries) {}

  @PostMapping("/batches")
  public ResponseEntity<?> create(@RequestHeader HttpHeaders headers, @RequestBody CreateBatchReq req) {
    if (req.programId() == null || req.amount() == null || req.amount() <= 0 || req.currency() == null)
      return ResponseEntity.unprocessableEntity().body(Map.of("error","validation_error","message","Invalid body"));
    if (req.beneficiaries() == null || req.beneficiaries().isEmpty())
      return ResponseEntity.unprocessableEntity().body(Map.of("error","validation_error","message","No beneficiaries"));

    var batch = new PaymentBatch();
    batch.setProgramId(req.programId());
    String createdBy = Optional.ofNullable(headers.getFirst("X-Auth-User")).orElse("admin");
    batch.setCreatedBy(createdBy);
    batches.save(batch);

    for (String u : req.beneficiaries()) {
      var pi = new PaymentInstruction();
      pi.setBatchId(batch.getId());
      pi.setBeneficiaryUsername(u);
      pi.setAmount(req.amount());
      pi.setCurrency(req.currency());
      instr.save(pi);
    }
    batch.setTotalCount(req.beneficiaries().size());
    batches.save(batch);

    return ResponseEntity.ok(Map.of("batchId", batch.getId(), "totalCount", batch.getTotalCount()));
  }

  @PostMapping("/batches/{id}/dispatch")
  public ResponseEntity<?> dispatch(@PathVariable Long id) {
    var b = batches.findById(id).orElse(null);
    if (b == null) return ResponseEntity.notFound().build();
    b.setStatus(PaymentBatch.Status.PROCESSING);
    batches.save(b);

    instr.findByBatchId(id).forEach(pi -> {
      var msg = new PaymentInstructionMsg(pi.getId(), b.getProgramId(), pi.getBeneficiaryUsername(), pi.getAmount(), pi.getCurrency());
      amqp.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_INSTR, msg);
      pi.setStatus(PaymentInstruction.Status.SENT);
      instr.save(pi);
    });

    return ResponseEntity.ok(Map.of("status","DISPATCHED"));
  }

  @GetMapping("/batches/{id}")
  public ResponseEntity<?> get(@PathVariable Long id) {
    var b = batches.findById(id).orElse(null);
    if (b == null) return ResponseEntity.notFound().build();
    var list = instr.findByBatchId(id);
    return ResponseEntity.ok(Map.of("batch", b, "instructions", list));
  }

  @GetMapping("/batches")
  public List<PaymentBatch> list() {
    return batches.findAll();
  }

  // ---- MQ consumer: status updates from MockBank ----
  @RabbitListener(queues = RabbitConfig.Q_STATUS)
  public void onStatus(PaymentStatusMsg st) {
    var pi = instr.findById(st.instructionId()).orElse(null);
    if (pi == null) return;

    if ("SUCCESS".equalsIgnoreCase(st.status())) {
      pi.setStatus(PaymentInstruction.Status.SUCCESS);
      pi.setBankRef(st.bankRef());
    } else {
      pi.setStatus(PaymentInstruction.Status.FAILED);
      pi.setFailReason(st.reason());
    }
    instr.save(pi);

    var b = batches.findById(pi.getBatchId()).orElse(null);
    if (b == null) return;

    var all = instr.findByBatchId(b.getId());
    int succ = (int) all.stream().filter(p -> p.getStatus() == PaymentInstruction.Status.SUCCESS).count();
    int fail = (int) all.stream().filter(p -> p.getStatus() == PaymentInstruction.Status.FAILED).count();
    b.setSuccessCount(succ);
    b.setFailedCount(fail);

    if (succ + fail == b.getTotalCount()) {
      b.setStatus(fail > 0 ? PaymentBatch.Status.FAILED : PaymentBatch.Status.COMPLETED);
    }
    batches.save(b);
  }
}
