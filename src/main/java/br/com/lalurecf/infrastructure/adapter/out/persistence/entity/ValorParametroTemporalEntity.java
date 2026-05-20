package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * JPA Entity para Valores de Parâmetros Temporais.
 *
 * <p>Representa períodos (mensais ou trimestrais) em que parâmetros tributários estão ativos
 * para uma empresa, conforme ADR-001.
 *
 * <p>Periodicidade:
 *
 * <ul>
 *   <li>Mensal: ano + mes preenchidos, trimestre NULL
 *   <li>Trimestral: ano + trimestre preenchidos, mes NULL
 * </ul>
 */
@Entity
@Table(
    name = "tb_valores_parametros_temporais",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_valores_temporais_periodo",
            columnNames = {"empresa_parametros_tributarios_id", "ano", "mes", "trimestre"}),
    indexes = {
      @Index(
          name = "idx_valores_temporais_empresa_param",
          columnList = "empresa_parametros_tributarios_id"),
      @Index(name = "idx_valores_temporais_ano", columnList = "ano"),
      @Index(name = "idx_valores_temporais_periodo", columnList = "ano, mes, trimestre")
    })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ValorParametroTemporalEntity extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_parametros_tributarios_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private EmpresaParametrosTributariosEntity empresaParametrosTributarios;

  @Column(name = "ano", nullable = false)
  private Integer ano;

  @Column(name = "mes")
  private Integer mes;

  @Column(name = "trimestre")
  private Integer trimestre;

  /**
   * Valida periodicidade antes de persistir ou atualizar.
   *
   * <p>Constraint XOR: Exatamente UM campo deve estar preenchido (mes OU trimestre).
   *
   * <p>Validações:
   *
   * <ul>
   *   <li>mes: deve estar entre 1 e 12
   *   <li>trimestre: deve estar entre 1 e 4
   * </ul>
   *
   * @throws IllegalStateException se ambos ou nenhum campo estiverem preenchidos
   * @throws IllegalArgumentException se valores estiverem fora do range permitido
   */
  @PrePersist
  @PreUpdate
  private void validatePeriodicity() {
    boolean hasMonth = mes != null;
    boolean hasQuarter = trimestre != null;

    // XOR constraint: exatamente UM campo preenchido
    if (hasMonth == hasQuarter) { // Ambos null ou ambos preenchidos
      throw new IllegalStateException(
          "Deve ter mes OU trimestre, nunca ambos ou nenhum");
    }

    // Validar range de mes (1-12)
    if (mes != null && (mes < 1 || mes > 12)) {
      throw new IllegalArgumentException("Mês deve estar entre 1 e 12");
    }

    // Validar range de trimestre (1-4)
    if (trimestre != null && (trimestre < 1 || trimestre > 4)) {
      throw new IllegalArgumentException("Trimestre deve estar entre 1 e 4");
    }
  }

  /**
   * Formata o período para exibição.
   *
   * @return String formatada como "Jan/2024" (mensal) ou "1º Tri/2024" (trimestral)
   */
  public String formatPeriodo() {
    if (mes != null) {
      String[] meses = {
        "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
        "Jul", "Ago", "Set", "Out", "Nov", "Dez"
      };
      return String.format("%s/%d", meses[mes - 1], ano);
    } else {
      return String.format("%dº Tri/%d", trimestre, ano);
    }
  }
}
