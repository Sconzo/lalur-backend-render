package br.com.lalurecf.infrastructure.dto.planodecontas;

import br.com.lalurecf.domain.enums.AccountType;
import br.com.lalurecf.domain.enums.ClasseContabil;
import br.com.lalurecf.domain.enums.NaturezaConta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para criação de conta contábil (PlanoDeContas).
 *
 * <p>Contém todos os campos ECF-specific necessários para cadastro de conta no plano de contas,
 * incluindo vinculação obrigatória a uma Conta Referencial RFB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlanoDeContasRequest {

  /** Código da conta contábil (ex: "1.1.01.001"). */
  @NotBlank(message = "Code is required")
  private String code;

  /** Nome da conta contábil (ex: "Caixa"). */
  @NotBlank(message = "Name is required")
  private String name;

  /** Tipo da conta (ATIVO, PASSIVO, RECEITA, etc.). */
  @NotNull(message = "Account type is required")
  private AccountType accountType;

  /** ID da Conta Referencial RFB (opcional). */
  private Long contaReferencialId;

  /** Classe contábil ECF (ATIVO_CIRCULANTE, RECEITA_BRUTA, etc.). */
  @NotNull(message = "Classe is required")
  private ClasseContabil classe;

  /** Natureza da conta (DEVEDORA ou CREDORA). */
  @NotNull(message = "Natureza is required")
  private NaturezaConta natureza;

  /** Indica se a conta afeta o resultado (DRE). */
  @NotNull(message = "AfetaResultado is required")
  private Boolean afetaResultado;

  /** Indica se despesa/custo é dedutível fiscalmente. */
  @NotNull(message = "Dedutivel is required")
  private Boolean dedutivel;
}
