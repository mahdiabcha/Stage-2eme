package com.mini.g2p.programcatalog.dto;

import com.mini.g2p.programcatalog.domain.CycleState;
import java.time.LocalDate;

public class CycleDtos {
  public record CreateCycleReq(String name, LocalDate startDate, LocalDate endDate) {}
  public record ChangeStateReq(CycleState value) {}
}
