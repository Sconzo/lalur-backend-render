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
 * DTO para requisição de atualização de conta referencial RFB.
 *
 * <p>Permite editar descrição e ano de validade. Código RFB não pode ser alterado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateContaReferencialRequest {

  @NotBlank(message = "Descrição é obrigatória")
  @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
  private String descricao;

  @Min(value = 2000, message = "Ano de validade deve ser >= 2000")
  @Max(value = 2031, message = "Ano de validade deve ser <= ano atual + 5")
  private Integer anoValidade;
}
