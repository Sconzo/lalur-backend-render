package br.com.lalurecf.application.port.in.lancamentoparteb;

import br.com.lalurecf.domain.enums.TipoAjuste;
import br.com.lalurecf.domain.enums.TipoApuracao;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.LancamentoParteBResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port IN para caso de uso de listagem de Lançamentos da Parte B.
 *
 * <p>Define contrato para listar lançamentos fiscais com filtros e paginação.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface ListLancamentoParteBUseCase {

  /**
   * Lista lançamentos da Parte B com filtros opcionais.
   *
   * @param anoReferencia filtro por ano de referência (opcional)
   * @param mesReferencia filtro por mês de referência (opcional)
   * @param tipoApuracao filtro por tipo de apuração (opcional)
   * @param tipoAjuste filtro por tipo de ajuste (opcional)
   * @param includeInactive se deve incluir lançamentos inativos
   * @param pageable configuração de paginação
   * @return página de lançamentos
   */
  Page<LancamentoParteBResponse> listLancamentosParteB(
      Integer anoReferencia,
      Integer mesReferencia,
      TipoApuracao tipoApuracao,
      TipoAjuste tipoAjuste,
      Boolean includeInactive,
      Pageable pageable);
}
