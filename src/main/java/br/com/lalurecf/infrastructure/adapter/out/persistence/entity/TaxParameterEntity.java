package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * JPA Entity para Parâmetros Tributários.
 *
 * <p>Estrutura flat (sem hierarquia parent/child) conforme ADR-001 v2.0. O tipo e natureza são
 * definidos através do relacionamento com TaxParameterTypeEntity.
 *
 * <p>Constraint: codigo + tipo_parametro_id devem ser únicos em conjunto (permite mesmo código para
 * tipos diferentes).
 */
@Entity
@Table(
    name = "tb_parametros_tributarios",
    uniqueConstraints = @UniqueConstraint(columnNames = {"codigo", "tipo_parametro_id"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TaxParameterEntity extends BaseEntity {

  @Column(name = "codigo", nullable = false, length = 100)
  private String codigo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tipo_parametro_id", nullable = false)
  private TaxParameterTypeEntity tipoParametro;

  @Column(name = "descricao", columnDefinition = "TEXT")
  private String descricao;
}
