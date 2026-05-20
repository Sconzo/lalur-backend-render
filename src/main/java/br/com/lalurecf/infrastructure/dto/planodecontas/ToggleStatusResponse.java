package br.com.lalurecf.infrastructure.dto.planodecontas;

import br.com.lalurecf.domain.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta para toggle de status de conta contábil.
 *
 * <p>Confirma sucesso da operação e retorna novo status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToggleStatusResponse {

  /** Indica se a operação foi bem-sucedida. */
  private Boolean success;

  /** Mensagem descritiva do resultado. */
  private String message;

  /** Novo status da conta após operação. */
  private Status newStatus;
}
