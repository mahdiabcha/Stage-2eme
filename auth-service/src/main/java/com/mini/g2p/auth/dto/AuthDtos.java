package com.mini.g2p.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
  public record RegisterRequest(@NotBlank String username, @NotBlank String password, String role) {}
  public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
  public record JwtResponse(String token) {}
  public record MeResponse(String username, java.util.Set<String> roles) {}
}
