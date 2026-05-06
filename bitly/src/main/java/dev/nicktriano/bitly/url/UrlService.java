package dev.nicktriano.bitly.url;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import dev.nicktriano.bitly.url.dto.ShortenUrlResponse;

@Service
public class UrlService {

  private final UrlRepository urlRepository;

  public UrlService(UrlRepository urlRepository) {
    this.urlRepository = urlRepository;
  }

  public UrlEntity shortenAndSaveUrl(String originalUrl, Optional<String> alias, Optional<Instant> expiration) {
    String shortCode = alias.orElseGet(() -> shorten(originalUrl));
    Instant expirationValue = expiration.orElse(null);

    UrlEntity entity = urlRepository.save(new UrlEntity(shortCode, originalUrl, expirationValue));

    return entity;
  }

  public String getOriginalUrlFromShortCode(String shortCode) {
    return urlRepository.findById(shortCode)
        .map(url -> {
          if (url.getExpiration() != null && url.getExpiration().isBefore(Instant.now())) {
            throw new UrlExpiredException(shortCode);
          }
          return url.getOriginalUrl();
        })
        .orElseThrow(() -> new UrlNotFoundException(shortCode));
  }

  private String shorten(String url) {
    return url.substring(0, 5);
  }
}
