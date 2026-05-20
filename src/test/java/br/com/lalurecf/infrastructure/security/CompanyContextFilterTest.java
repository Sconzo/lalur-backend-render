package br.com.lalurecf.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import br.com.lalurecf.application.port.out.CompanyRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.Company;
import br.com.lalurecf.domain.model.valueobject.CNPJ;
import br.com.lalurecf.infrastructure.exception.ResourceNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyContextFilter Unit Tests")
class CompanyContextFilterTest {

  @Mock
  private CompanyRepositoryPort companyRepository;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  private CompanyContextFilter filter;

  @BeforeEach
  void setUp() {
    filter = new CompanyContextFilter(companyRepository);
  }

  @AfterEach
  void tearDown() {
    // Ensure context is always cleared
    CompanyContext.clear();
  }

  @Test
  @DisplayName("Should set company context when valid header is present")
  void shouldSetCompanyContextWhenValidHeader() throws Exception {
    // Arrange
    Long companyId = 123L;
    Company company = createActiveCompany(companyId);

    when(request.getHeader("X-Company-Id")).thenReturn("123");
    when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

    // Act
    filter.doFilter(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
    verify(companyRepository).findById(companyId);
    // Context should be cleared in finally block
    assertNull(CompanyContext.getCurrentCompanyId());
  }

  @Test
  @DisplayName("Should continue filter chain when no header is present")
  void shouldContinueWhenNoHeader() throws Exception {
    // Arrange
    when(request.getHeader("X-Company-Id")).thenReturn(null);

    // Act
    filter.doFilter(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
    verify(companyRepository, never()).findById(anyLong());
    assertNull(CompanyContext.getCurrentCompanyId());
  }

  @Test
  @DisplayName("Should return 400 when header is not a valid number")
  void shouldReturn400WhenHeaderInvalid() throws Exception {
    // Arrange
    when(request.getHeader("X-Company-Id")).thenReturn("invalid");

    // Act
    filter.doFilter(request, response, filterChain);

    // Assert
    verify(response).sendError(
        eq(HttpServletResponse.SC_BAD_REQUEST),
        eq("Header X-Company-Id deve ser um número válido")
    );
    verify(filterChain, never()).doFilter(any(), any());
    assertNull(CompanyContext.getCurrentCompanyId());
  }

  @Test
  @DisplayName("Should return 404 when company does not exist")
  void shouldReturn404WhenCompanyNotFound() throws Exception {
    // Arrange
    when(request.getHeader("X-Company-Id")).thenReturn("999");
    when(companyRepository.findById(999L)).thenReturn(Optional.empty());

    // Act
    filter.doFilter(request, response, filterChain);

    // Assert
    verify(response).sendError(
        eq(HttpServletResponse.SC_NOT_FOUND),
        contains("Empresa com ID 999 não encontrada")
    );
    verify(filterChain, never()).doFilter(any(), any());
    assertNull(CompanyContext.getCurrentCompanyId());
  }

  @Test
  @DisplayName("Should return 404 when company is INACTIVE")
  void shouldReturn404WhenCompanyInactive() throws Exception {
    // Arrange
    Long companyId = 123L;
    Company company = createInactiveCompany(companyId);

    when(request.getHeader("X-Company-Id")).thenReturn("123");
    when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

    // Act
    filter.doFilter(request, response, filterChain);

    // Assert
    verify(response).sendError(
        eq(HttpServletResponse.SC_NOT_FOUND),
        contains("Empresa com ID 123 está inativa")
    );
    verify(filterChain, never()).doFilter(any(), any());
    assertNull(CompanyContext.getCurrentCompanyId());
  }

  @Test
  @DisplayName("Should clear context even when exception occurs")
  void shouldClearContextOnException() throws Exception {
    // Arrange
    when(request.getHeader("X-Company-Id")).thenReturn("123");
    when(companyRepository.findById(123L))
        .thenThrow(new RuntimeException("Database error"));

    // Act & Assert
    assertThrows(RuntimeException.class, () ->
        filter.doFilter(request, response, filterChain)
    );

    // Context should still be cleared
    assertNull(CompanyContext.getCurrentCompanyId());
  }

  // Helper methods

  private Company createActiveCompany(Long id) {
    Company company = new Company();
    company.setId(id);
    company.setCnpj(CNPJ.of("12345678901234"));
    company.setRazaoSocial("Test Company");
    company.setPeriodoContabil(LocalDate.of(2024, 1, 1));
    company.setStatus(Status.ACTIVE);
    return company;
  }

  private Company createInactiveCompany(Long id) {
    Company company = createActiveCompany(id);
    company.setStatus(Status.INACTIVE);
    return company;
  }
}
