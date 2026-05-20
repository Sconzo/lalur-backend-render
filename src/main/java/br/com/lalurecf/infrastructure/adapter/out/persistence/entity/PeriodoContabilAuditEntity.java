package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade de auditoria para rastrear alterações no Período Contábil de empresas.
 *
 * <p>Registra:
 * <ul>
 *   <li>Qual empresa teve o período alterado
 *   <li>Valor anterior e novo do período contábil
 *   <li>Quem fez a alteração (email do usuário)
 *   <li>Quando a alteração foi feita
 * </ul>
 *
 * <p>Nota: Esta entidade NÃO estende BaseEntity pois não possui soft delete.
 * Registros de auditoria devem ser permanentes e imutáveis.
 */
@Entity
@Table(name = "tb_periodo_contabil_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodoContabilAuditEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "periodo_contabil_anterior", nullable = false)
  private LocalDate periodoContabilAnterior;

  @Column(name = "periodo_contabil_novo", nullable = false)
  private LocalDate periodoContabilNovo;

  @Column(name = "changed_by", nullable = false, length = 255)
  private String changedBy;

  @Column(name = "changed_at", nullable = false)
  private LocalDateTime changedAt;
}
