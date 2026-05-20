package br.com.lalurecf.application.port.in.taxparameter;

import br.com.lalurecf.infrastructure.dto.taxparameter.TaxParameterTypeGroup;
import java.util.HashMap;
import java.util.List;

/**
 * Use case para buscar tipos/categorias distintos de parâmetros tributários.
 *
 * <p>Usado para popular dropdowns de filtros.
 */
public interface GetTaxParameterTypesUseCase {

  /**
   * Busca tipos únicos de parâmetros tributários com filtro opcional.
   *
   * @param search texto de busca (opcional)
   * @return lista de tipos únicos ordenados (limitado a 100 resultados)
   */
  List<String> getTypes(String search);

  /**
   * Busca parâmetros tributários para criação de empresa.
   *
   * @return parâmetros tributários agrupados por tipo com natureza
   */
  HashMap<String, TaxParameterTypeGroup> getTaxParametersForCompanyCreation();

}
