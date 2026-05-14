package dev.nicktriano.bitly.url;

public class UrlExpiredException extends RuntimeException {
    public UrlExpiredException(String shortCode) {
        super("URL with short code '" + shortCode + "' has expired");
    }
}
