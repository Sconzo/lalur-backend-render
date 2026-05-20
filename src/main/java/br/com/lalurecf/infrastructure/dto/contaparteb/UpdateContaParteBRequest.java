package br.com.lalurecf.infrastructure.dto.contaparteb;

import br.com.lalurecf.domain.enums.TipoSaldo;
import br.com.lalurecf.domain.enums.TipoTributo;
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
 * DTO para requisição de atualização de conta da Parte B (e-Lalur/e-Lacs).
 *
 * <p>Permite editar campos mutáveis. Campos imutáveis (codigoConta, anoBase) não estão presentes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class UpdateContaParteBRequest {

  @NotBlank(message = "Descrição é obrigatória")
  @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
  private String descricao;

  @NotNull(message = "Data de início de vigência é obrigatória")
  private LocalDate dataVigenciaInicio;

  private LocalDate dataVigenciaFim;

  @NotNull(message = "Tipo de tributo é obrigatório")
  private TipoTributo tipoTributo;

  @NotNull(message = "Saldo inicial é obrigatório")
  @DecimalMin(value = "0.0", message = "Saldo inicial deve ser >= 0")
  private BigDecimal saldoInicial;

  @NotNull(message = "Tipo de saldo é obrigatório")
  private TipoSaldo tipoSaldo;
}
