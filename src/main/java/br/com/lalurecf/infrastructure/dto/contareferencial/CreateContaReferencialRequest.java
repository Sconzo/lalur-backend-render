package br.com.lalurecf.infrastructure.dto.contareferencial;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para requisição de criação de conta referencial RFB.
 *
 * <p>Contém dados necessários para criar uma nova conta na tabela mestra de contas oficiais da
 * Receita Federal Brasil.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateContaReferencialRequest {

  @NotBlank(message = "Código RFB é obrigatório")
  @Size(max = 50, message = "Código RFB deve ter no máximo 50 caracteres")
  private String codigoRfb;

  @NotBlank(message = "Descrição é obrigatória")
  @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
  private String descricao;

  @Min(value = 2000, message = "Ano de validade deve ser >= 2000")
  @Max(value = 2031, message = "Ano de validade deve ser <= ano atual + 5")
  private Integer anoValidade;
}
