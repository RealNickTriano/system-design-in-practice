package dev.nicktriano.bitly.url;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    UrlRepository urlRepository;

    @InjectMocks
    UrlService urlService;

    @Test
    void shortenAndSaveUrl_withAlias_usesAlias() {
        when(urlRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UrlEntity result = urlService.shortenAndSaveUrl("https://example.com", Optional.of("myalias"), Optional.empty());

        assertThat(result.getShortCode()).isEqualTo("myalias");
    }

    @Test
    void shortenAndSaveUrl_withoutAlias_generatesEightCharAlphanumericCode() {
        when(urlRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UrlEntity result = urlService.shortenAndSaveUrl("https://example.com", Optional.empty(), Optional.empty());

        assertThat(result.getShortCode()).hasSize(8).matches("[A-Za-z0-9]{8}");
    }

    @Test
    void shortenAndSaveUrl_setsExpiration() {
        when(urlRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Instant expiration = Instant.now().plusSeconds(3600);

        UrlEntity result = urlService.shortenAndSaveUrl("https://example.com", Optional.empty(), Optional.of(expiration));

        assertThat(result.getExpiration()).isEqualTo(expiration);
    }

    @Test
    void shortenAndSaveUrl_retriesOnCollision() {
        when(urlRepository.save(any()))
            .thenThrow(new DataIntegrityViolationException("collision"))
            .thenThrow(new DataIntegrityViolationException("collision"))
            .thenAnswer(inv -> inv.getArgument(0));

        UrlEntity result = urlService.shortenAndSaveUrl("https://example.com", Optional.empty(), Optional.empty());

        assertThat(result.getShortCode()).hasSize(8);
        verify(urlRepository, times(3)).save(any());
    }

    @Test
    void shortenAndSaveUrl_throwsAfterMaxRetries() {
        when(urlRepository.save(any())).thenThrow(new DataIntegrityViolationException("collision"));

        assertThatThrownBy(() -> urlService.shortenAndSaveUrl("https://example.com", Optional.empty(), Optional.empty()))
            .isInstanceOf(ShortCodeGenerationException.class);
        verify(urlRepository, times(5)).save(any());
    }

    @Test
    void getOriginalUrlFromShortCode_returnsUrl() {
        when(urlRepository.findById("abc12345")).thenReturn(Optional.of(new UrlEntity("abc12345", "https://example.com", null)));

        assertThat(urlService.getOriginalUrlFromShortCode("abc12345")).isEqualTo("https://example.com");
    }

    @Test
    void getOriginalUrlFromShortCode_throwsNotFound_forUnknownCode() {
        when(urlRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.getOriginalUrlFromShortCode("unknown"))
            .isInstanceOf(UrlNotFoundException.class);
    }

    @Test
    void getOriginalUrlFromShortCode_throwsExpired_forPastExpiration() {
        Instant past = Instant.now().minusSeconds(3600);
        when(urlRepository.findById("abc12345")).thenReturn(Optional.of(new UrlEntity("abc12345", "https://example.com", past)));

        assertThatThrownBy(() -> urlService.getOriginalUrlFromShortCode("abc12345"))
            .isInstanceOf(UrlExpiredException.class);
    }

    @Test
    void getOriginalUrlFromShortCode_returnsUrl_forFutureExpiration() {
        Instant future = Instant.now().plusSeconds(3600);
        when(urlRepository.findById("abc12345")).thenReturn(Optional.of(new UrlEntity("abc12345", "https://example.com", future)));

        assertThat(urlService.getOriginalUrlFromShortCode("abc12345")).isEqualTo("https://example.com");
    }
}
