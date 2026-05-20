package br.com.lalurecf.domain.enums;

/**
 * Tipo de arquivo ECF gerenciado pelo sistema.
 *
 * <ul>
 *   <li>ARQUIVO_PARCIAL: bloco M gerado a partir dos Lançamentos da Parte B
 *   <li>IMPORTED_ECF: ECF completo importado pelo usuário de sistema externo (Receita Federal)
 *   <li>COMPLETE_ECF: ECF completo resultado do merge entre ARQUIVO_PARCIAL e IMPORTED_ECF
 * </ul>
 */
public enum EcfFileType {
  ARQUIVO_PARCIAL,
  IMPORTED_ECF,
  COMPLETE_ECF
}
