package com.mini.g2p.enrollment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
  @Bean
  public WebClient programCatalogClient(@Value("${clients.programCatalogBaseUrl}") String base) {
    return WebClient.builder().baseUrl(base).build();
  }
  @Bean
  public WebClient profileClient(@Value("${clients.profileBaseUrl}") String base) {
    return WebClient.builder().baseUrl(base).build();
  }
}
