package com.mini.g2p.enrollment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Service
public class EligibilityService {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public static class Result {
    public final boolean eligible;
    public final String reason;
    public Result(boolean eligible, String reason){ this.eligible = eligible; this.reason = reason; }
  }

  public Result evaluate(Map<String,Object> profileCtx, String rulesJson){
    if (rulesJson == null || rulesJson.isBlank()) return new Result(true, "No rules");
    String s = rulesJson.trim();

    // Cheap fast-path for "TRUE"
    String upper = s.replaceAll("\\s+","").toUpperCase();
    if (upper.equals("TRUE") || upper.contains("\"TYPE\":\"TRUE\"") || upper.endsWith(":TRUE}") || upper.contains(":TRUE")) {
      return new Result(true, "TRUE");
    }

    // Try to parse JSON rules
    try {
      JsonNode node = objectMapper.readTree(s);
      List<String> reasons = new ArrayList<>();
      boolean ok = evalNode(node, profileCtx, reasons);
      return new Result(ok, ok ? "OK" : String.join("; ", reasons));
    } catch (Exception e) {
      // If not JSON, be conservative
      return new Result(false, "Invalid rules");
    }
  }

  private boolean evalNode(JsonNode node, Map<String, Object> ctx, List<String> reasons) {
    if (node == null || node.isNull()) return false;

    if (node.hasNonNull("type") && node.has("rules")) {
      String type = node.get("type").asText();
      JsonNode rules = node.get("rules");

      return switch (type) {
        case "AND" -> {
          boolean all = true;
          if (rules != null && rules.isArray()) {
            for (JsonNode c : rules) all &= evalNode(c, ctx, reasons);
          } else if (rules != null && !rules.isNull()) {
            all &= evalNode(rules, ctx, reasons);
          } else {
            all = false;
          }
          yield all;
        }
        case "OR" -> {
          boolean any = false;
          List<String> tmp = new ArrayList<>();
          if (rules != null && rules.isArray()) {
            for (JsonNode c : rules) {
              List<String> r = new ArrayList<>();
              boolean ok = evalNode(c, ctx, r);
              if (ok) any = true; else tmp.addAll(r);
            }
          } else if (rules != null && !rules.isNull()) {
            List<String> r = new ArrayList<>();
            boolean ok = evalNode(rules, ctx, r);
            if (ok) any = true; else tmp.addAll(r);
          }
          if (!any) reasons.addAll(tmp);
          yield any;
        }
        case "NOT" -> {
          JsonNode child = null;
          if (rules != null && rules.isArray() && rules.size() > 0) child = rules.get(0);
          else if (rules != null && !rules.isNull()) child = rules;

          if (child == null) yield true;
          List<String> r = new ArrayList<>();
          boolean ok = evalNode(child, ctx, r);
          if (ok) {
            reasons.add(node.has("message") ? node.get("message").asText() : "NOT rule failed");
            yield false;
          }
          yield true;
        }
        default -> false;
      };
    }

    String field = node.path("field").asText(null);
    String op = node.path("op").asText(null);
    String msg = node.path("message").asText("Rule failed");

    if (op == null) { reasons.add(msg); return false; }

    Object actual = (field == null) ? null : ctx.get(field);

    boolean passed = switch (op) {
      case "EXISTS" -> actual != null;
      case "TRUE"   -> actual instanceof Boolean b && b;
      case "FALSE"  -> actual instanceof Boolean b && !b;

      case "EQ"  -> cmp(actual, node.get("value")) == 0;
      case "NEQ" -> cmp(actual, node.get("value")) != 0;
      case "GT"  -> cmp(actual, node.get("value")) > 0;
      case "GTE" -> cmp(actual, node.get("value")) >= 0;
      case "LT"  -> cmp(actual, node.get("value")) < 0;
      case "LTE" -> cmp(actual, node.get("value")) <= 0;

      case "BETWEEN" -> between(actual, node.get("min"), node.get("max"));

      case "IN"     -> inList(actual, node.get("value"));
      case "NOT_IN" -> !inList(actual, node.get("value"));

      default -> false;
    };

    if (!passed) reasons.add(msg);
    return passed;
  }

  private boolean inList(Object actual, JsonNode arr) {
    if (actual == null || arr == null || !arr.isArray()) return false;
    for (JsonNode v : arr) if (cmp(actual, v) == 0) return true;
    return false;
  }

  private boolean between(Object actual, JsonNode min, JsonNode max) {
    if (actual == null || min == null || max == null) return false;
    return cmp(actual, min) >= 0 && cmp(actual, max) <= 0;
  }

  private int cmp(Object actual, JsonNode expected) {
    if (actual == null || expected == null || expected.isNull()) return -1;
    if (actual instanceof Number aN) {
      if (!expected.isNumber()) return -1;
      return Double.compare(aN.doubleValue(), expected.asDouble());
    }
    if (actual instanceof Boolean aB) {
      if (!expected.isBoolean()) return -1;
      return Boolean.compare(aB, expected.asBoolean());
    }
    return String.valueOf(actual).compareToIgnoreCase(expected.asText());
  }

public Map<String, Object> contextFromProfile(JsonNode prof) {
  Map<String, Object> m = new HashMap<>();
  if (prof == null) return m;

  putText(m, "username", prof, "username");
  putText(m, "gender", prof, "gender");
  putText(m, "governorate", prof, "governorate");
  putNum(m, "householdSize", prof, "householdSize");
  putNum(m, "incomeMonthly", prof, "incomeMonthly");
  putBool(m, "kycVerified", prof, "kycVerified");

  // Accept both keys: birthDate (old) and dateOfBirth (current)
  String bd = null;
  if (prof.hasNonNull("birthDate")) {
    bd = prof.get("birthDate").asText();
  } else if (prof.hasNonNull("dateOfBirth")) {
    bd = prof.get("dateOfBirth").asText();
  }

  if (bd != null && !bd.isBlank()) {
    try {
      LocalDate ld = LocalDate.parse(bd);
      int age = Period.between(ld, LocalDate.now()).getYears();
      m.put("age", age);
    } catch (Exception ignored) { }
  }
  return m;
}


  private void putText(Map<String,Object> m, String key, JsonNode n, String k) {
    if (n.has(k) && !n.get(k).isNull()) m.put(key, n.get(k).asText());
  }
  private void putNum(Map<String,Object> m, String key, JsonNode n, String k) {
    if (n.has(k) && n.get(k).isNumber()) m.put(key, n.get(k).asDouble());
  }
  private void putBool(Map<String,Object> m, String key, JsonNode n, String k) {
    if (n.has(k) && n.get(k).isBoolean()) m.put(key, n.get(k).asBoolean());
  }
}
