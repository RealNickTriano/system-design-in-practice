package dev.nicktriano.bitly.url.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;

public record ShortenUrlResponse(
  @NotBlank
  String shortUrl,

  Instant expiration
) {}
