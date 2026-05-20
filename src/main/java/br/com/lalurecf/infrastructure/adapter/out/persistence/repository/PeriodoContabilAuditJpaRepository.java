package br.com.lalurecf.infrastructure.adapter.out.persistence.repository;

import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.PeriodoContabilAuditEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA para persistência de logs de auditoria do Período Contábil.
 *
 * <p>Permite rastrear histórico completo de alterações do período contábil de cada empresa.
 */
@Repository
public interface PeriodoContabilAuditJpaRepository
    extends JpaRepository<PeriodoContabilAuditEntity, Long> {

  /**
   * Busca todos os registros de auditoria de uma empresa, ordenados do mais recente ao mais
   * antigo.
   *
   * @param companyId ID da empresa
   * @return lista de registros de auditoria ordenada por data decrescente
   */
  List<PeriodoContabilAuditEntity> findByCompanyIdOrderByChangedAtDesc(Long companyId);
}
