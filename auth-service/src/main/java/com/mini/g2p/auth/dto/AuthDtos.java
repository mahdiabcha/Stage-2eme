package com.mini.g2p.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class AuthDtos {
  public static final class RegisterRequest {
    @NotBlank public String username;
    @NotBlank public String password;
    @NotBlank @Pattern(regexp="^[0-9]{8}$") public String nationalId;
  }
  public static final class LoginRequest { @NotBlank public String username; @NotBlank public String password; }
  public static final class AuthResponse { public String token; public AuthResponse(String t){this.token=t;} }
  public static final class ErrorResponse { public String error; public ErrorResponse(String e){this.error=e;} }
  private AuthDtos() {}
}
