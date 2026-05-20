package br.com.lalurecf.domain.enums;

/**
 * Classe contábil — tipo hierárquico da conta no plano de contas.
 *
 * <p>Indica se a conta é analítica (folha, recebe lançamentos) ou sintética (agregadora, totaliza
 * saldos de contas filhas).
 */
public enum ClasseContabil {
  /** Conta analítica — nível folha, recebe lançamentos diretamente. */
  ANALITICO,

  /** Conta sintética — nível agregador, totaliza saldos de contas filhas. */
  SINTETICO
}
