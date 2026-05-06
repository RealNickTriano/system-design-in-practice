package dev.nicktriano.bitly.url;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "urls")
@EntityListeners(AuditingEntityListener.class)
public class UrlEntity {

  @Id
  private String shortCode;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String originalUrl;

  private Instant expiration;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private Instant updatedAt;

  protected UrlEntity() {}

  public UrlEntity(String shortCode, String originalUrl, Instant expiration) {
      this.shortCode = shortCode;
      this.originalUrl = originalUrl;
      this.expiration = expiration;
  }

  public String getOriginalUrl() { return originalUrl; }
  public String getShortCode() { return shortCode; }
  public Instant getExpiration() { return expiration; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }

}
