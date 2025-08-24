package com.mini.g2p.programcatalog.dto;

import com.mini.g2p.programcatalog.domain.ProgramState;

public class ProgramDtos {
  public record CreateReq(String name, String description, String rulesJson) {}
  public record UpdateReq(String name, String description) {}
  public record UpdateRulesReq(String rulesJson) {}
  public record ChangeStateReq(ProgramState value) {}
}
