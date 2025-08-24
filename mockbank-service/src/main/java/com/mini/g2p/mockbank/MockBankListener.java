package com.mini.g2p.mockbank;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MockBankListener {

  private final AmqpTemplate amqp;

  // Local DTOs matching payment-service JSON payloads
  public record Instr(Long instructionId, Long programId, String beneficiary, Double amount, String currency) {}
  public record Status(Long instructionId, String status, String bankRef, String reason) {}

  @RabbitListener(queues = RabbitConfig.Q_INSTR)
  public void onInstruction(Instr msg) throws InterruptedException {
    System.out.println("[MOCKBANK] Received instruction: " + msg);

    // Simulate banking work
    Thread.sleep(300 + (int)(Math.random() * 1200));

    boolean ok = Math.random() > 0.1; // ~90% success
    Status status;
    if (ok) {
      status = new Status(msg.instructionId(), "SUCCESS", "BNK-" + System.currentTimeMillis(), null);
    } else {
      status = new Status(msg.instructionId(), "FAILED", null, "Insufficient funds");
    }

    amqp.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_STATUS, status);
    System.out.println("[MOCKBANK] Published status: " + status);
  }
}
