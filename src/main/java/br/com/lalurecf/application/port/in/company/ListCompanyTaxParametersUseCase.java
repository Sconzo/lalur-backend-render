package br.com.lalurecf.application.port.in.company;

import br.com.lalurecf.infrastructure.dto.company.TaxParameterSummary;
import java.util.List;

/**
 * Use case para listagem de parâmetros tributários de uma empresa.
 *
 * <p>Retorna todos os parâmetros tributários ATIVOS associados à empresa.
 */
public interface ListCompanyTaxParametersUseCase {

  /**
   * Lista os parâmetros tributários associados a uma empresa.
   *
   * @param companyId ID da empresa
   * @return lista de parâmetros tributários (somente ACTIVE)
   * @throws jakarta.persistence.EntityNotFoundException se empresa não existir
   */
  List<TaxParameterSummary> listTaxParameters(Long companyId);
}
