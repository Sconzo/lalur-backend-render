package br.com.lalurecf.application.port.in.lancamentocontabil;

import br.com.lalurecf.infrastructure.dto.lancamentocontabil.DeleteLancamentoContabilBatchResponse;

/**
 * Use case para deleção em lote de lançamentos contábeis por mês e ano.
 */
public interface DeleteLancamentoContabilBatchUseCase {

  /**
   * Deleta fisicamente todos os lançamentos de uma empresa em um determinado mês e ano.
   *
   * @param companyId ID da empresa
   * @param mes mês (1-12)
   * @param ano ano (ex: 2024)
   * @return resposta com quantidade de registros deletados
   */
  DeleteLancamentoContabilBatchResponse deleteBatch(Long companyId, Integer mes, Integer ano);
}
