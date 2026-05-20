package br.com.lalurecf.application.port.in.company;

import br.com.lalurecf.infrastructure.dto.company.CompanyResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Use case para listagem de empresas.
 *
 * <p>Suporta:
 * <ul>
 *   <li>Filtro global (busca em todos os campos)</li>
 *   <li>Filtro por CNPJ</li>
 *   <li>Filtro por Razão Social</li>
 *   <li>Combinação de filtros</li>
 *   <li>Paginação e ordenação</li>
 *   <li>Incluir/excluir empresas inativas</li>
 * </ul>
 */
public interface ListCompaniesUseCase {

  /**
   * Lista empresas com filtros e paginação.
   *
   * @param strSearch filtro global (busca em todos os campos)
   * @param cnpjFilters lista de CNPJs para filtro (comparação exata)
   * @param razaoSocialFilters lista de Razões Sociais para filtro (comparação exata)
   * @param includeInactive incluir empresas inativas (padrão: false)
   * @param pageable configuração de paginação e ordenação
   * @return página de empresas
   */
  Page<CompanyResponse> list(
      String strSearch,
      List<String> cnpjFilters,
      List<String> razaoSocialFilters,
      boolean includeInactive,
      Pageable pageable
  );
}
