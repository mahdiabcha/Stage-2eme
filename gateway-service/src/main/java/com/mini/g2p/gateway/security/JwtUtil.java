package com.mini.g2p.gateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;
import java.util.Objects;

@Component
public class JwtUtil {
  @Value("${app.jwt.secret}") private String secret;
  private Key key(){ return Keys.hmacShaKeyFor(Objects.requireNonNull(secret).getBytes(StandardCharsets.UTF_8)); }
  private Jws<Claims> parse(String token){ return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token); }
  public boolean isValid(String token){
    try { parse(token); return true; } catch (JwtException | IllegalArgumentException e){ return false; }
  }
  public String getUsername(String token){ return parse(token).getBody().getSubject(); }
  public List<String> getRoles(String token){
    Object r = parse(token).getBody().get("roles");
    if (r instanceof List<?> l) return l.stream().map(String::valueOf).toList();
    return List.of();
  }
}
