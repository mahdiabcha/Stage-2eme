package com.mini.g2p.mockbank;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class MockBankListener {

  private final AmqpTemplate amqp;
  private final Random rnd = new Random();

  public MockBankListener(AmqpTemplate amqp) { this.amqp = amqp; }

  public record Instr(Long instructionId, Long programId, String beneficiary, Double amount, String currency) {}
  public record Status(Long instructionId, String status, String bankRef, String reason) {}

  @RabbitListener(queues = RabbitConfig.Q_INSTR)
  public void onInstr(Instr msg) throws InterruptedException {
    // Simulate processing time
    Thread.sleep(500 + rnd.nextInt(1500));

    boolean ok = rnd.nextDouble() > 0.1; // ~90% success
    if (ok) {
      amqp.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_STATUS,
          new Status(msg.instructionId(), "SUCCESS", "BNK-" + System.currentTimeMillis(), null));
    } else {
      amqp.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_STATUS,
          new Status(msg.instructionId(), "FAILED", null, "Insufficient funds"));
    }
  }
}
