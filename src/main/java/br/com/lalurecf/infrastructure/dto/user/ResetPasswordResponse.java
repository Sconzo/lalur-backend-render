package br.com.lalurecf.infrastructure.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resposta de reset de senha.
 *
 * <p>Indica sucesso da operação de reset de senha.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta da operação de reset de senha")
public class ResetPasswordResponse {

  @Schema(description = "Indica se a operação foi bem-sucedida", example = "true")
  private Boolean success;

  @Schema(
      description = "Mensagem descritiva do resultado",
      example = "Senha redefinida. Usuário deve trocar no próximo login.")
  private String message;
}
