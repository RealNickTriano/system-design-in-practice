package dev.nicktriano.bitly.url;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
class UrlControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    UrlService urlService;

    @Test
    void postUrls_validRequest_returns200WithShortUrl() throws Exception {
        when(urlService.shortenAndSaveUrl(anyString(), any(), any()))
            .thenReturn(new UrlEntity("abc12345", "https://example.com", null));

        mockMvc.perform(post("/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shortUrl").value(containsString("abc12345")));
    }

    @Test
    void postUrls_missingUrl_returns400() throws Exception {
        mockMvc.perform(post("/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void postUrls_invalidUrl_returns400() throws Exception {
        mockMvc.perform(post("/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"not-a-url\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void postUrls_withAlias_shortUrlContainsAlias() throws Exception {
        when(urlService.shortenAndSaveUrl(anyString(), any(), any()))
            .thenReturn(new UrlEntity("myalias", "https://example.com", null));

        mockMvc.perform(post("/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com\",\"alias\":\"myalias\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shortUrl").value(containsString("myalias")));
    }

    @Test
    void postUrls_withExpiration_responseContainsExpiration() throws Exception {
        Instant expiration = Instant.parse("2027-01-01T00:00:00Z");
        when(urlService.shortenAndSaveUrl(anyString(), any(), any()))
            .thenReturn(new UrlEntity("abc12345", "https://example.com", expiration));

        mockMvc.perform(post("/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com\",\"expiration\":\"2027-01-01T00:00:00Z\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expiration").exists());
    }

    @Test
    void getShortCode_validCode_returns302WithLocation() throws Exception {
        when(urlService.getOriginalUrlFromShortCode("abc12345")).thenReturn("https://example.com");

        mockMvc.perform(get("/abc12345"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "https://example.com"));
    }

    @Test
    void getShortCode_unknownCode_returns404WithProblemDetail() throws Exception {
        when(urlService.getOriginalUrlFromShortCode("unknown")).thenThrow(new UrlNotFoundException("unknown"));

        mockMvc.perform(get("/unknown"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void getShortCode_expiredCode_returns410WithProblemDetail() throws Exception {
        when(urlService.getOriginalUrlFromShortCode("expired")).thenThrow(new UrlExpiredException("expired"));

        mockMvc.perform(get("/expired"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void postUrls_duplicateAlias_returns409WithProblemDetail() throws Exception {
        when(urlService.shortenAndSaveUrl(anyString(), any(), any())).thenThrow(new AliasConflictException("taken"));

        mockMvc.perform(post("/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com\",\"alias\":\"taken\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(containsString("taken")));
    }

    @Test
    void postUrls_aliasWithSpace_returns400WithValidationMessage() throws Exception {
        mockMvc.perform(post("/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com\",\"alias\":\"my alias\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("letters, numbers, hyphens")));
    }

    @Test
    void postUrls_aliasWithSpecialChars_returns400() throws Exception {
        mockMvc.perform(post("/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com\",\"alias\":\"my@alias!\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void postUrls_codeGenerationFailure_returns500WithProblemDetail() throws Exception {
        when(urlService.shortenAndSaveUrl(anyString(), any(), any())).thenThrow(new ShortCodeGenerationException());

        mockMvc.perform(post("/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com\"}"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.detail").exists());
    }
}
