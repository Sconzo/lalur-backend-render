package br.com.lalurecf.domain.enums;

/**
 * Status de entidades para implementar soft delete.
 *
 * <p>Este enum é usado em todas as entidades do sistema para evitar DELETE físico. Ao invés de
 * deletar registros, marcamos como INACTIVE, permitindo auditoria completa e possibilidade de
 * recovery de dados.
 *
 * <p>Uso:
 *
 * <ul>
 *   <li>ACTIVE: Entidade ativa e visível no sistema
 *   <li>INACTIVE: Entidade "deletada" logicamente, não aparece em queries normais
 * </ul>
 */
public enum Status {
  /** Entidade ativa e visível no sistema. */
  ACTIVE,

  /**
   * Entidade inativa (soft delete). Não aparece em queries normais mas permanece no banco para
   * auditoria.
   */
  INACTIVE
}
