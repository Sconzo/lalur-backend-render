package br.com.lalurecf.infrastructure.security;

import br.com.lalurecf.application.port.out.CompanyRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.Company;
import br.com.lalurecf.infrastructure.exception.ResourceNotFoundException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Servlet filter that extracts and validates the X-Company-Id header.
 * <p>
 * This filter processes the X-Company-Id header from incoming requests,
 * validates that the company exists and is ACTIVE, and stores it in the
 * {@link CompanyContext} for use throughout the request lifecycle.
 * </p>
 * <p>
 * <strong>Order:</strong> Executes after Spring Security filter
 * (HIGHEST_PRECEDENCE + 10) to ensure authentication is complete.
 * </p>
 * <p>
 * <strong>CRITICAL:</strong> Always clears the ThreadLocal in finally block
 * to prevent memory leaks.
 * </p>
 *
 * @see CompanyContext
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CompanyContextFilter implements Filter {

  private static final Logger log = LoggerFactory.getLogger(CompanyContextFilter.class);
  private static final String COMPANY_ID_HEADER = "X-Company-Id";

  private final CompanyRepositoryPort companyRepository;

  public CompanyContextFilter(CompanyRepositoryPort companyRepository) {
    this.companyRepository = companyRepository;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    try {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      String companyIdHeader = httpRequest.getHeader(COMPANY_ID_HEADER);

      if (companyIdHeader != null && !companyIdHeader.isBlank()) {
        try {
          Long companyId = Long.parseLong(companyIdHeader);

          // Validate company exists and is ACTIVE
          Company company = companyRepository.findById(companyId)
              .orElseThrow(() -> new ResourceNotFoundException(
                  "Empresa com ID " + companyId + " não encontrada"));

          if (!Status.ACTIVE.equals(company.getStatus())) {
            throw new ResourceNotFoundException(
                "Empresa com ID " + companyId + " está inativa");
          }

          // Store in context for this request
          CompanyContext.setCurrentCompanyId(companyId);
          log.debug("Company context set: companyId={}", companyId);

        } catch (NumberFormatException e) {
          log.warn("Invalid X-Company-Id header format: {}", companyIdHeader);
          httpResponse.sendError(
              HttpServletResponse.SC_BAD_REQUEST,
              "Header X-Company-Id deve ser um número válido"
          );
          return;
        } catch (ResourceNotFoundException e) {
          log.warn("Company validation failed: {}", e.getMessage());
          httpResponse.sendError(
              HttpServletResponse.SC_NOT_FOUND,
              e.getMessage()
          );
          return;
        } catch (Exception e) {
          log.error("Unexpected error loading company {}: {}", companyIdHeader, e.getMessage(), e);
          httpResponse.sendError(
              HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "Erro ao carregar empresa com ID " + companyIdHeader
          );
          return;
        }
      }

      // Continue filter chain
      chain.doFilter(request, response);

    } finally {
      // CRITICAL: Always clear ThreadLocal to prevent memory leaks
      CompanyContext.clear();
      log.trace("Company context cleared");
    }
  }
}
