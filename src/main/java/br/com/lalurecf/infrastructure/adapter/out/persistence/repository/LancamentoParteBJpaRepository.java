package br.com.lalurecf.infrastructure.adapter.out.persistence.repository;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.LancamentoParteBEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository para LancamentoParteBEntity.
 *
 * <p>Query methods para busca de Lançamentos da Parte B por empresa, ano e mês de referência.
 */
@Repository
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface LancamentoParteBJpaRepository
    extends JpaRepository<LancamentoParteBEntity, Long>,
        JpaSpecificationExecutor<LancamentoParteBEntity> {

  /**
   * Busca todos lançamentos Parte B de uma empresa em um ano de referência.
   *
   * @param companyId ID da empresa
   * @param anoReferencia ano de referência
   * @return lista de entities
   */
  List<LancamentoParteBEntity> findByCompanyIdAndAnoReferencia(
      Long companyId, Integer anoReferencia);

  /**
   * Busca lançamentos Parte B filtrados por status (ex: ACTIVE) direto no banco.
   *
   * @param companyId ID da empresa
   * @param anoReferencia ano de referência
   * @param status status dos lançamentos
   * @return lista de entities
   */
  List<LancamentoParteBEntity> findByCompanyIdAndAnoReferenciaAndStatus(
      Long companyId, Integer anoReferencia, Status status);

  /**
   * Busca todas lançamentos Parte B de uma empresa com paginação.
   *
   * @param companyId ID da empresa
   * @param pageable configuração de paginação
   * @return página de entities da empresa
   */
  Page<LancamentoParteBEntity> findByCompanyId(Long companyId, Pageable pageable);

  /**
   * Busca lançamentos por empresa, ano e mês de referência.
   *
   * @param companyId ID da empresa
   * @param anoReferencia ano de referência
   * @param mesReferencia mês de referência
   * @return lista de entities
   */
  List<LancamentoParteBEntity> findByCompanyIdAndAnoReferenciaAndMesReferencia(
      Long companyId, Integer anoReferencia, Integer mesReferencia);
}
