package com.mini.g2p.programcatalog.dto;

import java.time.LocalDate;
import java.util.List;

public class EntitlementDtos {
  public record Item(String username, Double amount, String currency, LocalDate validFrom, LocalDate validUntil) {}
  public record GenerateReq(List<Item> items) {}
  // Light view for internal calls (PaymentService)
  public record ApprovedEntitlement(Long id, String username, Double amount, String currency) {}
}
