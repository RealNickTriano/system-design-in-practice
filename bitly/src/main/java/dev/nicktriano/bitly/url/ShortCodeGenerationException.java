package dev.nicktriano.bitly.url;

public class ShortCodeGenerationException extends RuntimeException {
    public ShortCodeGenerationException() {
        super("Failed to generate a unique short code");
    }
}
