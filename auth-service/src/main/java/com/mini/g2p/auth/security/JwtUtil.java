package com.mini.g2p.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;

@Component
public class JwtUtil {

  @Value("${app.jwt.secret}")
  private String secret;

  @Value("${app.jwt.expiration:3600000}")
  private long expirationMs;

  private Key key() { return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); }

  public String generateToken(String username, Map<String,Object> claims) {
    Date now = new Date();
    Date exp = new Date(now.getTime() + expirationMs);
    return Jwts.builder()
        .setSubject(username)
        .addClaims(claims)
        .setIssuedAt(now)
        .setExpiration(exp)
        .signWith(key(), SignatureAlgorithm.HS256)
        .compact();
  }

  public boolean isValid(String token) {
    try { parse(token); return true; }
    catch (JwtException | IllegalArgumentException e) { return false; }
  }

  private Jws<Claims> parse(String token) { return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token); }

  public String getUsername(String token) { return parse(token).getBody().getSubject(); }

  public List<String> getRoles(String token) {
    Object r = parse(token).getBody().get("roles");
    if (r instanceof Collection<?> c) return c.stream().map(String::valueOf).toList();
    return List.of();
  }
}
