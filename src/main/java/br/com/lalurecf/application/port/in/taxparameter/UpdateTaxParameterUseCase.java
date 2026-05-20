package br.com.lalurecf.application.port.in.taxparameter;

import br.com.lalurecf.infrastructure.dto.taxparameter.TaxParameterResponse;
import br.com.lalurecf.infrastructure.dto.taxparameter.UpdateTaxParameterRequest;

/**
 * Use case para atualização de parâmetro tributário.
 */
public interface UpdateTaxParameterUseCase {

  /**
   * Atualiza parâmetro tributário.
   *
   * @param id ID do parâmetro
   * @param request novos dados (código, tipo, descrição)
   * @return parâmetro atualizado
   * @throws jakarta.persistence.EntityNotFoundException se não encontrado
   * @throws IllegalArgumentException se tentar alterar código para um já existente
   */
  TaxParameterResponse update(Long id, UpdateTaxParameterRequest request);
}
