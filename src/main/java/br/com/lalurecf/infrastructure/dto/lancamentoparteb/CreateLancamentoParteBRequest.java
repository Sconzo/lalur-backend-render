package br.com.lalurecf.infrastructure.dto.lancamentoparteb;

import br.com.lalurecf.domain.enums.TipoAjuste;
import br.com.lalurecf.domain.enums.TipoApuracao;
import br.com.lalurecf.domain.enums.TipoRelacionamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para criação de Lançamento da Parte B.
 *
 * <p>Representa request de criação de ajuste fiscal (adição/exclusão) IRPJ/CSLL com validações de
 * negócio via Bean Validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class CreateLancamentoParteBRequest {

  @NotNull(message = "Mês de referência é obrigatório")
  @Min(value = 1, message = "Mês de referência deve ser entre 1 e 12")
  @Max(value = 12, message = "Mês de referência deve ser entre 1 e 12")
  private Integer mesReferencia;

  @NotNull(message = "Tipo de apuração é obrigatório")
  private TipoApuracao tipoApuracao;

  @NotNull(message = "Tipo de relacionamento é obrigatório")
  private TipoRelacionamento tipoRelacionamento;

  private Long contaContabilId;

  private Long contaParteBId;

  @NotNull(message = "Parâmetro tributário é obrigatório")
  private Long parametroTributarioId;

  @NotNull(message = "Tipo de ajuste é obrigatório")
  private TipoAjuste tipoAjuste;

  @NotBlank(message = "Descrição é obrigatória")
  @Size(max = 2000, message = "Descrição deve ter no máximo 2000 caracteres")
  private String descricao;

  @NotNull(message = "Valor é obrigatório")
  @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
  private BigDecimal valor;
}
