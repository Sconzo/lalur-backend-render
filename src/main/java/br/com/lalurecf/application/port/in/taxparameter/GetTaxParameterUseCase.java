package br.com.lalurecf.application.port.in.taxparameter;

import br.com.lalurecf.infrastructure.dto.taxparameter.TaxParameterResponse;

/**
 * Use case para busca de parâmetro tributário por ID.
 */
public interface GetTaxParameterUseCase {

  /**
   * Busca parâmetro tributário por ID.
   *
   * @param id ID do parâmetro
   * @return parâmetro encontrado
   * @throws jakarta.persistence.EntityNotFoundException se não encontrado
   */
  TaxParameterResponse getById(Long id);
}
