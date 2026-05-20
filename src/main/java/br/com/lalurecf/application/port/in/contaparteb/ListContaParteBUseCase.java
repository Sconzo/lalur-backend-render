package br.com.lalurecf.application.port.in.contaparteb;

import br.com.lalurecf.domain.enums.TipoTributo;
import br.com.lalurecf.infrastructure.dto.contaparteb.ContaParteBResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port IN para caso de uso de listagem de contas da Parte B (e-Lalur/e-Lacs).
 *
 * <p>Define contrato para consulta de contas fiscais com filtros e paginação.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface ListContaParteBUseCase {

  /**
   * Lista contas da Parte B da empresa no contexto com filtros e paginação.
   *
   * @param search termo de busca em codigoConta e descricao (opcional)
   * @param anoBase filtro por ano base (opcional)
   * @param tipoTributo filtro por tipo de tributo (opcional)
   * @param includeInactive se deve incluir contas inativas
   * @param pageable configuração de paginação
   * @return página de contas Parte B
   */
  Page<ContaParteBResponse> listContasParteB(
      String search,
      Integer anoBase,
      TipoTributo tipoTributo,
      Boolean includeInactive,
      Pageable pageable);
}
