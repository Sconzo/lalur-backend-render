package br.com.lalurecf.infrastructure.dto.company;

import br.com.lalurecf.domain.model.CompanyStatus;

/**
 * DTO para resposta de alteração de status.
 */
public record ToggleStatusResponse(
    boolean success,
    String message,
    CompanyStatus newStatus
) {
}
