
package com.mini.g2p.profile.domain;

import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "profile_documents")
@Getter @Setter
public class ProfileDocument {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String ownerUsername;

  @Column(nullable = false)
  private String type;

  private String contentType;

  @Column(nullable = false)
  private Long size;

  private String filename;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @JsonIgnore
  private byte[] data;
}
