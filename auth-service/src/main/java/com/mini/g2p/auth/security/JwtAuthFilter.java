package com.mini.g2p.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtUtil jwt;
  private final UserDetailsService uds;

  public JwtAuthFilter(JwtUtil jwt, UserDetailsService uds) {
    this.jwt = jwt; this.uds = uds;
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res,
                                  @NonNull FilterChain chain) throws IOException, jakarta.servlet.ServletException {
    String h = req.getHeader(HttpHeaders.AUTHORIZATION);
    if (h == null || !h.startsWith("Bearer ")) { chain.doFilter(req, res); return; }
    String token = h.substring(7).trim();
    if (!jwt.isValid(token)) { chain.doFilter(req, res); return; }

    String user = jwt.getUsername(token);
    var details = uds.loadUserByUsername(user);

    List<SimpleGrantedAuthority> auths = jwt.getRoles(token).stream()
        .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());

    var auth = new UsernamePasswordAuthenticationToken(details, null, auths);
    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
    chain.doFilter(req, res);
  }
}
