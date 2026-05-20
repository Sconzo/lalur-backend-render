package br.com.lalurecf.infrastructure.dto.contaparteb;

import br.com.lalurecf.domain.enums.TipoSaldo;
import br.com.lalurecf.domain.enums.TipoTributo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
 * DTO para requisição de criação de conta da Parte B (e-Lalur/e-Lacs).
 *
 * <p>Contém dados necessários para criar uma nova conta fiscal específica de IRPJ/CSLL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class CreateContaParteBRequest {

  @NotBlank(message = "Código da conta é obrigatório")
  @Size(max = 50, message = "Código da conta deve ter no máximo 50 caracteres")
  private String codigoConta;

  @NotBlank(message = "Descrição é obrigatória")
  @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
  private String descricao;

  @NotNull(message = "Ano base é obrigatório")
  @Min(value = 2000, message = "Ano base deve ser >= 2000")
  @Max(value = 2027, message = "Ano base deve ser <= ano atual + 1")
  private Integer anoBase;

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
