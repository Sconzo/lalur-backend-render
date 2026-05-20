package br.com.lalurecf.application.port.in.contareferencial;

import br.com.lalurecf.infrastructure.dto.contareferencial.ContaReferencialResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port IN para caso de uso de listagem de contas referenciais RFB.
 *
 * <p>Define contrato para listar contas com paginação e filtros.
 */
public interface ListContaReferencialUseCase {

  /**
   * Lista contas referenciais com paginação e filtros.
   *
   * @param search termo de busca em codigoRfb e descricao (opcional)
   * @param anoValidade filtro por ano de validade (opcional)
   * @param includeInactive se deve incluir contas inativas
   * @param pageable configuração de paginação
   * @return página de contas referenciais
   */
  Page<ContaReferencialResponse> listContasReferenciais(
      String search, Integer anoValidade, Boolean includeInactive, Pageable pageable);
}
