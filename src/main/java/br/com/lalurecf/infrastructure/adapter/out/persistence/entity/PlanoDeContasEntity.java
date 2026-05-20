package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import br.com.lalurecf.domain.enums.AccountType;
import br.com.lalurecf.domain.enums.ClasseContabil;
import br.com.lalurecf.domain.enums.NaturezaConta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Entidade JPA para Plano de Contas Contábil.
 *
 * <p>Armazena contas contábeis de cada empresa por ano fiscal, com estrutura plana vinculada à
 * tabela mestra de Contas Referenciais RFB.
 *
 * <p>Inclui campos ECF-specific para compliance com layout oficial da ECF (Escrituração Contábil
 * Fiscal).
 *
 * <p>Constraint único: (company_id, code, fiscal_year) garante que não existam contas duplicadas
 * para mesma empresa + código + ano.
 */
@Entity
@Table(
    name = "tb_plano_de_contas",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_plano_de_contas_company_code_year",
            columnNames = {"company_id", "code", "fiscal_year"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoDeContasEntity extends BaseEntity {

  /**
   * Empresa dona da conta.
   *
   * <p>FK obrigatória - cada conta pertence a uma empresa específica.
   */
  @ManyToOne
  @JoinColumn(name = "company_id", nullable = false)
  private CompanyEntity company;

  /**
   * Conta Referencial RFB oficial.
   *
   * <p>FK opcional - conta contábil pode ser vinculada a uma conta da tabela mestra RFB
   * para compliance ECF.
   */
  @ManyToOne
  @JoinColumn(name = "conta_referencial_id", nullable = true)
  private ContaReferencialEntity contaReferencial;

  /**
   * Código da conta contábil.
   *
   * <p>Exemplo: "1.1.01.001", "3.1.01.002".
   */
  @Column(name = "code", nullable = false, length = 50)
  private String code;

  /**
   * Nome da conta contábil.
   *
   * <p>Exemplo: "Caixa", "Bancos Conta Movimento", "Receita de Vendas".
   */
  @Column(name = "name", nullable = false, length = 500)
  private String name;

  /**
   * Ano fiscal da conta.
   *
   * <p>Permite versionamento anual do plano de contas.
   *
   * <p>Exemplo: 2024.
   */
  @Column(name = "fiscal_year", nullable = false)
  private Integer fiscalYear;

  /**
   * Tipo da conta.
   *
   * <p>Classificação fundamental: ATIVO, PASSIVO, PATRIMONIO_LIQUIDO, RECEITA, DESPESA, CUSTO,
   * RESULTADO, COMPENSACAO, ATIVO_RETIFICADORA, PASSIVO_RETIFICADORA.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "account_type", nullable = false, length = 50)
  private AccountType accountType;

  /**
   * Classe contábil ECF.
   *
   * <p>Classificação detalhada conforme layout ECF: ATIVO_CIRCULANTE, ATIVO_NAO_CIRCULANTE,
   * PASSIVO_CIRCULANTE, PASSIVO_NAO_CIRCULANTE, PATRIMONIO_LIQUIDO, RECEITA_BRUTA,
   * DEDUCOES_RECEITA, CUSTOS, DESPESAS_OPERACIONAIS, OUTRAS_RECEITAS, OUTRAS_DESPESAS,
   * RESULTADO_FINANCEIRO.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "classe", nullable = false, length = 50)
  private ClasseContabil classe;

  /**
   * Nível hierárquico da conta (1-5).
   *
   * <p>Usado para estruturação ECF.
   *
   * <p>Exemplo: 1 = sintética raiz, 5 = analítica mais detalhada.
   */
  @Column(name = "nivel", nullable = false)
  private Integer nivel;

  /**
   * Natureza da conta.
   *
   * <p>DEVEDORA: débito aumenta saldo.
   *
   * <p>CREDORA: crédito aumenta saldo.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "natureza", nullable = false, length = 20)
  private NaturezaConta natureza;

  /**
   * Indica se a conta afeta o resultado (DRE).
   *
   * <p>true: conta de receita, despesa ou custo que impacta o resultado do exercício.
   *
   * <p>false: conta patrimonial que não afeta DRE.
   */
  @Column(name = "afeta_resultado", nullable = false)
  private Boolean afetaResultado;

  /**
   * Indica se despesa/custo é dedutível fiscalmente.
   *
   * <p>Relevante para apuração de IRPJ/CSLL.
   *
   * <p>true: despesa dedutível (ex: salários).
   *
   * <p>false: despesa indedutível (ex: multas).
   */
  @Column(name = "dedutivel", nullable = false)
  private Boolean dedutivel;
}
