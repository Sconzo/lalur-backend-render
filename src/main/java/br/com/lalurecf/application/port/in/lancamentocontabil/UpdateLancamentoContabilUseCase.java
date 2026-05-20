package br.com.lalurecf.application.port.in.lancamentocontabil;

import br.com.lalurecf.domain.model.LancamentoContabil;

/**
 * Use case para atualizar lançamento contábil.
 * Valida Período Contábil antes de permitir edição.
 */
public interface UpdateLancamentoContabilUseCase {

  /**
   * Atualiza um lançamento contábil existente.
   * Valida Período Contábil (data >= company.periodoContabil).
   *
   * @param id ID do lançamento a ser atualizado
   * @param lancamento dados atualizados
   * @return lançamento atualizado
   * @throws br.com.lalurecf.domain.exception.ResourceNotFoundException se não encontrado
   * @throws br.com.lalurecf.domain.exception.BusinessRuleException se data < Período Contábil
   */
  LancamentoContabil update(Long id, LancamentoContabil lancamento);
}
