package dev.nicktriano.bitly.url;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import dev.nicktriano.bitly.url.dto.ShortenUrlResponse;

@Service
public class UrlService {
  
  public ShortenUrlResponse shortenUrl(String originalUrl, Optional<String> alias, Optional<Instant> expiration) {
    return new ShortenUrlResponse("new url", null);
  }

  public Optional<String> getOriginalUrlFromShortCode(String shortCode) {
    return null;
  }
}
