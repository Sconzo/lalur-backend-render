package br.com.lalurecf.infrastructure.dto.company;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for company selection request.
 * <p>
 * Used when user explicitly selects a company to work with.
 * The company ID is validated to ensure it exists and is ACTIVE.
 * </p>
 */
public record SelectCompanyRequest(
    @NotNull(message = "Company ID é obrigatório")
    Long companyId
) {
}
