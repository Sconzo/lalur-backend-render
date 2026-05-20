package br.com.lalurecf.application.port.in.company;

import br.com.lalurecf.domain.model.CompanyStatus;
import br.com.lalurecf.infrastructure.dto.company.ToggleStatusResponse;

/**
 * Use case para alternar status de empresa (ACTIVE ↔ INACTIVE).
 */
public interface ToggleCompanyStatusUseCase {

  /**
   * Alterna o status de uma empresa.
   *
   * @param id ID da empresa
   * @param newStatus novo status desejado
   * @return resposta com sucesso e novo status
   * @throws jakarta.persistence.EntityNotFoundException se empresa não existir
   */
  ToggleStatusResponse toggleStatus(Long id, CompanyStatus newStatus);
}
