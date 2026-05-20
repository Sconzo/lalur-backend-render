package br.com.lalurecf.domain.enums;

/**
 * Tipo de saldo para contas da Parte B (e-Lalur/e-Lacs).
 *
 * <p>Este enum define a natureza do saldo de uma conta fiscal da Parte B, indicando se o saldo é
 * devedor ou credor.
 *
 * <p>Uso:
 *
 * <ul>
 *   <li>DEVEDOR: Saldo devedor (ativo, prejuízo fiscal, adições temporárias)
 *   <li>CREDOR: Saldo credor (passivo, exclusões temporárias)
 * </ul>
 */
public enum TipoSaldo {
  /** Saldo devedor (ativo, prejuízo fiscal, adições temporárias). */
  DEVEDOR,

  /** Saldo credor (passivo, exclusões temporárias). */
  CREDOR
}
