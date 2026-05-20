package br.com.lalurecf.application.port.out;

import br.com.lalurecf.domain.model.LancamentoContabil;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port de saída para persistência de Lançamentos Contábeis.
 *
 * <p>Define as operações de persistência necessárias para gerenciar lançamentos contábeis
 * (partidas dobradas).
 *
 * <p>Segue padrão Hexagonal Architecture (Ports & Adapters).
 */
public interface LancamentoContabilRepositoryPort {

  /**
   * Salva um lançamento contábil.
   *
   * @param lancamento lançamento a ser salvo
   * @return lançamento salvo com ID gerado
   */
  LancamentoContabil save(LancamentoContabil lancamento);

  /**
   * Busca lançamento contábil por ID.
   *
   * @param id ID do lançamento
   * @return Optional contendo o lançamento se encontrado
   */
  Optional<LancamentoContabil> findById(Long id);

  /**
   * Busca todos lançamentos de uma empresa em um ano fiscal específico.
   *
   * @param companyId ID da empresa
   * @param fiscalYear ano fiscal
   * @return lista de lançamentos
   */
  List<LancamentoContabil> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear);

  /**
   * Busca lançamentos de uma empresa com paginação.
   *
   * @param companyId ID da empresa
   * @param pageable configuração de paginação
   * @return página de lançamentos
   */
  Page<LancamentoContabil> findByCompanyId(Long companyId, Pageable pageable);

  /**
   * Busca lançamentos para export aplicando filtros no banco (status, fiscalYear, range de datas).
   *
   * @param companyId ID da empresa
   * @param fiscalYear ano fiscal
   * @param dataInicio data inicial (inclusive, opcional)
   * @param dataFim data final (inclusive, opcional)
   * @return lista de lançamentos ativos ordenados por data ASC
   */
  List<LancamentoContabil> findForExport(
      Long companyId, Integer fiscalYear, LocalDate dataInicio, LocalDate dataFim);

  /**
   * Busca lançamentos aplicando filtros dinâmicos direto no banco com paginação.
   *
   * @param companyId ID da empresa (obrigatório)
   * @param contaDebitoId filtro opcional por conta de débito
   * @param contaCreditoId filtro opcional por conta de crédito
   * @param data filtro opcional por data exata
   * @param dataInicio filtro opcional por data inicial (inclusive)
   * @param dataFim filtro opcional por data final (inclusive)
   * @param fiscalYear filtro opcional por ano fiscal
   * @param includeInactive se true, inclui lançamentos inativos
   * @param pageable configuração de paginação
   * @return página filtrada de lançamentos
   */
  Page<LancamentoContabil> findFiltered(
      Long companyId,
      Long contaDebitoId,
      Long contaCreditoId,
      LocalDate data,
      LocalDate dataInicio,
      LocalDate dataFim,
      Integer fiscalYear,
      boolean includeInactive,
      Pageable pageable);

  /**
   * Salva uma lista de lançamentos contábeis em batch via JDBC.
   *
   * @param lancamentos lista de lançamentos a salvar
   */
  void saveAll(List<LancamentoContabil> lancamentos);

  /**
   * Deleta um lançamento contábil.
   *
   * <p>Nota: Implementação deve usar soft delete via campo status.
   *
   * @param id ID do lançamento a deletar
   */
  void deleteById(Long id);

  /**
   * Deleta fisicamente todos os lançamentos de uma empresa em um determinado mês e ano.
   *
   * @param companyId ID da empresa
   * @param mes mês (1-12)
   * @param ano ano (ex: 2024)
   * @return quantidade de registros deletados
   */
  int deleteByCompanyIdAndMesAndAno(Long companyId, Integer mes, Integer ano);
}
