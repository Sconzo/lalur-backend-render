package br.com.lalurecf.infrastructure.dto.user;

import br.com.lalurecf.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para requisição de criação de usuário.
 *
 * <p>Contém dados necessários para criar um novo usuário no sistema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

  @NotBlank(message = "Nome é obrigatório")
  @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
  private String firstName;

  @NotBlank(message = "Sobrenome é obrigatório")
  @Size(max = 100, message = "Sobrenome deve ter no máximo 100 caracteres")
  private String lastName;

  @NotBlank(message = "Email é obrigatório")
  @Email(message = "Email inválido")
  @Size(max = 255, message = "Email deve ter no máximo 255 caracteres")
  private String email;

  @NotBlank(message = "Senha é obrigatória")
  @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
  private String password;

  @NotNull(message = "Role é obrigatória")
  private UserRole role;
}
