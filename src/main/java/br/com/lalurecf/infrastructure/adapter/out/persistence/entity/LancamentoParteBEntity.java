package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import br.com.lalurecf.domain.enums.TipoAjuste;
import br.com.lalurecf.domain.enums.TipoApuracao;
import br.com.lalurecf.domain.enums.TipoRelacionamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entidade JPA para Lançamento da Parte B (e-Lalur/e-Lacs).
 *
 * <p>Representa ajustes fiscais (adições/exclusões) ao lucro líquido para apuração de IRPJ/CSLL.
 * Possui validação condicional de FKs baseada no tipoRelacionamento:
 *
 * <ul>
 *   <li>CONTA_CONTABIL: contaContabil NOT NULL, contaParteB NULL
 *   <li>CONTA_PARTE_B: contaParteB NOT NULL, contaContabil NULL
 *   <li>AMBOS: ambos NOT NULL
 * </ul>
 */
@Entity
@Table(name = "tb_lancamento_parte_b")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LancamentoParteBEntity extends BaseEntity {

  /** Empresa dona do lançamento. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", nullable = false)
  private CompanyEntity company;

  /** Mês de referência do lançamento (1-12). */
  @Column(name = "mes_referencia", nullable = false)
  private Integer mesReferencia;

  /** Ano de referência do lançamento. */
  @Column(name = "ano_referencia", nullable = false)
  private Integer anoReferencia;

  /** Tipo de apuração fiscal (IRPJ ou CSLL). */
  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_apuracao", nullable = false, length = 10)
  private TipoApuracao tipoApuracao;

  /** Tipo de relacionamento com contas. */
  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_relacionamento", nullable = false, length = 20)
  private TipoRelacionamento tipoRelacionamento;

  /**
   * Conta contábil relacionada (nullable dependendo de tipoRelacionamento).
   *
   * <p>NOT NULL se tipoRelacionamento = CONTA_CONTABIL ou AMBOS.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "conta_contabil_id")
  private PlanoDeContasEntity contaContabil;

  /**
   * Conta da Parte B relacionada (nullable dependendo de tipoRelacionamento).
   *
   * <p>NOT NULL se tipoRelacionamento = CONTA_PARTE_B ou AMBOS.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "conta_parte_b_id")
  private ContaParteBEntity contaParteB;

  /** Parâmetro tributário que fundamenta o ajuste (obrigatório). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parametro_tributario_id", nullable = false)
  private TaxParameterEntity parametroTributario;

  /** Tipo de ajuste fiscal (ADICAO ou EXCLUSAO). */
  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_ajuste", nullable = false, length = 10)
  private TipoAjuste tipoAjuste;

  /** Descrição do ajuste fiscal. */
  @Column(name = "descricao", nullable = false, length = 2000)
  private String descricao;

  /** Valor do ajuste (sempre positivo). */
  @Column(name = "valor", nullable = false, precision = 19, scale = 2)
  private BigDecimal valor;

  /**
   * Validação condicional de FKs executada antes de persistir.
   *
   * @throws IllegalStateException se as regras de FK não forem atendidas
   */
  @PrePersist
  @PreUpdate
  public void validateConditionalForeignKeys() {
    if (tipoRelacionamento == null) {
      throw new IllegalStateException("tipoRelacionamento não pode ser nulo");
    }

    if (valor != null && valor.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalStateException("valor deve ser maior que zero");
    }

    switch (tipoRelacionamento) {
      case CONTA_CONTABIL:
        if (contaContabil == null) {
          throw new IllegalStateException(
              "contaContabil é obrigatória quando tipoRelacionamento = CONTA_CONTABIL");
        }
        if (contaParteB != null) {
          throw new IllegalStateException(
              "contaParteB deve ser nula quando tipoRelacionamento = CONTA_CONTABIL");
        }
        break;

      case CONTA_PARTE_B:
        if (contaParteB == null) {
          throw new IllegalStateException(
              "contaParteB é obrigatória quando tipoRelacionamento = CONTA_PARTE_B");
        }
        if (contaContabil != null) {
          throw new IllegalStateException(
              "contaContabil deve ser nula quando tipoRelacionamento = CONTA_PARTE_B");
        }
        break;

      case AMBOS:
        if (contaContabil == null) {
          throw new IllegalStateException(
              "contaContabil é obrigatória quando tipoRelacionamento = AMBOS");
        }
        if (contaParteB == null) {
          throw new IllegalStateException(
              "contaParteB é obrigatória quando tipoRelacionamento = AMBOS");
        }
        break;

      default:
        throw new IllegalStateException("tipoRelacionamento inválido: " + tipoRelacionamento);
    }
  }
}
