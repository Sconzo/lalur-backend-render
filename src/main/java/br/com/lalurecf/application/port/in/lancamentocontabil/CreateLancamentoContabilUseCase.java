package br.com.lalurecf.application.port.in.lancamentocontabil;

import br.com.lalurecf.domain.model.LancamentoContabil;

/**
 * Use case para criar lançamento contábil com partidas dobradas.
 */
public interface CreateLancamentoContabilUseCase {

  /**
   * Cria um novo lançamento contábil.
   * Valida partidas dobradas, Período Contábil e vínculos com empresa.
   *
   * @param lancamento lançamento a ser criado
   * @return lançamento criado com ID
   */
  LancamentoContabil create(LancamentoContabil lancamento);
}
