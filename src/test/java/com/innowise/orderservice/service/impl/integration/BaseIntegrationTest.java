package com.innowise.orderservice.service.impl.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

  static WireMockServer wireMock = new WireMockServer(wireMockConfig().dynamicPort());

  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test");

  @BeforeAll
  static void startWireMock() {
    wireMock.start();
    WireMock.configureFor("localhost", wireMock.port());
  }

  @AfterAll
  static void stopWireMock() {
    wireMock.stop();
  }

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    createSchema("order_schema");

    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.jpa.properties.hibernate.default_schema", () -> "order_schema");

    registry.add("spring.liquibase.url", postgres::getJdbcUrl);
    registry.add("spring.liquibase.user", postgres::getUsername);
    registry.add("spring.liquibase.password", postgres::getPassword);
    registry.add("spring.liquibase.default-schema", () -> "order_schema");

    registry.add("service.user-service.url", () -> "http://localhost:" + wireMock.port());

    registry.add("jwt.secret",
        () -> "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2");
    registry.add("jwt.access-token-expiration", () -> "3600000");
    registry.add("jwt.refresh-token-expiration", () -> "86400000");

    registry.add("service.user-service.url", () -> "http://localhost:" + wireMock.port());
  }

  private static void createSchema(String schemaName) {
    try (Connection conn = postgres.createConnection("");
        Statement stmt = conn.createStatement()) {
      stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to create schema " + schemaName, e);
    }
  }
}