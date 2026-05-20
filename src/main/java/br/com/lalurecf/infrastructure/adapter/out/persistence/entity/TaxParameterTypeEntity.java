package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import br.com.lalurecf.domain.enums.ParameterNature;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * JPA Entity para Tipos de Parâmetros Tributários.
 *
 * <p>Agrupa parâmetros tributários por tipo e define sua natureza (GLOBAL, MONTHLY, QUARTERLY).
 * Permite que usuários criem novos tipos de parâmetros com naturezas específicas.
 */
@Entity
@Table(name = "tb_tipos_parametros_tributarios")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TaxParameterTypeEntity extends BaseEntity {

  @Column(name = "descricao", nullable = false, unique = true, length = 255)
  private String descricao;

  @Enumerated(EnumType.STRING)
  @Column(name = "natureza", nullable = false, length = 20)
  private ParameterNature natureza;

  @Column(name = "obrigatorio")
  private Boolean obrigatorio;

  @Column(name = "ordem_exibicao")
  private Integer ordemExibicao;

  @Column(name = "exclusivo_lancamentos", nullable = false)
  private Boolean exclusivoLancamentos;
}
