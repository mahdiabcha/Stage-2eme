package com.mini.g2p.enrollment.dto;

import java.time.Instant;

public class BeneficiaryDtos {

  public record BeneficiaryItem(
      Long enrollmentId,
      Long programId,
      String username,
      String status,
      String note,
      Instant createdAt
  ) {}
}
