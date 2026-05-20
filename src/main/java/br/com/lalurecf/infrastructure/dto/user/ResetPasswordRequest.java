package br.com.lalurecf.infrastructure.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para requisição de reset de senha.
 *
 * <p>Contém senha temporária que será definida pelo ADMIN.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requisição de reset de senha por ADMIN")
public class ResetPasswordRequest {

  @NotBlank(message = "Senha temporária é obrigatória")
  @Size(min = 8, message = "Senha temporária deve ter no mínimo 8 caracteres")
  @Schema(
      description = "Senha temporária definida pelo ADMIN (mínimo 8 caracteres)",
      example = "TempPass123",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String temporaryPassword;
}
