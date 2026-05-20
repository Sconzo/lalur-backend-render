package br.com.lalurecf.util;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Classe base para testes de integração com TestContainers.
 *
 * <p>Configura um container PostgreSQL compartilhado para todos os testes de integração. Todos os
 * testes de integração devem estender esta classe.
 */
@SpringBootTest(properties = {"spring.profiles.active=test"})
@Testcontainers
public abstract class IntegrationTestBase {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15.5-alpine")
          .withDatabaseName("test_db")
          .withUsername("test_user")
          .withPassword("test_pass");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }
}
