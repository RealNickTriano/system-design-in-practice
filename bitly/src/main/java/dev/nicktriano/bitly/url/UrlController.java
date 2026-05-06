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
import org.springframework.http.ProblemDetail;
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
  public ResponseEntity<ProblemDetail> handleNotFound(UrlNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage()));
  }

  @ExceptionHandler(UrlExpiredException.class)
  public ResponseEntity<ProblemDetail> handleExpired(UrlExpiredException e) {
    return ResponseEntity.status(HttpStatus.GONE)
        .body(ProblemDetail.forStatusAndDetail(HttpStatus.GONE, e.getMessage()));
  }

  @ExceptionHandler(ShortCodeGenerationException.class)
  public ResponseEntity<ProblemDetail> handleCodeGenerationFailure(ShortCodeGenerationException e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
  }

}
