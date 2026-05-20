package br.com.lalurecf.domain.enums;

/**
 * Tipo de relacionamento do lançamento da Parte B.
 *
 * <p>Define como o lançamento fiscal se relaciona com contas contábeis e contas da Parte B.
 *
 * <ul>
 *   <li>CONTA_CONTABIL: Lançamento vinculado apenas a conta contábil
 *   <li>CONTA_PARTE_B: Lançamento vinculado apenas a conta da Parte B
 *   <li>AMBOS: Lançamento vinculado a ambas (conta contábil e conta Parte B)
 * </ul>
 */
public enum TipoRelacionamento {
  /** Lançamento relacionado apenas à conta contábil. */
  CONTA_CONTABIL,

  /** Lançamento relacionado apenas à conta da Parte B. */
  CONTA_PARTE_B,

  /** Lançamento relacionado a ambas contas (contábil e Parte B). */
  AMBOS
}
