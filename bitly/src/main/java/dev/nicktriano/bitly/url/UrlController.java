package dev.nicktriano.bitly.url;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import dev.nicktriano.bitly.url.dto.ShortenUrlRequestBody;
import dev.nicktriano.bitly.url.dto.ShortenUrlResponse;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
public class UrlController {

  private UrlService urlService;

  UrlController(UrlService urlService) {
    this.urlService = urlService;
  }

  @PostMapping("/urls")
  public ShortenUrlResponse shortenUrl(@RequestBody ShortenUrlRequestBody body) {
    String originalUrl = body.url();
    Optional<String> alias = Optional.ofNullable(body.alias());
    Optional<Instant> expiration = Optional.ofNullable(body.expiration());

    UrlEntity entity = urlService.shortenAndSaveUrl(originalUrl, alias, expiration);

    String shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
        .path("/{shortCode}")
        .buildAndExpand(entity.getShortCode())
        .toUriString();

    return new ShortenUrlResponse(shortUrl, entity.getExpiration());
  }

  @GetMapping("/{shortCode}")
  public ResponseEntity<Object> getOriginalUrl(@PathVariable String shortCode) {
    String originalUrl = urlService.getOriginalUrlFromShortCode(shortCode);

    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(originalUrl)).build();
  }

  @ExceptionHandler(UrlNotFoundException.class)
  public ResponseEntity<Void> handleNotFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  @ExceptionHandler(UrlExpiredException.class)
  public ResponseEntity<Void> handleExpired() {
    return ResponseEntity.status(HttpStatus.GONE).build();
  }

}
