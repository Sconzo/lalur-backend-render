package br.com.lalurecf.domain.enums;

/**
 * Tipo de tributo para contas da Parte B (e-Lalur/e-Lacs).
 *
 * <p>Este enum define a que tributo uma conta da Parte B está relacionada. Contas podem ser
 * específicas de IRPJ, CSLL, ou aplicáveis a ambos os tributos.
 *
 * <p>Uso:
 *
 * <ul>
 *   <li>IRPJ: Conta específica do Imposto de Renda Pessoa Jurídica
 *   <li>CSLL: Conta específica da Contribuição Social sobre o Lucro Líquido
 *   <li>AMBOS: Conta aplicável tanto para IRPJ quanto CSLL
 * </ul>
 */
public enum TipoTributo {
  /** Conta específica para cálculo de IRPJ (Imposto de Renda Pessoa Jurídica). */
  IRPJ,

  /** Conta específica para cálculo de CSLL (Contribuição Social sobre o Lucro Líquido). */
  CSLL,

  /** Conta aplicável tanto para IRPJ quanto CSLL. */
  AMBOS
}
