package br.com.lalurecf.infrastructure.dto.lancamentocontabil;

import br.com.lalurecf.infrastructure.validation.LancamentoContabilValidator;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para criação de lançamento contábil.
 *
 * <p>Ao menos uma das contas (débito ou crédito) deve ser informada.
 * Se ambas forem informadas, devem ser contas distintas e da classe ANALITICO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@LancamentoContabilValidator
public class CreateLancamentoContabilRequest {

  private Long contaDebitoId;

  private Long contaCreditoId;

  @NotNull(message = "Data é obrigatória")
  private LocalDate data;

  @NotNull(message = "Valor é obrigatório")
  @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
  private BigDecimal valor;

  @NotBlank(message = "Histórico é obrigatório")
  @Size(max = 2000, message = "Histórico deve ter no máximo 2000 caracteres")
  private String historico;

  @Size(max = 100, message = "Número do documento deve ter no máximo 100 caracteres")
  private String numeroDocumento;
}
