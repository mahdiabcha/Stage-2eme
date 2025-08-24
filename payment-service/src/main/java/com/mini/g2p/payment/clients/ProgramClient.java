package com.mini.g2p.payment.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import java.util.List;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
@Component
public class ProgramClient {

  private final WebClient web;

  public ProgramClient(@Value("${clients.programBaseUrl}") String baseUrl) {
    this.web = WebClient.builder().baseUrl(baseUrl).build();
  }

  public record ApprovedEntitlement(Long id, String username, Double amount, String currency) {}

  public List<ApprovedEntitlement> getApprovedEntitlements(Long cycleId) {
    return web.get()
      .uri(uri -> uri.path("/internal/entitlements/approved").queryParam("cycleId", cycleId).build())
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToFlux(ApprovedEntitlement.class)
      .collectList()
      .onErrorResume(err -> Mono.just(List.of()))
      .block();
  }
}
