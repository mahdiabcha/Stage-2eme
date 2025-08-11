package com.mini.g2p.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class JwtUtil {
  private final Key key;

  public JwtUtil(JwtProps props) {
    this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public Jws<Claims> parse(String token) {
    return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
  }

  public boolean isValid(String token) {
    try {
      parse(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public String getUsername(String token) {
    return parse(token).getBody().getSubject();
  }

  @SuppressWarnings("unchecked")
  public java.util.Set<String> getRoles(String token) {
    var claims = parse(token).getBody();
    Object r = claims.get("roles");
    if (r instanceof java.util.Collection<?> c) {
      java.util.Set<String> out = new java.util.HashSet<>();
      for (Object o : c) out.add(String.valueOf(o));
      return out;
    }
    return java.util.Collections.emptySet();
  }
}
