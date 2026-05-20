package br.com.lalurecf.application.port.in.contareferencial;

import br.com.lalurecf.infrastructure.dto.user.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusResponse;

/**
 * Port IN para caso de uso de alteração de status de conta referencial RFB.
 *
 * <p>Define contrato para alternar status entre ACTIVE e INACTIVE.
 */
public interface ToggleContaReferencialStatusUseCase {

  /**
   * Altera status de uma conta referencial.
   *
   * @param id ID da conta
   * @param request novo status
   * @return resposta com novo status
   * @throws br.com.lalurecf.infrastructure.exception.ResourceNotFoundException se conta não
   *     encontrada
   */
  ToggleStatusResponse toggleStatus(Long id, ToggleStatusRequest request);
}
