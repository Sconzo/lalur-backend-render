package br.com.lalurecf.application.port.in.taxparameter;

import br.com.lalurecf.domain.enums.ParameterNature;
import br.com.lalurecf.infrastructure.dto.taxparameter.TaxParameterResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Use case para listagem de parâmetros tributários.
 *
 * <p>Suporta:
 * <ul>
 *   <li>Filtro por tipo (categoria)
 *   <li>Filtro por natureza (GLOBAL, MONTHLY, QUARTERLY)
 *   <li>Busca por código e descrição
 *   <li>Filtro por status
 *   <li>Paginação e ordenação
 * </ul>
 */
public interface ListTaxParametersUseCase {

  /**
   * Lista parâmetros tributários com filtros e paginação.
   *
   * @param type filtro por tipo (categoria) - opcional
   * @param nature filtro por natureza (GLOBAL, MONTHLY, QUARTERLY) - opcional
   * @param search busca em código e descrição - opcional
   * @param includeInactive incluir parâmetros inativos (padrão: false)
   * @param pageable configuração de paginação e ordenação
   * @return página de parâmetros
   */
  Page<TaxParameterResponse> list(
      String type,
      Long typeId,
      ParameterNature nature,
      String search,
      boolean includeInactive,
      Boolean fiscalMovementExclusive,
      Pageable pageable
  );
}
