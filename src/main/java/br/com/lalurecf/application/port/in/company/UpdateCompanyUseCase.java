package br.com.lalurecf.application.port.in.company;

import br.com.lalurecf.infrastructure.dto.company.CompanyDetailResponse;
import br.com.lalurecf.infrastructure.dto.company.UpdateCompanyRequest;

/**
 * Use case para atualização de empresa.
 *
 * <p>Permite editar todos os campos exceto CNPJ (imutável).
 */
public interface UpdateCompanyUseCase {

  /**
   * Atualiza dados de uma empresa existente.
   *
   * @param id ID da empresa
   * @param request novos dados da empresa
   * @return empresa atualizada
   * @throws jakarta.persistence.EntityNotFoundException se empresa não existir
   */
  CompanyDetailResponse update(Long id, UpdateCompanyRequest request);
}
