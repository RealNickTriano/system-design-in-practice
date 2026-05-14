package dev.nicktriano.bitly.url;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import dev.nicktriano.bitly.url.dto.ShortenUrlRequestBody;
import dev.nicktriano.bitly.url.dto.ShortenUrlResponse;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
  public ShortenUrlResponse shortenUrl(@Valid @RequestBody ShortenUrlRequestBody body) {
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

  @GetMapping("/{shortCode:[A-Za-z0-9_-]+}")
  public ResponseEntity<Object> getOriginalUrl(@PathVariable String shortCode) {
    String originalUrl = urlService.getOriginalUrlFromShortCode(shortCode);

    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(originalUrl)).build();
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException e) {
    String message = e.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining(", "));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message));
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

  @ExceptionHandler(AliasConflictException.class)
  public ResponseEntity<ProblemDetail> handleAliasConflict(AliasConflictException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage()));
  }

  @ExceptionHandler(ShortCodeGenerationException.class)
  public ResponseEntity<ProblemDetail> handleCodeGenerationFailure(ShortCodeGenerationException e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
  }

}
