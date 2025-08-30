package com.mini.g2p.enrollment.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  @Bean @Qualifier("profileClient")
  WebClient profileClient(WebClient.Builder b,
     @Value("${clients.profileBaseUrl:${PROFILE_BASEURL:http://profile-service:8083}}") String base) {
    return b.baseUrl(base).build();
  }

  @Bean @Qualifier("programClient")
  WebClient programClient(WebClient.Builder b,
     @Value("${clients.programCatalogBaseUrl:${PROGRAM_CATALOG_BASEURL:http://program-catalog-service:8085}}") String base) {
    return b.baseUrl(base).build();
  }
}
