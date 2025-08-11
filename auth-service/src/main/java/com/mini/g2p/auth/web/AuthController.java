package com.mini.g2p.auth.web;

import com.mini.g2p.auth.domain.User;
import com.mini.g2p.auth.dto.AuthDtos.*;
import com.mini.g2p.auth.repo.UserRepository;
import com.mini.g2p.auth.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final AuthenticationManager authManager;
  private final JwtUtil jwt;

  public AuthController(UserRepository users, PasswordEncoder encoder, AuthenticationManager authManager, JwtUtil jwt) {
    this.users = users; this.encoder = encoder; this.authManager = authManager; this.jwt = jwt;
  }

  @GetMapping("/hello")
  public String hello() { return "auth ok"; }

  @PostMapping("/register")
  public Map<String, Object> register(@RequestBody @Valid RegisterRequest req) {
    if (users.existsByUsername(req.username())) {
      return Map.of("error", "username_exists");
    }
    var u = new User();
    u.setUsername(req.username());
    u.setPassword(encoder.encode(req.password()));
    u.setRoles(Set.of(req.role() == null || req.role().isBlank() ? "CITIZEN" : req.role().toUpperCase()));
    users.save(u);
    return Map.of("status", "created", "username", u.getUsername(), "roles", u.getRoles());
  }

  @PostMapping("/login")
  public JwtResponse login(@RequestBody @Valid LoginRequest req) {
    Authentication auth = authManager.authenticate(
        new UsernamePasswordAuthenticationToken(req.username(), req.password()));
    var roles = auth.getAuthorities().stream().map(a -> a.getAuthority()).collect(java.util.stream.Collectors.toSet());
    String token = jwt.generate(req.username(), Map.of("roles", roles));
    return new JwtResponse(token);
  }

  @GetMapping("/me")
  public MeResponse me(Authentication auth) {
    var roles = auth.getAuthorities().stream().map(a -> a.getAuthority()).collect(java.util.stream.Collectors.toSet());
    return new MeResponse(auth.getName(), roles);
  }
}
