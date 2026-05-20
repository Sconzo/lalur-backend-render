package br.com.lalurecf.infrastructure.dto.company;

import br.com.lalurecf.domain.model.CompanyStatus;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para alteração de status de empresa.
 */
public record ToggleStatusRequest(

    @NotNull(message = "Status é obrigatório")
    CompanyStatus status
) {
}
