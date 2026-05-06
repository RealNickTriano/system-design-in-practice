package dev.nicktriano.bitly.url;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UrlIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UrlRepository urlRepository;

    @Autowired
    UrlService urlService;

    @BeforeEach
    void setUp() {
        urlRepository.deleteAll();
    }

    @Test
    void shortenAndResolve_roundTrip() throws Exception {
        String response = mockMvc.perform(post("/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String shortCode = response.replaceAll(".*/(\\w+)\".*", "$1");

        mockMvc.perform(get("/{code}", shortCode))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "https://example.com"));
    }

    @Test
    void shortenWithAlias_resolvesByAlias() throws Exception {
        mockMvc.perform(post("/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com\",\"alias\":\"myalias\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/myalias"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "https://example.com"));
    }

    @Test
    void resolveExpiredUrl_returns410() throws Exception {
        urlService.shortenAndSaveUrl("https://example.com", Optional.of("expiredlink"),
            Optional.of(Instant.now().minusSeconds(3600)));

        mockMvc.perform(get("/expiredlink"))
            .andExpect(status().isGone());
    }

    @Test
    void resolveUnknownCode_returns404() throws Exception {
        mockMvc.perform(get("/nonexistent"))
            .andExpect(status().isNotFound());
    }

    @Test
    void auditingFields_arePopulatedAfterSave() throws Exception {
        mockMvc.perform(post("/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com\",\"alias\":\"auditcheck\"}"))
            .andExpect(status().isOk());

        UrlEntity entity = urlRepository.findById("auditcheck").orElseThrow();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }
}
