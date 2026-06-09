package br.com.lalurecf.infrastructure.adapter.out.persistence.repository;

import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyTaxParameterEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for CompanyTaxParameter associations.
 * Manages the relationship between companies and tax parameters with audit trail.
 */
@Repository
public interface CompanyTaxParameterJpaRepository
    extends JpaRepository<CompanyTaxParameterEntity, Long> {

  /**
   * Find all tax parameter associations for a specific company.
   *
   * @param companyId the company ID
   * @return list of associations
   */
  List<CompanyTaxParameterEntity> findByCompanyId(Long companyId);

  /**
   * Find a specific company-tax parameter association.
   *
   * @param companyId the company ID
   * @param taxParameterId the tax parameter ID
   * @return Optional containing the association if found
   */
  Optional<CompanyTaxParameterEntity> findByCompanyIdAndTaxParameterId(
      Long companyId,
      Long taxParameterId);

  /**
   * Delete all tax parameter associations for a specific company.
   * Used when updating the full list of parameters.
   *
   * @param companyId the company ID
   */
  @Modifying
  @Query("DELETE FROM CompanyTaxParameterEntity c WHERE c.companyId = :companyId")
  void deleteAllByCompanyId(@Param("companyId") Long companyId);

  /**
   * Delete a specific company-tax parameter association.
   *
   * @param companyId the company ID
   * @param taxParameterId the tax parameter ID
   */
  @Modifying
  @Query("DELETE FROM CompanyTaxParameterEntity c WHERE c.companyId = :companyId"
      + " AND c.taxParameterId = :taxParameterId")
  void deleteByCompanyIdAndTaxParameterId(
      @Param("companyId") Long companyId,
      @Param("taxParameterId") Long taxParameterId
  );

  /**
   * Busca o código do parâmetro tributário ACTIVE associado à empresa para um determinado tipo
   * (identificado pela descrição do tipo). Útil para resolver parâmetros GLOBAL como
   * {@code PERIODO_DE_APURACAO} (códigos {@code A}/{@code T}).
   *
   * @param companyId ID da empresa
   * @param typeDescription descrição do tipo de parâmetro (ex.: {@code PERIODO_DE_APURACAO})
   * @return código do parâmetro ACTIVE, ou empty se a empresa não tem o tipo associado
   */
  @Query("SELECT tp.codigo FROM CompanyTaxParameterEntity ctp, TaxParameterEntity tp"
      + " WHERE ctp.taxParameterId = tp.id"
      + " AND ctp.companyId = :companyId"
      + " AND tp.tipoParametro.descricao = :typeDescription"
      + " AND tp.status = br.com.lalurecf.domain.enums.Status.ACTIVE")
  Optional<String> findActiveParameterCodeByCompanyAndTypeDescription(
      @Param("companyId") Long companyId,
      @Param("typeDescription") String typeDescription);
}
