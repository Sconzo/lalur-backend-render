package br.com.lalurecf.infrastructure.dto.company;

/**
 * DTO for company selection response.
 * <p>
 * Confirms that a company has been selected and provides
 * basic information for UI feedback.
 * </p>
 */
public record SelectCompanyResponse(
    Boolean success,
    Long companyId,
    String companyName,
    String message
) {
}
