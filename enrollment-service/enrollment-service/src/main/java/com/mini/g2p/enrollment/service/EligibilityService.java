package com.mini.g2p.enrollment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Service
public class EligibilityService {
  private final ObjectMapper om = new ObjectMapper();

  public record Result(boolean eligible, List<String> reasons) {}

  public Result evaluate(String rulesJson, Map<String, Object> profileCtx) {
    if (rulesJson == null || rulesJson.isBlank()) return new Result(true, List.of());
    try {
      JsonNode root = om.readTree(rulesJson);
      List<String> reasons = new ArrayList<>();
      boolean pass = evalNode(root, profileCtx, reasons);
      return new Result(pass, reasons);
    } catch (Exception e) {
      return new Result(false, List.of("Invalid eligibility configuration"));
    }
  }

  private boolean evalNode(JsonNode node, Map<String, Object> ctx, List<String> reasons) {
    if (node == null || node.isNull()) return false;

    // Composite node: { "type": "AND|OR|NOT", "rules": [...] , "message": "..." }
    if (node.hasNonNull("type") && node.has("rules")) {
      String type = node.get("type").asText();
      JsonNode rules = node.get("rules"); // may be array OR single object

      return switch (type) {
        case "AND" -> {
          boolean all = true;
          if (rules != null && rules.isArray()) {
            for (JsonNode c : rules) all &= evalNode(c, ctx, reasons);
          } else if (rules != null && !rules.isNull()) {
            all &= evalNode(rules, ctx, reasons);
          } else {
            all = false; // no rules -> fail
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
          // For NOT we evaluate the first (or only) child, then invert
          JsonNode child = null;
          if (rules != null && rules.isArray() && rules.size() > 0) child = rules.get(0);
          else if (rules != null && !rules.isNull()) child = rules;

          if (child == null) yield true; // NOT of nothing -> true
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

    // Leaf rule: { field, op, value?, message? }
    String field = node.path("field").asText(null);
    String op = node.path("op").asText(null);
    String msg = node.path("message").asText("Rule failed");

    if (op == null) { // malformed rule
      reasons.add(msg);
      return false;
    }

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

  /**
   * Compare Object actual against expected JsonNode.
   * - Numbers: numeric compare
   * - Booleans: true > false, equals if same
   * - Others: case-insensitive string compare
   * Returns: negative if actual < expected, 0 if equal, positive if actual > expected, -1 on incompatible.
   */
  private int cmp(Object actual, JsonNode expected) {
    if (actual == null || expected == null || expected.isNull()) return -1;

    // Number compare
    if (actual instanceof Number aN) {
      if (!expected.isNumber()) return -1;
      double a = aN.doubleValue();
      double b = expected.asDouble();
      return Double.compare(a, b);
    }

    // Boolean compare
    if (actual instanceof Boolean aB) {
      if (!expected.isBoolean()) return -1;
      boolean b = expected.asBoolean();
      return Boolean.compare(aB, b);
    }

    // Fallback: string compare (case-insensitive)
    return String.valueOf(actual).compareToIgnoreCase(expected.asText());
  }

  // helper for building context from profile JSON
  public Map<String, Object> contextFromProfile(JsonNode prof) {
    Map<String, Object> m = new HashMap<>();
    if (prof == null) return m;

    m.put("username", text(prof, "username"));
    m.put("gender", text(prof, "gender"));
    m.put("governorate", text(prof, "governorate"));
    m.put("householdSize", num(prof, "householdSize"));
    m.put("incomeMonthly", num(prof, "incomeMonthly"));
    m.put("kycVerified", bool(prof, "kycVerified"));

    // compute age from birthDate (ISO-8601: yyyy-MM-dd)
    String bd = text(prof, "birthDate");
    if (bd != null) {
      try {
        var ld = LocalDate.parse(bd);
        int age = Period.between(ld, LocalDate.now()).getYears();
        m.put("age", age);
      } catch (Exception ignored) {
        // ignore parse errors: age will be absent
      }
    }
    return m;
  }

  private String text(JsonNode n, String k) {
    return n.has(k) && !n.get(k).isNull() ? n.get(k).asText() : null;
    }
  private Double num(JsonNode n, String k) {
    return n.has(k) && n.get(k).isNumber() ? n.get(k).asDouble() : null;
  }
  private Boolean bool(JsonNode n, String k) {
    return n.has(k) && n.get(k).isBoolean() ? n.get(k).asBoolean() : null;
  }
}
