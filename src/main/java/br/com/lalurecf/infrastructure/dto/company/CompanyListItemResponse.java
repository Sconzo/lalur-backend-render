package br.com.lalurecf.infrastructure.dto.company;

/**
 * DTO for company list item in dropdown (simplified view).
 * <p>
 * Used by endpoints that return company lists for selection purposes,
 * such as the my-companies endpoint.
 * </p>
 */
public record CompanyListItemResponse(
    Long id,
    String cnpj,
    String razaoSocial
) {
}
