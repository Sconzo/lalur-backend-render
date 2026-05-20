package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entidade JPA para Lançamento Contábil (partidas dobradas).
 *
 * <p>Implementa o método de partidas dobradas, onde cada lançamento possui uma conta de débito
 * e uma conta de crédito. O valor é sempre positivo e representa o montante debitado de uma
 * conta e creditado em outra.
 *
 * <p>Validações automáticas via @PrePersist e @PreUpdate:
 *
 * <ul>
 *   <li>Valor deve ser maior que zero
 *   <li>Conta de débito deve ser diferente da conta de crédito
 * </ul>
 *
 * <p>Observação: A validação de data do lançamento vs Período Contábil é realizada via
 * PeriodoContabilAspect baseado na interface TemporalEntity do domain model.
 */
@Entity
@Table(name = "tb_lancamento_contabil")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LancamentoContabilEntity extends BaseEntity {

  /**
   * Empresa dona do lançamento.
   *
   * <p>FK obrigatória - cada lançamento pertence a uma empresa específica.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", nullable = false)
  private CompanyEntity company;

  /**
   * Conta de débito (opcional — ao menos uma das contas deve ser informada).
   *
   * <p>Quando presente, deve ser uma conta da classe ANALITICO.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "conta_debito_id", nullable = true)
  private PlanoDeContasEntity contaDebito;

  /**
   * Conta de crédito (opcional — ao menos uma das contas deve ser informada).
   *
   * <p>Quando presente, deve ser uma conta da classe ANALITICO.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "conta_credito_id", nullable = true)
  private PlanoDeContasEntity contaCredito;

  /**
   * Data do lançamento contábil.
   *
   * <p>Representa tanto a data do lançamento quanto a data de competência para validação
   * de Período Contábil.
   */
  @Column(name = "data", nullable = false)
  private LocalDate data;

  /**
   * Valor do lançamento (sempre positivo).
   *
   * <p>Este valor será debitado na contaDebito e creditado na contaCredito.
   */
  @Column(name = "valor", nullable = false, precision = 19, scale = 2)
  private BigDecimal valor;

  /**
   * Histórico do lançamento.
   *
   * <p>Descrição detalhada do fato contábil.
   */
  @Column(name = "historico", nullable = false, length = 2000)
  private String historico;

  /**
   * Número do documento que originou o lançamento (opcional).
   *
   * <p>Exemplo: número da nota fiscal, número do cheque, etc.
   */
  @Column(name = "numero_documento", length = 100)
  private String numeroDocumento;

  /**
   * Ano fiscal do lançamento.
   *
   * <p>Deve corresponder ao ano fiscal das contas de débito e crédito para consistência.
   */
  @Column(name = "fiscal_year", nullable = false)
  private Integer fiscalYear;

  /**
   * Valida regras de negócio antes de persistir ou atualizar.
   *
   * <p>Validações:
   *
   * <ul>
   *   <li>Valor deve ser maior que zero
   *   <li>Ao menos uma conta (débito ou crédito) deve ser informada
   *   <li>Se ambas informadas, devem ser contas diferentes
   * </ul>
   *
   * @throws IllegalStateException se alguma validação falhar
   */
  @PrePersist
  @PreUpdate
  public void validateLancamentoContabil() {
    if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalStateException(
          "Valor do lançamento contábil deve ser maior que zero. Valor atual: " + valor);
    }

    if (contaDebito == null && contaCredito == null) {
      throw new IllegalStateException(
          "Ao menos uma conta (débito ou crédito) deve ser informada no lançamento contábil");
    }

    if (contaDebito != null && contaCredito != null
        && contaDebito.getId().equals(contaCredito.getId())) {
      throw new IllegalStateException(
          "Conta de débito e conta de crédito devem ser diferentes. "
              + "Conta informada: "
              + contaDebito.getId());
    }
  }
}
