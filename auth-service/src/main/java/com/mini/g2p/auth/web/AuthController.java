package com.mini.g2p.auth.web;

import com.mini.g2p.auth.dto.AuthDtos.*;
import com.mini.g2p.auth.domain.User;
import com.mini.g2p.auth.repo.UserRepository;
import com.mini.g2p.auth.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final JwtUtil jwt;
  private final AuthenticationManager authManager;

  public AuthController(UserRepository users, PasswordEncoder encoder, JwtUtil jwt, AuthenticationManager authManager) {
    this.users = users; this.encoder = encoder; this.jwt = jwt; this.authManager = authManager;
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest in) {
    if (users.existsByUsername(in.username)) return ResponseEntity.status(409).body(new ErrorResponse("username exists"));
    if (users.existsByNationalId(in.nationalId)) return ResponseEntity.status(409).body(new ErrorResponse("nationalId exists"));

    User u = new User();
    u.setUsername(in.username); u.setNationalId(in.nationalId);
    u.setPasswordHash(encoder.encode(in.password));
    u.setRoles(Set.of("CITIZEN"));
    u.setCreatedAt(Instant.now());
    users.save(u);
    return ResponseEntity.status(201).body(Map.of("message","registered"));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest in) {
    try { authManager.authenticate(new UsernamePasswordAuthenticationToken(in.username, in.password)); }
    catch (BadCredentialsException ex) { return ResponseEntity.status(401).body(new ErrorResponse("Bad credentials")); }
    var u = users.findByUsername(in.username).orElseThrow();
    String token = jwt.generateToken(u.getUsername(), Map.of("roles", u.getRoles()));
    return ResponseEntity.ok(new AuthResponse(token));
  }

  @GetMapping("/me")
  public ResponseEntity<?> me(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated())
      return ResponseEntity.status(401).body(new ErrorResponse("Unauthorized"));
    var u = users.findByUsername(authentication.getName()).orElseThrow();
    return ResponseEntity.ok(Map.of("username", u.getUsername(), "roles", u.getRoles()));
  }
}
