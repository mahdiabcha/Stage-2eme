
package com.mini.g2p.profile.web;

import com.mini.g2p.profile.domain.CitizenProfile;
import com.mini.g2p.profile.domain.CitizenProfile.PaymentMethod;
import com.mini.g2p.profile.domain.ProfileDocument;
import com.mini.g2p.profile.dto.ProfileRequest;
import com.mini.g2p.profile.repo.CitizenProfileRepository;
import com.mini.g2p.profile.repo.ProfileDocumentRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class ProfileController {

  private final CitizenProfileRepository profiles;
  private final ProfileDocumentRepository docs;

  public ProfileController(CitizenProfileRepository profiles,
                           ProfileDocumentRepository docs) {
    this.profiles = profiles;
    this.docs = docs;
  }

  /* ---------------------------
     Helpers
  ---------------------------- */

  private static String sanitize(String s) {
    if (s == null) return "document";
    // keep a simple, safe filename
    return s.replaceAll("[\\r\\n\\\\/\\t\\0]", "_").trim();
  }

  private static String contentDispositionAttachment(String filename) {
    String safe = sanitize(filename).replace("\"", "");
    String encoded = URLEncoder.encode(safe, StandardCharsets.UTF_8).replace("+", "%20");
    // RFC 5987: send both for broad compatibility
    return "attachment; filename=\"" + safe + "\"; filename*=UTF-8''" + encoded;
  }

  private static String requireUser(HttpHeaders headers) {
    String user = SecurityHelpers.currentUser(headers);
    if (user == null || user.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no user");
    }
    return user;
  }

  /* ---------------------------
     Profile: read & update
  ---------------------------- */

  @GetMapping("/profiles/me")
  public Map<String, Object> me(@RequestHeader HttpHeaders headers) {
    String user = requireUser(headers);

    var p = profiles.findByUsername(user).orElseGet(() -> {
      var np = new CitizenProfile();
      np.setUsername(user);
      return profiles.save(np);
    });

    Map<String, Object> m = new LinkedHashMap<>();
    m.put("username", p.getUsername());
    m.put("firstName", p.getFirstName());
    m.put("lastName", p.getLastName());
    m.put("gender", p.getGender());
    m.put("dateOfBirth", p.getDateOfBirth());
    m.put("governorate", p.getGovernorate());
    m.put("district", p.getDistrict());
    m.put("householdSize", p.getHouseholdSize());
    m.put("incomeMonthly", p.getIncomeMonthly());
    m.put("kycVerified", p.getKycVerified());
    m.put("paymentMethod", p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "NONE");
    m.put("bankName", p.getBankName());
    m.put("iban", p.getIban());
    m.put("accountHolder", p.getAccountHolder());
    m.put("walletProvider", p.getWalletProvider());
    m.put("walletNumber", p.getWalletNumber());
    return m;
  }

  @PostMapping("/profiles/me")
  public ResponseEntity<?> update(@RequestHeader HttpHeaders headers,
                                  @RequestBody ProfileRequest req) {
    String user = requireUser(headers);

    var p = profiles.findByUsername(user).orElseGet(() -> {
      var np = new CitizenProfile();
      np.setUsername(user);
      return profiles.save(np);
    });

    p.setFirstName(req.firstName);
    p.setLastName(req.lastName);
    if (req.gender != null) p.setGender(req.gender);
    if (req.dateOfBirth != null && !req.dateOfBirth.isBlank()) {
      p.setDateOfBirth(LocalDate.parse(req.dateOfBirth));
    }
    p.setGovernorate(req.governorate);
    p.setDistrict(req.district);
    p.setHouseholdSize(req.householdSize);
    p.setIncomeMonthly(req.incomeMonthly);
    p.setKycVerified(req.kycVerified);

    if (req.paymentMethod != null) {
      try {
        p.setPaymentMethod(PaymentMethod.valueOf(req.paymentMethod.toUpperCase(Locale.ROOT)));
      } catch (IllegalArgumentException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid paymentMethod");
      }
    }
    p.setBankName(req.bankName);
    p.setIban(req.iban);
    p.setAccountHolder(req.accountHolder);
    p.setWalletProvider(req.walletProvider);
    p.setWalletNumber(req.walletNumber);

    profiles.save(p);
    return ResponseEntity.ok(Map.of("ok", true));
  }

  /* ---------------------------
     Documents: upload
     (matches your working call)
     POST /profiles/me/documents?type=ID
     -F "file=@path"
  ---------------------------- */

  @PostMapping(path = "/profiles/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, Object> upload(@RequestHeader HttpHeaders headers,
                                    @RequestParam(value = "type", required = false) String type,
                                    @RequestParam("file") MultipartFile file) {
    String user = requireUser(headers);

    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "empty file");
    }

    try {
      var d = new ProfileDocument();
      d.setOwnerUsername(user);
      d.setType(type != null ? type : "OTHER");
      d.setFilename(sanitize(Objects.requireNonNullElse(file.getOriginalFilename(), "document")));
      d.setContentType(Objects.requireNonNullElse(file.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE));
      d.setSize(file.getSize());
      d.setData(file.getBytes());

      docs.save(d);

      Map<String, Object> resp = new LinkedHashMap<>();
      resp.put("size", d.getSize());
      resp.put("type", d.getContentType());
      resp.put("id", d.getId());
      return resp;
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to store file");
    }
  }

  /* ---------------------------
     Documents: list (fixes 405)
     GET /profiles/me/documents
     Returns lightweight metadata.
  ---------------------------- */

  public record DocMeta(Long id, String type, String filename,
                        String contentType, Long size) {}

  @GetMapping("/profiles/me/documents")
  public List<DocMeta> myDocuments(@RequestHeader HttpHeaders headers) {
    String user = requireUser(headers);

    // Avoid requiring custom repo methods: filter in memory
    return docs.findAll().stream()
        .filter(d -> user.equals(d.getOwnerUsername()))
        .sorted(Comparator.comparing(ProfileDocument::getId).reversed())
        .map(d -> new DocMeta(
            d.getId(),
            d.getType(),
            d.getFilename(),
            d.getContentType(),
            d.getSize()
        ))
        .collect(Collectors.toList());
  }

  /* ---------------------------
     Documents: download (binary)
     GET /profiles/me/documents/{id}/download
  ---------------------------- */

  @Transactional(readOnly = true)
  @GetMapping("/profiles/me/documents/{id}/download")
  public ResponseEntity<byte[]> download(@RequestHeader HttpHeaders headers,
                                         @PathVariable Long id) {
    String user = requireUser(headers);

    var doc = docs.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "document not found"));

    if (!user.equals(doc.getOwnerUsername())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "document not found");
    }

    MediaType ct;
    try {
      ct = doc.getContentType() != null ? MediaType.parseMediaType(doc.getContentType())
                                        : MediaType.APPLICATION_OCTET_STREAM;
    } catch (Exception e) {
      ct = MediaType.APPLICATION_OCTET_STREAM;
    }

    byte[] body = doc.getData();
    if (body == null) body = new byte[0];

    HttpHeaders h = new HttpHeaders();
    h.setContentType(ct);
    if (doc.getSize() != null) h.setContentLength(doc.getSize());
    h.set(HttpHeaders.CONTENT_DISPOSITION, contentDispositionAttachment(
        Optional.ofNullable(doc.getFilename()).orElse("document")
    ));

    return new ResponseEntity<>(body, h, HttpStatus.OK);
  }

  /* ---------------------------
     Documents: delete (optional)
  ---------------------------- */

  @DeleteMapping("/profiles/me/documents/{id}")
  public ResponseEntity<?> delete(@RequestHeader HttpHeaders headers,
                                  @PathVariable Long id) {
    String user = requireUser(headers);

    var doc = docs.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "document not found"));

    if (!user.equals(doc.getOwnerUsername())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "document not found");
    }

    docs.delete(doc);
    return ResponseEntity.noContent().build();
  }
}
