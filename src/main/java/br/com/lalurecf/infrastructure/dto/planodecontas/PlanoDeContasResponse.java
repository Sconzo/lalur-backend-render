package br.com.lalurecf.infrastructure.dto.planodecontas;

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
 * DTO de resposta para conta contábil (PlanoDeContas).
 *
 * <p>Inclui código da Conta Referencial RFB para exibição na UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoDeContasResponse {

  /** ID da conta. */
  private Long id;

  /** Código da conta contábil (ex: "1.1.01.001"). */
  private String code;

  /** Nome da conta contábil (ex: "Caixa"). */
  private String name;

  /** Ano fiscal da conta (ex: 2024). */
  private Integer fiscalYear;

  /** Tipo da conta (ATIVO, PASSIVO, RECEITA, etc.). */
  private AccountType accountType;

  /** ID da Conta Referencial RFB. */
  private Long contaReferencialId;

  /** Código da Conta Referencial RFB (ex: "1.01.01"). */
  private String contaReferencialCodigo;

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

  /** Timestamp de criação. */
  private LocalDateTime createdAt;

  /** Timestamp de última atualização. */
  private LocalDateTime updatedAt;
}
