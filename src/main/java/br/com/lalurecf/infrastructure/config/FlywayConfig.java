package br.com.lalurecf.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração customizada do Flyway.
 *
 * <p>Executa repair antes de migrate para corrigir checksums desatualizados na tabela
 * flyway_schema_history. Isso é necessário quando migrations já aplicadas são editadas (ex:
 * correção de typos, ajustes em seeds).
 */
@Slf4j
@Configuration
public class FlywayConfig {

  /**
   * Strategy que executa repair antes de migrate para corrigir checksum mismatches.
   *
   * @return FlywayMigrationStrategy customizada
   */
  @Bean
  public FlywayMigrationStrategy flywayMigrationStrategy() {
    return (Flyway flyway) -> {
      log.info("Running Flyway repair to fix checksum mismatches...");
      flyway.repair();
      log.info("Running Flyway migrate...");
      flyway.migrate();
    };
  }
}
