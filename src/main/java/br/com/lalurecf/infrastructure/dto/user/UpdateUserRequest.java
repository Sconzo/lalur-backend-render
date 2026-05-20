package br.com.lalurecf.infrastructure.dto.user;

import br.com.lalurecf.domain.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para requisição de atualização de usuário.
 *
 * <p>Permite atualizar nome e role do usuário. Email e senha não podem ser alterados.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

  @NotBlank(message = "Nome é obrigatório")
  @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
  private String firstName;

  @NotBlank(message = "Sobrenome é obrigatório")
  @Size(max = 100, message = "Sobrenome deve ter no máximo 100 caracteres")
  private String lastName;

  @NotNull(message = "Role é obrigatória")
  private UserRole role;
}
