package br.com.lalurecf.application.port.in.contaparteb;

import br.com.lalurecf.infrastructure.dto.user.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusResponse;

/**
 * Port IN para caso de uso de alteração de status de conta da Parte B (e-Lalur/e-Lacs).
 *
 * <p>Define contrato para alternar status entre ACTIVE e INACTIVE (soft delete).
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface ToggleContaParteBStatusUseCase {

  /**
   * Altera status de uma conta da Parte B.
   *
   * @param id ID da conta
   * @param request novo status desejado
   * @return resposta com resultado da operação
   * @throws br.com.lalurecf.infrastructure.exception.ResourceNotFoundException se conta não existir
   */
  ToggleStatusResponse toggleStatus(Long id, ToggleStatusRequest request);
}
