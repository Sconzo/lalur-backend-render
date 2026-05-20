package br.com.lalurecf.domain.enums;

/**
 * Tipo de conta contábil.
 *
 * <p>Define a classificação fundamental da conta no plano de contas.
 */
public enum AccountType {
  /** Conta do Ativo. */
  ATIVO,

  /** Conta do Passivo. */
  PASSIVO,

  /** Conta do Patrimônio Líquido. */
  PATRIMONIO_LIQUIDO,

  /** Conta de Receita. */
  RECEITA,

  /** Conta de Despesa. */
  DESPESA,

  /** Conta de Custo. */
  CUSTO,

  /** Conta de Resultado. */
  RESULTADO,

  /** Conta de Compensação. */
  COMPENSACAO,

  /** Conta Retificadora do Ativo. */
  ATIVO_RETIFICADORA,

  /** Conta Retificadora do Passivo. */
  PASSIVO_RETIFICADORA
}
