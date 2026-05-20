package br.com.lalurecf.infrastructure.adapter.out.persistence.repository;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.LancamentoContabilEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository para Lançamento Contábil.
 *
 * <p>Fornece operações de persistência para LancamentoContabilEntity.
 */
@Repository
public interface LancamentoContabilJpaRepository
    extends JpaRepository<LancamentoContabilEntity, Long>,
        JpaSpecificationExecutor<LancamentoContabilEntity> {

  /**
   * Busca todos lançamentos de uma empresa em um ano fiscal específico.
   *
   * @param companyId ID da empresa
   * @param fiscalYear ano fiscal
   * @return lista de lançamentos
   */
  List<LancamentoContabilEntity> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear);

  /**
   * Busca lançamentos de uma empresa com paginação.
   *
   * @param companyId ID da empresa
   * @param pageable configuração de paginação
   * @return página de lançamentos
   */
  Page<LancamentoContabilEntity> findByCompanyId(Long companyId, Pageable pageable);

  /**
   * Busca lançamentos para export filtrando por status, ano fiscal e range de datas no banco.
   *
   * <p>Todos os filtros são aplicados via SQL evitando carregar dados excedentes em memória.
   *
   * @param companyId ID da empresa
   * @param fiscalYear ano fiscal
   * @param status status dos lançamentos
   * @param dataInicio data inicial (inclusive, pode ser null)
   * @param dataFim data final (inclusive, pode ser null)
   * @return lista de lançamentos ordenada por data ASC
   */
  @Query(
      "SELECT l FROM LancamentoContabilEntity l "
          + "WHERE l.company.id = :companyId "
          + "AND l.fiscalYear = :fiscalYear "
          + "AND l.status = :status "
          + "AND (:dataInicio IS NULL OR l.data >= :dataInicio) "
          + "AND (:dataFim IS NULL OR l.data <= :dataFim) "
          + "ORDER BY l.data ASC")
  List<LancamentoContabilEntity> findForExport(
      @Param("companyId") Long companyId,
      @Param("fiscalYear") Integer fiscalYear,
      @Param("status") Status status,
      @Param("dataInicio") LocalDate dataInicio,
      @Param("dataFim") LocalDate dataFim);

  /**
   * Deleta fisicamente todos os lançamentos de uma empresa em um determinado mês e ano.
   *
   * @param companyId ID da empresa
   * @param mes mês (1-12)
   * @param ano ano (ex: 2024)
   * @return quantidade de registros deletados
   */
  @Modifying
  @Query(
      "DELETE FROM LancamentoContabilEntity l "
          + "WHERE l.company.id = :companyId "
          + "AND EXTRACT(MONTH FROM l.data) = :mes "
          + "AND EXTRACT(YEAR FROM l.data) = :ano")
  int deleteByCompanyIdAndMesAndAno(
      @Param("companyId") Long companyId,
      @Param("mes") Integer mes,
      @Param("ano") Integer ano);

  /**
   * Conta lançamentos de uma empresa em um determinado mês e ano.
   *
   * @param companyId ID da empresa
   * @param mes mês (1-12)
   * @param ano ano (ex: 2024)
   * @return quantidade de registros
   */
  @Query(
      "SELECT COUNT(l) FROM LancamentoContabilEntity l "
          + "WHERE l.company.id = :companyId "
          + "AND EXTRACT(MONTH FROM l.data) = :mes "
          + "AND EXTRACT(YEAR FROM l.data) = :ano")
  int countByCompanyIdAndMesAndAno(
      @Param("companyId") Long companyId,
      @Param("mes") Integer mes,
      @Param("ano") Integer ano);
}
