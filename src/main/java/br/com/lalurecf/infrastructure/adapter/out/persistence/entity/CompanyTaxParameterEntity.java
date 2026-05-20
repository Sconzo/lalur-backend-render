package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity representing the association between Company and TaxParameter.
 * Explicit table with audit fields (ADR-001 v2.0).
 * Table: tb_empresa_parametros_tributarios
 */
@Entity
@Table(
    name = "tb_empresa_parametros_tributarios",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"empresa_id", "parametro_tributario_id"}
    )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyTaxParameterEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "empresa_id", nullable = false)
  private Long companyId;

  @Column(name = "parametro_tributario_id", nullable = false)
  private Long taxParameterId;

  @Column(name = "criado_por")
  private Long createdBy;

  @Column(name = "criado_em", nullable = false)
  private LocalDateTime createdAt;
}
