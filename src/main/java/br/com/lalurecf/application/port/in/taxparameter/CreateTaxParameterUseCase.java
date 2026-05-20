package br.com.lalurecf.application.port.in.taxparameter;

import br.com.lalurecf.infrastructure.dto.taxparameter.CreateTaxParameterRequest;
import br.com.lalurecf.infrastructure.dto.taxparameter.TaxParameterResponse;

/**
 * Use case para criação de parâmetro tributário.
 *
 * <p>Validações:
 * <ul>
 *   <li>Código único (não pode duplicar)
 *   <li>Código alfanumérico com hífens
 * </ul>
 */
public interface CreateTaxParameterUseCase {

  /**
   * Cria um novo parâmetro tributário.
   *
   * @param request dados do parâmetro
   * @return parâmetro criado
   * @throws IllegalArgumentException se código já existe
   */
  TaxParameterResponse create(CreateTaxParameterRequest request);
}
