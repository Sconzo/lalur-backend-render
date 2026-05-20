package br.com.lalurecf.application.port.out;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.TipoAjuste;
import br.com.lalurecf.domain.enums.TipoApuracao;
import br.com.lalurecf.domain.model.LancamentoParteB;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port de saída para persistência de LancamentoParteB.
 *
 * <p>Interface de repositório para Lançamentos da Parte B (e-Lalur/e-Lacs), vinculados a
 * empresas específicas.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface LancamentoParteBRepositoryPort {

  /**
   * Salva um lançamento Parte B (create ou update).
   *
   * @param lancamento lançamento Parte B a salvar
   * @return lançamento Parte B salvo com ID gerado
   */
  LancamentoParteB save(LancamentoParteB lancamento);

  /**
   * Busca lançamento Parte B por ID.
   *
   * @param id ID do lançamento
   * @return Optional com lançamento se encontrado
   */
  Optional<LancamentoParteB> findById(Long id);

  /**
   * Busca todos lançamentos Parte B de uma empresa em um ano de referência.
   *
   * @param companyId ID da empresa
   * @param anoReferencia ano de referência
   * @return lista de lançamentos da empresa no ano especificado
   */
  List<LancamentoParteB> findByCompanyIdAndAnoReferencia(Long companyId, Integer anoReferencia);

  /**
   * Busca lançamentos Parte B filtrando por status no banco.
   *
   * @param companyId ID da empresa
   * @param anoReferencia ano de referência
   * @param status status dos lançamentos (ex: ACTIVE)
   * @return lista de lançamentos filtrados
   */
  List<LancamentoParteB> findByCompanyIdAndAnoReferenciaAndStatus(
      Long companyId, Integer anoReferencia, Status status);

  /**
   * Busca todos lançamentos Parte B de uma empresa com paginação.
   *
   * @param companyId ID da empresa
   * @param pageable configuração de paginação
   * @return página de lançamentos da empresa
   */
  Page<LancamentoParteB> findByCompanyId(Long companyId, Pageable pageable);

  /**
   * Busca lançamentos aplicando filtros dinâmicos direto no banco com paginação.
   *
   * @param companyId ID da empresa (obrigatório)
   * @param anoReferencia filtro opcional por ano
   * @param mesReferencia filtro opcional por mês
   * @param tipoApuracao filtro opcional por tipo de apuração
   * @param tipoAjuste filtro opcional por tipo de ajuste
   * @param includeInactive se true, inclui INACTIVE
   * @param pageable configuração de paginação
   * @return página filtrada de lançamentos
   */
  Page<LancamentoParteB> findFiltered(
      Long companyId,
      Integer anoReferencia,
      Integer mesReferencia,
      TipoApuracao tipoApuracao,
      TipoAjuste tipoAjuste,
      boolean includeInactive,
      Pageable pageable);

  /**
   * Busca lançamentos por empresa, ano e mês de referência.
   *
   * @param companyId ID da empresa
   * @param anoReferencia ano de referência
   * @param mesReferencia mês de referência
   * @return lista de lançamentos
   */
  List<LancamentoParteB> findByCompanyIdAndAnoReferenciaAndMesReferencia(
      Long companyId, Integer anoReferencia, Integer mesReferencia);

  /**
   * Salva uma lista de lançamentos Parte B em batch via JDBC.
   *
   * @param lancamentos lista de lançamentos a salvar
   */
  void saveAll(List<LancamentoParteB> lancamentos);

  /**
   * Deleta lançamento por ID.
   *
   * @param id ID do lançamento
   */
  void deleteById(Long id);
}
