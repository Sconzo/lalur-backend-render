package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

/**
 * JPA Entity para associação entre Empresa e Parâmetros Tributários.
 *
 * <p>Tabela de relacionamento many-to-many entre tb_empresa e tb_parametros_tributarios. Cada
 * registro representa um parâmetro tributário associado a uma empresa específica.
 *
 * <p>Esta entidade não estende BaseEntity pois tem campos de auditoria simplificados (apenas
 * criado_em e criado_por, sem atualizado_em/atualizado_por e status).
 */
@Entity
@Table(
    name = "tb_empresa_parametros_tributarios",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_empresa_parametro",
            columnNames = {"empresa_id", "parametro_tributario_id"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaParametrosTributariosEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private CompanyEntity empresa;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "parametro_tributario_id", nullable = false)
  private TaxParameterEntity parametroTributario;

  @CreatedDate
  @Column(name = "criado_em", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @CreatedBy
  @Column(name = "criado_por", updatable = false)
  private Long createdBy;
}
