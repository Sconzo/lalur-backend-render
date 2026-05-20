package br.com.lalurecf.application.port.in.company;

import br.com.lalurecf.infrastructure.dto.company.CompanyDetailResponse;

/**
 * Use case para obter detalhes de uma empresa por ID.
 */
public interface GetCompanyUseCase {

  /**
   * Busca empresa por ID.
   *
   * @param id ID da empresa
   * @return detalhes completos da empresa
   * @throws jakarta.persistence.EntityNotFoundException se empresa não existir
   */
  CompanyDetailResponse getById(Long id);
}
