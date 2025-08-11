package com.mini.g2p.enrollment.web;

import org.springframework.http.HttpHeaders;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class SecurityHelpers {
  private SecurityHelpers(){}
  public static String currentUser(HttpHeaders h){ return h.getFirst("X-Auth-User"); }
  public static Set<String> roles(HttpHeaders h){
    String raw = h.getFirst("X-Auth-Roles");
    if (raw==null) return Set.of();
    return Arrays.stream(raw.split(",")).map(String::trim).collect(Collectors.toSet());
  }
  public static boolean hasRole(HttpHeaders h, String role){ return roles(h).contains(role); }
}
