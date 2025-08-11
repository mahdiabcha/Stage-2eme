package com.mini.g2p.profile.web;

import org.springframework.http.HttpHeaders;
public final class SecurityHelpers {
  private SecurityHelpers(){}
  public static String currentUser(HttpHeaders headers){ return headers.getFirst("X-Auth-User"); }
}
