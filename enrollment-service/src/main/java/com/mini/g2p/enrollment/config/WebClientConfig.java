package com.mini.g2p.enrollment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(WebClientConfig.ClientsProps.class)
public class WebClientConfig {

  @Bean
  WebClient profileClient(WebClient.Builder builder, ClientsProps props) {
    return builder
        .baseUrl(props.getProfileBaseUrl())
        // no default X-Auth headers here; we forward real ones per-request
        .exchangeStrategies(ExchangeStrategies.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
            .build())
        .build();
  }

  @Bean
  WebClient programCatalogClient(WebClient.Builder builder, ClientsProps props) {
    return builder
        .baseUrl(props.getProgramCatalogBaseUrl())
        .exchangeStrategies(ExchangeStrategies.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
            .build())
        .build();
  }

  @ConfigurationProperties(prefix = "clients")
  public static class ClientsProps {
    private String programCatalogBaseUrl;
    private String profileBaseUrl;

    public String getProgramCatalogBaseUrl() { return programCatalogBaseUrl; }
    public void setProgramCatalogBaseUrl(String v) { this.programCatalogBaseUrl = v; }

    public String getProfileBaseUrl() { return profileBaseUrl; }
    public void setProfileBaseUrl(String v) { this.profileBaseUrl = v; }
  }
}
