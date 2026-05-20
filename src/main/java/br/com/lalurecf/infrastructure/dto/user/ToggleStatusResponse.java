package br.com.lalurecf.infrastructure.dto.user;

import br.com.lalurecf.domain.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resposta de alteração de status.
 *
 * <p>Indica sucesso da operação e o novo status do usuário.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToggleStatusResponse {

  private Boolean success;
  private String message;
  private Status newStatus;
}
