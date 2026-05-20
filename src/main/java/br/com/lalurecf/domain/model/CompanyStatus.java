package br.com.lalurecf.domain.model;

/**
 * Type alias para Status específico de Company.
 *
 * <p>Mantém nomenclatura semântica nos DTOs de Company.
 */
public enum CompanyStatus {
  /** Empresa ativa e visível no sistema. */
  ACTIVE,

  /**
   * Empresa inativa (soft delete). Não aparece em queries normais mas permanece no banco para
   * auditoria.
   */
  INACTIVE;

  /**
   * Converte para o enum Status genérico do domínio.
   */
  public br.com.lalurecf.domain.enums.Status toStatus() {
    return br.com.lalurecf.domain.enums.Status.valueOf(this.name());
  }

  /**
   * Converte de Status genérico para CompanyStatus.
   */
  public static CompanyStatus fromStatus(br.com.lalurecf.domain.enums.Status status) {
    return CompanyStatus.valueOf(status.name());
  }
}
