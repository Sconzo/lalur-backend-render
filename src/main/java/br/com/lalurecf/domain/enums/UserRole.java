package br.com.lalurecf.domain.enums;

/**
 * Papéis (roles) de usuários no sistema.
 *
 * <p>Define os níveis de acesso e permissões dos usuários:
 *
 * <ul>
 *   <li>ADMIN: Administrador do sistema com acesso total
 *   <li>CONTADOR: Contador com acesso restrito a funcionalidades contábeis
 * </ul>
 */
public enum UserRole {
  /** Administrador do sistema. Acesso total a todas as funcionalidades. */
  ADMIN,

  /** Contador. Acesso às funcionalidades contábeis e fiscais. */
  CONTADOR
}
