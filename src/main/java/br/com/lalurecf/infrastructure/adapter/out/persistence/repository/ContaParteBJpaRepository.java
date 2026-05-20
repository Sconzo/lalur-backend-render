package br.com.lalurecf.infrastructure.adapter.out.persistence.repository;

import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ContaParteBEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository para ContaParteBEntity.
 *
 * <p>Query methods para busca de Contas da Parte B por empresa, código e ano base.
 */
@Repository
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface ContaParteBJpaRepository extends JpaRepository<ContaParteBEntity, Long> {

  /**
   * Busca todas contas Parte B de uma empresa em um ano base.
   *
   * @param companyId ID da empresa
   * @param anoBase ano base
   * @return lista de entities
   */
  List<ContaParteBEntity> findByCompanyIdAndAnoBase(Long companyId, Integer anoBase);

  /**
   * Busca conta Parte B por empresa, código e ano base.
   *
   * @param companyId ID da empresa
   * @param codigoConta código da conta
   * @param anoBase ano base
   * @return Optional com entity se encontrada
   */
  Optional<ContaParteBEntity> findByCompanyIdAndCodigoContaAndAnoBase(
      Long companyId, String codigoConta, Integer anoBase);

  /**
   * Busca todas contas Parte B de uma empresa com paginação.
   *
   * @param companyId ID da empresa
   * @param pageable configuração de paginação
   * @return página de entities da empresa
   */
  Page<ContaParteBEntity> findByCompanyId(Long companyId, Pageable pageable);
}
