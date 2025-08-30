package com.mini.g2p.programcatalog.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

@Component
public class EnrollmentClient {
  private final WebClient web;
  public EnrollmentClient(WebClient.Builder b,
      @Value("${clients.enrollmentBaseUrl:${ENROLLMENT_BASEURL:http://enrollment-service:8084}}") String base) {
    this.web = b.baseUrl(base).build();
  }

  public record BeneficiaryItem(Long enrollmentId, Long programId, String username, String status, String note) {}

  public List<String> getApprovedUsernames(Long programId, HttpHeaders headers) {
    return web.get()
        .uri("/programs/{id}/beneficiaries?status=APPROVED", programId)
        .headers(h -> h.setAll(headers.toSingleValueMap()))
        .retrieve()
        .bodyToFlux(BeneficiaryItem.class)
        .map(BeneficiaryItem::username)
        .collectList()
        .block();
  }
}
