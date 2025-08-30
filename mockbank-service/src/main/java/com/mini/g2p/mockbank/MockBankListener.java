package com.mini.g2p.mockbank;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class MockBankListener {
  private final AmqpTemplate amqp;
  public MockBankListener(AmqpTemplate amqp){ this.amqp = amqp; }

  // match payment-service DTO field names
  public record PaymentInstructionMsg(Long instructionId, Long programId, String username, Double amount, String currency) {}
  public record PaymentStatusMsg(Long instructionId, String status, String bankRef, String failReason) {}

  @RabbitListener(queues = RabbitConfig.Q_INSTR) // <- plural queue
  public void onInstruction(PaymentInstructionMsg msg) throws Exception {
    Thread.sleep(50 + (int)(Math.random() * 150));
    boolean ok = Math.random() < 0.85;

    // Correct field order: (instructionId, status, bankRef, failReason)
    var statusMsg = new PaymentStatusMsg(
        msg.instructionId(),
        ok ? "SUCCESS" : "FAILED",
        ok ? "BNK-" + UUID.randomUUID() : null,
        ok ? null : "INSUFFICIENT_FUNDS"
    );

    amqp.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_STATUS, statusMsg);
  }
}
