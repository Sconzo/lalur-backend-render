package br.com.lalurecf.application.port.in;

import br.com.lalurecf.infrastructure.dto.user.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusResponse;

/**
 * Port IN para caso de uso de alteração de status de usuário.
 *
 * <p>Define contrato para alternar status entre ACTIVE e INACTIVE.
 */
public interface ToggleUserStatusUseCase {

  /**
   * Altera status de um usuário.
   *
   * @param id identificador do usuário
   * @param request novo status
   * @return resposta com novo status
   * @throws br.com.lalurecf.infrastructure.exception.ResourceNotFoundException se usuário não
   *     encontrado
   */
  ToggleStatusResponse toggleStatus(Long id, ToggleStatusRequest request);
}
