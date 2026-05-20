package br.com.lalurecf.application.port.in.lancamentoparteb;

import br.com.lalurecf.infrastructure.dto.lancamentoparteb.LancamentoParteBResponse;

/**
 * Port IN para caso de uso de busca de Lançamento da Parte B por ID.
 *
 * <p>Define contrato para obter um lançamento fiscal específico.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface GetLancamentoParteBUseCase {

  /**
   * Obtém lançamento da Parte B por ID.
   *
   * @param id ID do lançamento
   * @return dados do lançamento
   * @throws br.com.lalurecf.infrastructure.exception.ResourceNotFoundException se não encontrado
   */
  LancamentoParteBResponse getLancamentoParteBById(Long id);
}
