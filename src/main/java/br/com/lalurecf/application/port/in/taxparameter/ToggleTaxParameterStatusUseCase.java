package br.com.lalurecf.application.port.in.taxparameter;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.infrastructure.dto.company.ToggleStatusResponse;

/**
 * Use case para alternar status de parâmetro tributário (ACTIVE ↔ INACTIVE).
 */
public interface ToggleTaxParameterStatusUseCase {

  /**
   * Alterna status do parâmetro tributário.
   *
   * @param id ID do parâmetro
   * @param newStatus novo status
   * @return resposta com sucesso e novo status
   * @throws jakarta.persistence.EntityNotFoundException se não encontrado
   */
  ToggleStatusResponse toggleStatus(Long id, Status newStatus);
}
