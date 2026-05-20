package br.com.lalurecf.application.port.in.company;

import br.com.lalurecf.domain.model.Company;

/**
 * Use case for selecting a company to work with.
 * <p>
 * This use case validates that the company exists and is ACTIVE
 * before allowing the user to select it.
 * </p>
 * <p>
 * Note: The actual company context (via X-Company-Id header) is managed
 * by {@link br.com.lalurecf.infrastructure.security.CompanyContextFilter}.
 * This use case is for the explicit selection endpoint only.
 * </p>
 */
public interface SelectCompanyUseCase {

  /**
   * Selects a company for the current user to work with.
   *
   * @param companyId the ID of the company to select
   * @return the selected company
   * @throws br.com.lalurecf.infrastructure.exception.ResourceNotFoundException
   *         if company does not exist or is INACTIVE
   */
  Company selectCompany(Long companyId);
}
