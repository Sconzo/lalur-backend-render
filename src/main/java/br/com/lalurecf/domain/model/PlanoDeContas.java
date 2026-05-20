package br.com.lalurecf.domain.model;

import br.com.lalurecf.domain.enums.AccountType;
import br.com.lalurecf.domain.enums.ClasseContabil;
import br.com.lalurecf.domain.enums.NaturezaConta;
import br.com.lalurecf.domain.enums.Status;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model para Plano de Contas Contábil.
 *
 * <p>Representa uma conta contábil de uma empresa para um ano fiscal específico, vinculada
 * obrigatoriamente a uma Conta Referencial RFB oficial. Inclui campos ECF-specific para compliance
 * com layout oficial da Escrituração Contábil Fiscal.
 *
 * <p>POJO puro sem dependências de frameworks (hexagonal architecture).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoDeContas {

  /** ID da conta. */
  private Long id;

  /** ID da empresa dona da conta. */
  private Long companyId;

  /** ID da Conta Referencial RFB (FK obrigatória). */
  private Long contaReferencialId;

  /** Código da conta contábil (ex: "1.1.01.001"). */
  private String code;

  /** Nome da conta contábil (ex: "Caixa"). */
  private String name;

  /** Ano fiscal da conta (ex: 2024). */
  private Integer fiscalYear;

  /** Tipo da conta (ATIVO, PASSIVO, RECEITA, etc.). */
  private AccountType accountType;

  /** Classe contábil ECF (ATIVO_CIRCULANTE, RECEITA_BRUTA, etc.). */
  private ClasseContabil classe;

  /** Nível hierárquico (1-5) para estruturação ECF. */
  private Integer nivel;

  /** Natureza da conta (DEVEDORA ou CREDORA). */
  private NaturezaConta natureza;

  /** Indica se a conta afeta o resultado (DRE). */
  private Boolean afetaResultado;

  /** Indica se despesa/custo é dedutível fiscalmente. */
  private Boolean dedutivel;

  /** Status da conta (ACTIVE/INACTIVE). */
  private Status status;

  /** Timestamp de criação (auditoria). */
  private LocalDateTime createdAt;

  /** Timestamp de última atualização (auditoria). */
  private LocalDateTime updatedAt;

  /** ID do usuário que criou (auditoria). */
  private Long createdBy;

  /** ID do usuário que atualizou (auditoria). */
  private Long updatedBy;
}
