package dev.nicktriano.bitly.url.dto;

import java.time.Instant;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotBlank;

public record ShortenUrlRequestBody(
  
  @NotBlank(message = "Url is required")
  @URL
  String url,

  String alias,

  Instant expiration
) {
}
