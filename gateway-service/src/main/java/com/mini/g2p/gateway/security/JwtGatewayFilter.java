package com.mini.g2p.gateway.security;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@org.springframework.boot.context.properties.EnableConfigurationProperties(JwtProps.class)
public class JwtGatewayFilter implements GlobalFilter, Ordered {

  private final JwtUtil jwt;
  private final JwtProps props;
  private final AntPathMatcher matcher = new AntPathMatcher();

  public JwtGatewayFilter(JwtUtil jwt, JwtProps props){ this.jwt=jwt; this.props=props; }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();

    // Public paths
    for (String p : props.getPublicPaths()) if (matcher.match(p, path)) return chain.filter(exchange);

    // Require Authorization
    String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (auth == null || !auth.startsWith("Bearer ")) return unauthorized(exchange, "Missing or invalid Authorization header");
    String token = auth.substring(7).trim();
    if (!jwt.isValid(token)) return unauthorized(exchange, "Invalid or expired token");

    String user = jwt.getUsername(token);
    Set<String> roles = jwt.getRoles(token).stream().map(r -> r.replace("ROLE_","")).collect(Collectors.toSet());

    var req = exchange.getRequest().mutate()
        .header("X-Auth-User", user)
        .header("X-Auth-Roles", String.join(",", roles))
        .build();

    return chain.filter(exchange.mutate().request(req).build());
  }

  @Override public int getOrder(){ return -100; }

  private Mono<Void> unauthorized(ServerWebExchange exchange, String msg){
    var res = exchange.getResponse();
    res.setStatusCode(HttpStatus.UNAUTHORIZED);
    res.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    var bytes = ("{\"error\":\"unauthorized\",\"message\":\""+msg+"\"}").getBytes(StandardCharsets.UTF_8);
    return res.writeWith(Mono.just(res.bufferFactory().wrap(bytes)));
  }
}
