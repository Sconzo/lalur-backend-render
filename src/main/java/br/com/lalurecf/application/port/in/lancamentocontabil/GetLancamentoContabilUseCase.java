package br.com.lalurecf.application.port.in.lancamentocontabil;

import br.com.lalurecf.domain.model.LancamentoContabil;

/**
 * Use case para buscar lançamento contábil por ID.
 */
public interface GetLancamentoContabilUseCase {

  /**
   * Busca lançamento contábil por ID.
   *
   * @param id ID do lançamento
   * @return lançamento encontrado
   * @throws br.com.lalurecf.domain.exception.ResourceNotFoundException se não encontrado
   */
  LancamentoContabil getById(Long id);
}
