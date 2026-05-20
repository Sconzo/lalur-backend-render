package br.com.lalurecf.application.port.in.taxparametertype;

import br.com.lalurecf.infrastructure.dto.taxparametertype.CreateTaxParameterTypeRequest;
import br.com.lalurecf.infrastructure.dto.taxparametertype.TaxParameterTypeResponse;

/**
 * Use case para criação de tipo de parâmetro tributário.
 *
 * <p>Validações:
 * <ul>
 *   <li>Descrição única (não pode duplicar)
 *   <li>Natureza válida (GLOBAL, MONTHLY, QUARTERLY)
 * </ul>
 */
public interface CreateTaxParameterTypeUseCase {

  /**
   * Cria um novo tipo de parâmetro tributário.
   *
   * @param request dados do tipo
   * @return tipo criado
   * @throws IllegalArgumentException se descrição já existe
   */
  TaxParameterTypeResponse create(CreateTaxParameterTypeRequest request);
}
