package br.com.lalurecf.application.port.in.planodecontas;

import br.com.lalurecf.infrastructure.dto.planodecontas.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.planodecontas.ToggleStatusResponse;

/**
 * Port IN: Use case para alternar status de conta contábil (PlanoDeContas).
 *
 * <p>Permite ativar (ACTIVE) ou inativar (INACTIVE) uma conta.
 */
public interface TogglePlanoDeContasStatusUseCase {

  /**
   * Alterna status de conta contábil.
   *
   * @param id ID da conta
   * @param request novo status desejado
   * @return confirmação da operação
   */
  ToggleStatusResponse execute(Long id, ToggleStatusRequest request);
}
