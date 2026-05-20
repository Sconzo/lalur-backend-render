package br.com.lalurecf.domain.enums;

/**
 * Natureza do parâmetro tributário.
 *
 * <p>Define como o parâmetro se comporta em relação a valores temporais:
 *
 * <ul>
 *   <li>GLOBAL: Valor único para o ano todo, não aceita valores temporais
 *   <li>MONTHLY: Requer valores por mês (1-12)
 *   <li>QUARTERLY: Requer valores por trimestre (1-4)
 * </ul>
 */
public enum ParameterNature {
  /** Valor único para o ano todo. Não aceita valores temporais. */
  GLOBAL,

  /** Requer valores por mês (1-12). */
  MONTHLY,

  /** Requer valores por trimestre (1-4). */
  QUARTERLY
}
