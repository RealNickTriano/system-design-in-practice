package dev.nicktriano.bitly.url;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(CacheAutoConfiguration.class)
@TestPropertySource(properties = "spring.cache.type=none")
class UrlRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    UrlRepository urlRepository;

    @Test
    void save_populatesCreatedAtAndUpdatedAt() {
        UrlEntity saved = entityManager.persistAndFlush(new UrlEntity("abc12345", "https://example.com", null));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findById_existingShortCode_returnsEntity() {
        entityManager.persistAndFlush(new UrlEntity("abc12345", "https://example.com", null));
        entityManager.clear();

        Optional<UrlEntity> result = urlRepository.findById("abc12345");

        assertThat(result).isPresent();
        assertThat(result.get().getOriginalUrl()).isEqualTo("https://example.com");
    }

    @Test
    void findById_unknownShortCode_returnsEmpty() {
        assertThat(urlRepository.findById("nonexistent")).isEmpty();
    }

    @Test
    void save_duplicateShortCode_throwsException() {
        entityManager.persistAndFlush(new UrlEntity("abc12345", "https://example.com", null));
        entityManager.clear();

        assertThatThrownBy(() -> entityManager.persistAndFlush(new UrlEntity("abc12345", "https://other.com", null)))
            .isInstanceOf(Exception.class);
    }

}
