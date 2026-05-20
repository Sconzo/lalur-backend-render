package br.com.lalurecf.infrastructure.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para requisição de login.
 *
 * <p>Contém credenciais do usuário (email e senha) validadas via Bean Validation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

  @NotBlank(message = "Email é obrigatório")
  @Email(message = "Email deve ser válido")
  private String email;

  @NotBlank(message = "Senha é obrigatória")
  private String password;
}
