package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import br.com.lalurecf.domain.enums.TipoSaldo;
import br.com.lalurecf.domain.enums.TipoTributo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entidade JPA para Conta da Parte B (e-Lalur/e-Lacs).
 *
 * <p>Representa contas fiscais específicas de IRPJ/CSLL, separadas do plano de contas contábil.
 * Vinculada a uma empresa específica.
 *
 * <p>Constraint único: (company_id, codigo_conta, ano_base) evita duplicação de código por empresa
 * e ano.
 */
@Entity
@Table(
    name = "tb_conta_parte_b",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_conta_parte_b_company_codigo_ano",
            columnNames = {"company_id", "codigo_conta", "ano_base"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class ContaParteBEntity extends BaseEntity {

  /** Empresa dona do cadastro. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", nullable = false)
  private CompanyEntity company;

  /** Código da conta Parte B (ex: "4.01.01"). */
  @Column(name = "codigo_conta", nullable = false, length = 50)
  private String codigoConta;

  /** Descrição da conta. */
  @Column(name = "descricao", nullable = false, length = 1000)
  private String descricao;

  /** Ano base de criação/referência da conta. */
  @Column(name = "ano_base", nullable = false)
  private Integer anoBase;

  /** Data de início de vigência da conta. */
  @Column(name = "data_vigencia_inicio", nullable = false)
  private LocalDate dataVigenciaInicio;

  /**
   * Data de fim de vigência da conta (nullable).
   *
   * <p>null = conta ainda vigente.
   */
  @Column(name = "data_vigencia_fim")
  private LocalDate dataVigenciaFim;

  /** Tipo de tributo (IRPJ, CSLL ou AMBOS). */
  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_tributo", nullable = false, length = 20)
  private TipoTributo tipoTributo;

  /** Saldo inicial da conta. */
  @Column(name = "saldo_inicial", precision = 19, scale = 2)
  private BigDecimal saldoInicial;

  /** Tipo de saldo (DEVEDOR ou CREDOR). */
  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_saldo", length = 20)
  private TipoSaldo tipoSaldo;
}
