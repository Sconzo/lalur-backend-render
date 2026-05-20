package br.com.lalurecf.infrastructure.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para requisição de troca de senha.
 *
 * <p>Contém email, senha temporária e nova senha com validações.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

  @NotBlank(message = "Email é obrigatório")
  @Email(message = "Email deve ser válido")
  private String email;

  @NotBlank(message = "Senha atual é obrigatória")
  private String currentPassword;

  @NotBlank(message = "Nova senha é obrigatória")
  @Size(min = 8, message = "Nova senha deve ter no mínimo 8 caracteres")
  private String newPassword;
}
