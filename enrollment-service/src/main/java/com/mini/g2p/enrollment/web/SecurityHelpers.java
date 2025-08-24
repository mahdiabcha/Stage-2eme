package com.mini.g2p.enrollment.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class SecurityHelpers {
  private static final ObjectMapper M = new ObjectMapper();

  private SecurityHelpers(){}

  public static String currentUser(HttpHeaders headers) {
    // Prefer explicit header (works when hitting service directly)
    String x = headers.getFirst("X-Auth-User");
    if (x != null && !x.isBlank()) return x;

    // Otherwise try JWT: Authorization: Bearer <jwt>
    String auth = headers.getFirst(HttpHeaders.AUTHORIZATION);
    if (auth != null && auth.startsWith("Bearer ")) {
      String jwt = auth.substring("Bearer ".length());
      try {
        String[] parts = jwt.split("\\.");
        if (parts.length >= 2) {
          String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
          JsonNode node = M.readTree(payload);
          if (node.hasNonNull("sub")) return node.get("sub").asText();
        }
      } catch (Exception ignored) { }
    }
    return "anonymous";
  }

  public static Set<String> currentRoles(HttpHeaders headers) {
    String r = headers.getFirst("X-Auth-Roles");
    if (r != null && !r.isBlank()) {
      String[] parts = r.split("[,\\s]+");
      Set<String> set = new HashSet<>();
      for (String p : parts) if (!p.isBlank()) set.add(p.trim());
      return set;
    }
    String auth = headers.getFirst(HttpHeaders.AUTHORIZATION);
    if (auth != null && auth.startsWith("Bearer ")) {
      String jwt = auth.substring("Bearer ".length());
      try {
        String[] parts = jwt.split("\\.");
        if (parts.length >= 2) {
          String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
          JsonNode node = M.readTree(payload);
          if (node.has("roles") && node.get("roles").isArray()) {
            Set<String> set = new HashSet<>();
            node.get("roles").forEach(n -> set.add(n.asText()));
            return set;
          }
        }
      } catch (Exception ignored) { }
    }
    return Set.of();
  }

  public static boolean hasRole(HttpHeaders headers, String role) {
    return currentRoles(headers).contains(role);
  }
}
