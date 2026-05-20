package br.com.lalurecf.application.port.in.lancamentocontabil;

import br.com.lalurecf.domain.model.LancamentoContabil;

/**
 * Use case para alternar status de lançamento contábil (ACTIVE ↔ INACTIVE).
 * Valida Período Contábil antes de permitir alteração.
 */
public interface ToggleLancamentoContabilStatusUseCase {

  /**
   * Alterna o status do lançamento contábil.
   * Valida Período Contábil (data >= company.periodoContabil).
   *
   * @param id ID do lançamento
   * @return lançamento com status atualizado
   * @throws br.com.lalurecf.domain.exception.ResourceNotFoundException se não encontrado
   * @throws br.com.lalurecf.domain.exception.BusinessRuleException se data < Período Contábil
   */
  LancamentoContabil toggleStatus(Long id);
}
