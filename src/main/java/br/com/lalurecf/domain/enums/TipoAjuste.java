package br.com.lalurecf.domain.enums;

/**
 * Tipo de ajuste fiscal na Parte B (e-Lalur/e-Lacs).
 *
 * <p>Define se o lançamento é uma adição ou exclusão ao lucro líquido para fins de apuração da
 * base de cálculo de IRPJ/CSLL.
 */
public enum TipoAjuste {
  /** Adição ao lucro líquido (aumenta a base de cálculo). */
  ADICAO,

  /** Exclusão do lucro líquido (diminui a base de cálculo). */
  EXCLUSAO
}
