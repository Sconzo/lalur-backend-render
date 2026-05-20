package br.com.lalurecf.domain.enums;

/**
 * Status do arquivo ECF no ciclo de vida do sistema.
 *
 * <ul>
 *   <li>DRAFT: recém gerado ou importado, ainda não validado
 *   <li>VALIDATED: validação bem-sucedida
 *   <li>ERROR: validação com erros
 *   <li>FINALIZED: ECF Completo transmitido ao SPED, bloqueado para modificação
 * </ul>
 */
public enum EcfFileStatus {
  DRAFT,
  VALIDATED,
  ERROR,
  FINALIZED
}
