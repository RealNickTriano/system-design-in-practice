package dev.nicktriano.bitly;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class BitlyApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

	@Container
	@ServiceConnection
	static GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(6379);

	@Test
	void contextLoads() {
	}

}
