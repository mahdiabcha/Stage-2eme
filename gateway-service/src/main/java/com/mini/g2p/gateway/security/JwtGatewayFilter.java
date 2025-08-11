package com.mini.g2p.gateway.security;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

  private final JwtProps props;
  private final JwtUtil jwt;
  private final AntPathMatcher matcher = new AntPathMatcher();

  public JwtGatewayFilter(JwtProps props, JwtUtil jwt) {
    this.props = props;
    this.jwt = jwt;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();

    // allow public paths
    for (String p : props.getPublicPaths()) {
      if (matcher.match(p, path)) {
        return chain.filter(exchange);
      }
    }

    // require Authorization: Bearer <token>
    String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (auth == null || !auth.startsWith("Bearer ")) {
      return unauthorized(exchange, "Missing or invalid Authorization header");
    }

    String token = auth.substring(7);
    if (!jwt.isValid(token)) {
      return unauthorized(exchange, "Invalid or expired token");
    }

    // enrich headers for downstream services
    String user = jwt.getUsername(token);
    String roles = String.join(",", jwt.getRoles(token));

    var mutated = exchange.getRequest().mutate()
        .header("X-Auth-User", user)
        .header("X-Auth-Roles", roles)
        .build();

    return chain.filter(exchange.mutate().request(mutated).build());
  }

  @Override
  public int getOrder() {
    // run early
    return -100;
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
    var res = exchange.getResponse();
    res.setStatusCode(HttpStatus.UNAUTHORIZED);
    res.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    var bytes = ("{\"error\":\"unauthorized\",\"message\":\"" + msg + "\"}")
        .getBytes(StandardCharsets.UTF_8);
    return res.writeWith(Mono.just(res.bufferFactory().wrap(bytes)));
  }
}
