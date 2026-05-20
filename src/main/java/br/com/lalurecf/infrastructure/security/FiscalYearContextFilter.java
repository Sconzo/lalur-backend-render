package br.com.lalurecf.infrastructure.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Year;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Servlet filter that extracts and validates the X-Fiscal-Year header.
 *
 * <p>This filter processes the X-Fiscal-Year header from incoming requests,
 * validates that the year is between 2000 and the current year, and stores it
 * in the {@link FiscalYearContext} for use throughout the request lifecycle.
 *
 * <p><strong>Order:</strong> Executes after CompanyContextFilter
 * (HIGHEST_PRECEDENCE + 11).
 *
 * <p><strong>CRITICAL:</strong> Always clears the ThreadLocal in finally block
 * to prevent memory leaks.
 *
 * @see FiscalYearContext
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 11)
public class FiscalYearContextFilter implements Filter {

  private static final Logger log = LoggerFactory.getLogger(FiscalYearContextFilter.class);
  private static final String FISCAL_YEAR_HEADER = "X-Fiscal-Year";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    try {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      String fiscalYearHeader = httpRequest.getHeader(FISCAL_YEAR_HEADER);

      if (fiscalYearHeader != null && !fiscalYearHeader.isBlank()) {
        try {
          int fiscalYear = Integer.parseInt(fiscalYearHeader.trim());
          int currentYear = Year.now().getValue();

          if (fiscalYear < 2000 || fiscalYear > currentYear) {
            log.warn("Invalid X-Fiscal-Year value: {}", fiscalYear);
            httpResponse.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "X-Fiscal-Year deve estar entre 2000 e " + currentYear);
            return;
          }

          FiscalYearContext.setCurrentFiscalYear(fiscalYear);
          log.debug("Fiscal year context set: fiscalYear={}", fiscalYear);

        } catch (NumberFormatException e) {
          log.warn("Invalid X-Fiscal-Year header format: {}", fiscalYearHeader);
          httpResponse.sendError(
              HttpServletResponse.SC_BAD_REQUEST,
              "Header X-Fiscal-Year deve ser um número válido");
          return;
        }
      }

      chain.doFilter(request, response);

    } finally {
      FiscalYearContext.clear();
      log.trace("Fiscal year context cleared");
    }
  }
}
