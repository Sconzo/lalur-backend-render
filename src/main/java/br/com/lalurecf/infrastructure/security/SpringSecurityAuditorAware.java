package br.com.lalurecf.infrastructure.security;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Implementação de AuditorAware para auditoria JPA.
 *
 * <p>Fornece o auditor atual (usuário autenticado) para campos @CreatedBy e @LastModifiedBy.
 * Retorna o ID do usuário autenticado (extraído do custom principal) ou SYSTEM_USER_ID se não
 * autenticado.
 *
 * <p><strong>IMPORTANTE:</strong> NÃO faz queries ao banco para evitar StackOverflow em loops de
 * auditoria. O ID do usuário deve estar disponível no SecurityContext via
 * JwtAuthenticationFilter.
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<Long> {

  /**
   * ID do usuário SYSTEM (usado para operações sem autenticação). Este ID é fixo e corresponde ao
   * usuário system@lalurecf.com.br.
   */
  private static final Long SYSTEM_USER_ID = 1L;

  /**
   * Obtém o auditor atual do contexto de segurança.
   *
   * <p>Extrai o ID do usuário do principal customizado configurado em JwtAuthenticationFilter.
   *
   * @return Optional contendo o ID do usuário autenticado ou SYSTEM_USER_ID se não autenticado
   */
  @Override
  public Optional<Long> getCurrentAuditor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.of(SYSTEM_USER_ID);
    }

    // Extrair userId do custom principal (Long) configurado em JwtAuthenticationFilter
    Object principal = authentication.getPrincipal();

    // Verificar se é usuário anônimo (String "anonymousUser")
    if ("anonymousUser".equals(principal)) {
      return Optional.of(SYSTEM_USER_ID);
    }

    // Se principal é Long (userId configurado em JwtAuthenticationFilter)
    if (principal instanceof Long userId) {
      return Optional.of(userId);
    }

    // Fallback: SYSTEM_USER_ID (caso principal não seja Long nem String)
    return Optional.of(SYSTEM_USER_ID);
  }
}
