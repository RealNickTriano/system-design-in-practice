package dev.nicktriano.bitly.url.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;

public record ShortenUrlRequestBody(
  @NotBlank(message = "Url is required")
  String url,

  String alias,

  Instant expiration
) {
}
