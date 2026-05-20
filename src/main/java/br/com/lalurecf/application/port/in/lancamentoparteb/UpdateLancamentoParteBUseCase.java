package br.com.lalurecf.application.port.in.lancamentoparteb;

import br.com.lalurecf.infrastructure.dto.lancamentoparteb.LancamentoParteBResponse;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.UpdateLancamentoParteBRequest;

/**
 * Port IN para caso de uso de atualização de Lançamento da Parte B.
 *
 * <p>Define contrato para editar lançamento fiscal com revalidações.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface UpdateLancamentoParteBUseCase {

  /**
   * Atualiza lançamento da Parte B existente.
   *
   * @param id ID do lançamento a atualizar
   * @param request novos dados do lançamento
   * @return lançamento atualizado
   * @throws br.com.lalurecf.infrastructure.exception.ResourceNotFoundException se não encontrado
   * @throws IllegalArgumentException se validações de negócio falharem
   */
  LancamentoParteBResponse updateLancamentoParteB(Long id, UpdateLancamentoParteBRequest request);
}
