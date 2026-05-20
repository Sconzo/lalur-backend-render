package br.com.lalurecf.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuração JPA com suporte a auditoria automática.
 *
 * <p>Habilita {@literal @}EnableJpaAuditing para que campos anotados com {@literal @}CreatedDate,
 * {@literal @}LastModifiedDate, {@literal @}CreatedBy e {@literal @}LastModifiedBy sejam
 * preenchidos automaticamente.
 *
 * <p>O AuditorAware (SpringSecurityAuditorAware) é detectado automaticamente via @Component e
 * retorna o ID do usuário autenticado.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
  // AuditorAware is auto-detected via @Component annotation
}
