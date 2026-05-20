package br.com.lalurecf.domain.enums;

/**
 * Natureza da conta contábil.
 *
 * <p>Define o saldo natural da conta (devedora ou credora).
 */
public enum NaturezaConta {
  /** Conta de natureza devedora (débito aumenta saldo). */
  DEVEDORA,

  /** Conta de natureza credora (crédito aumenta saldo). */
  CREDORA
}
