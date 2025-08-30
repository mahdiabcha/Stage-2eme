package com.mini.g2p.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProps {
  private String secret;
  private List<String> publicPaths = new ArrayList<>();
  public String getSecret(){return secret;}
  public void setSecret(String secret){this.secret=secret;}
  public List<String> getPublicPaths(){return publicPaths;}
  public void setPublicPaths(List<String> p){this.publicPaths=p;}
}
