package br.com.lalurecf.infrastructure.adapter.out.persistence.repository;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.PlanoDeContasEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository para PlanoDeContasEntity.
 *
 * <p>Query methods customizados para busca de contas contábeis por empresa, código e ano fiscal.
 */
@Repository
public interface PlanoDeContasJpaRepository
    extends JpaRepository<PlanoDeContasEntity, Long>,
        JpaSpecificationExecutor<PlanoDeContasEntity> {

  /**
   * Busca todas contas de uma empresa para um ano fiscal.
   *
   * @param companyId ID da empresa
   * @param fiscalYear ano fiscal
   * @return lista de contas
   */
  List<PlanoDeContasEntity> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear);

  /**
   * Busca conta por empresa, código e ano fiscal.
   *
   * @param companyId ID da empresa
   * @param code código da conta
   * @param fiscalYear ano fiscal
   * @return Optional com conta se encontrada
   */
  Optional<PlanoDeContasEntity> findByCompanyIdAndCodeAndFiscalYear(
      Long companyId, String code, Integer fiscalYear);

  /**
   * Busca todas contas de uma empresa com paginação.
   *
   * @param companyId ID da empresa
   * @param pageable configuração de paginação
   * @return página de contas
   */
  Page<PlanoDeContasEntity> findByCompanyId(Long companyId, Pageable pageable);

  /**
   * Verifica se existe ao menos uma conta com determinado status para uma empresa.
   *
   * @param companyId ID da empresa
   * @param status status a verificar
   * @return true se existir ao menos uma conta
   */
  boolean existsByCompanyIdAndStatus(Long companyId, Status status);
}
