package dev.nicktriano.bitly.url;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import io.seruco.encoding.base62.Base62;

@Service
public class UrlService {

  private static final int SHORT_CODE_LENGTH = 8;
  private static final int MAX_INSERT_ATTEMPTS = 5;
  private static final Base62 base62 = Base62.createInstance();
  private static final SecureRandom random = new SecureRandom();

  private final UrlRepository urlRepository;

  public UrlService(UrlRepository urlRepository) {
    this.urlRepository = urlRepository;
  }

  public UrlEntity shortenAndSaveUrl(String originalUrl, Optional<String> alias, Optional<Instant> expiration) {
    Instant expirationValue = expiration.orElse(null);

    if (alias.isPresent()) {
      String aliasValue = alias.get();
      if (urlRepository.existsById(aliasValue)) {
        throw new AliasConflictException(aliasValue);
      }
      return urlRepository.save(new UrlEntity(aliasValue, originalUrl, expirationValue));
    }

    int attempts = 0;
    while (attempts < MAX_INSERT_ATTEMPTS) {
      try {
        attempts++;
        String shortCode = shorten(originalUrl);
        return urlRepository.save(new UrlEntity(shortCode, originalUrl, expirationValue));
      } catch (DataIntegrityViolationException e) {
        // short code collision — retry with a new one
      }
    }

    throw new ShortCodeGenerationException();

  }

  @Cacheable("urls")
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
    String canonical = canonicalize(url);

    byte[] salt = new byte[8];
    random.nextBytes(salt);

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(salt);
      byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));

      return new String(base62.encode(hash), StandardCharsets.UTF_8).substring(0, SHORT_CODE_LENGTH);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private String canonicalize(String url) {
    URI uri = URI.create(url);
    String host = uri.getHost().toLowerCase();
    int port = uri.getPort();
    boolean isDefaultPort = (uri.getScheme().equals("https") && port == 443)
        || (uri.getScheme().equals("http") && port == 80);

    String path = uri.getPath().isEmpty() || uri.getPath().equals("/") ? "" : uri.getPath().replaceAll("/+$", "");

    return URI.create(
        uri.getScheme() + "://" + host + (isDefaultPort || port == -1 ? "" : ":" + port) + path
    ).toString();
  }
}
