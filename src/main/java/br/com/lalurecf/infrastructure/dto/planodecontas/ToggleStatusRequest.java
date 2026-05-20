package br.com.lalurecf.infrastructure.dto.planodecontas;

import br.com.lalurecf.domain.enums.Status;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para alternar status de conta contábil (PlanoDeContas).
 *
 * <p>Permite ativar (ACTIVE) ou inativar (INACTIVE) uma conta.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToggleStatusRequest {

  /** Novo status desejado (ACTIVE ou INACTIVE). */
  @NotNull(message = "Status is required")
  private Status status;
}
