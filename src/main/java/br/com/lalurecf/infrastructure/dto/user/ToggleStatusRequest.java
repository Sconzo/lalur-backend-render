package br.com.lalurecf.infrastructure.dto.user;

import br.com.lalurecf.domain.enums.Status;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para requisição de alteração de status de usuário.
 *
 * <p>Permite alternar entre ACTIVE e INACTIVE.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToggleStatusRequest {

  @NotNull(message = "Status é obrigatório")
  private Status status;
}
