package dev.nicktriano.bitly.url.dto;

import java.time.Instant;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ShortenUrlRequestBody(

  @NotBlank(message = "Url is required")
  @URL
  String url,

  @Pattern(regexp = "[A-Za-z0-9_-]+", message = "Alias may only contain letters, numbers, hyphens, and underscores")
  String alias,

  Instant expiration
) {
}
