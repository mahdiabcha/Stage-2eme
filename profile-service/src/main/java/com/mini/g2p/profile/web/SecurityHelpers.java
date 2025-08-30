package com.mini.g2p.profile.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class SecurityHelpers {
  private static final ObjectMapper M = new ObjectMapper();
  private SecurityHelpers(){}

  public static String currentUser(HttpHeaders headers) {
    String u = headers.getFirst("X-Auth-User");
    if (u != null && !u.isBlank()) return u;
    String auth = headers.getFirst(HttpHeaders.AUTHORIZATION);
    if (auth != null && auth.startsWith("Bearer ")) {
      String[] parts = auth.substring(7).split("\\.");
      if (parts.length >= 2) {
        try {
          String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
          JsonNode node = M.readTree(json);
          if (node.hasNonNull("sub")) return node.get("sub").asText();
        } catch (Exception ignored) {}
      }
    }
    return null;
  }

  public static boolean hasRole(HttpHeaders headers, String role) {
    String want = role.toUpperCase(Locale.ROOT).replace("ROLE_","");
    String rh = headers.getFirst("X-Auth-Roles");
    if (rh != null) {
      for (String r : rh.split("[,\\s]+")) if (want.equals(r.toUpperCase(Locale.ROOT).replace("ROLE_",""))) return true;
    }
    String auth = headers.getFirst(HttpHeaders.AUTHORIZATION);
    if (auth != null && auth.startsWith("Bearer ")) {
      String[] parts = auth.substring(7).split("\\.");
      if (parts.length >= 2) try {
        String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        JsonNode node = M.readTree(json);
        if (node.has("roles")) for (JsonNode r : node.get("roles"))
          if (want.equals(r.asText("").toUpperCase(Locale.ROOT).replace("ROLE_",""))) return true;
      } catch (Exception ignored) {}
    }
    return false;
  }
}
