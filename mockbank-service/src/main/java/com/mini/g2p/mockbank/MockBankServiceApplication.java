package com.mini.g2p.mockbank;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRabbit
public class MockBankServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(MockBankServiceApplication.class, args);
  }
}
