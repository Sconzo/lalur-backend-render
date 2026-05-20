package br.com.lalurecf.application.port.in.lancamentoparteb;

import br.com.lalurecf.infrastructure.dto.user.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusResponse;

/**
 * Port IN para caso de uso de alteração de status de Lançamento da Parte B.
 *
 * <p>Define contrato para alternar status entre ACTIVE e INACTIVE (soft delete).
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface ToggleLancamentoParteBStatusUseCase {

  /**
   * Altera status de um lançamento da Parte B.
   *
   * @param id ID do lançamento
   * @param request novo status desejado
   * @return resposta com resultado da operação
   * @throws br.com.lalurecf.infrastructure.exception.ResourceNotFoundException se não encontrado
   */
  ToggleStatusResponse toggleStatus(Long id, ToggleStatusRequest request);
}
