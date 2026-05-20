package br.com.lalurecf.infrastructure.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lalurecf.domain.enums.UserRole;
import br.com.lalurecf.infrastructure.security.JwtTokenProvider;
import br.com.lalurecf.util.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Testes de integração para segurança Spring Security com JWT. */
@DisplayName("Security - Testes de Integração")
@AutoConfigureMockMvc
class SecurityIntegrationTest extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtTokenProvider jwtTokenProvider;

  @Test
  @DisplayName("Deve retornar 401 ao acessar endpoint protegido sem token")
  void shouldReturn401WithoutToken() throws Exception {
    mockMvc.perform(get("/test/protected")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Deve retornar 200 ao acessar endpoint protegido com token válido")
  void shouldReturn200WithValidToken() throws Exception {
    // Arrange
    String token = jwtTokenProvider.generateAccessToken("test@example.com", UserRole.CONTADOR);

    // Act & Assert
    mockMvc
        .perform(get("/test/protected").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Deve retornar 401 ao acessar endpoint protegido com token inválido")
  void shouldReturn401WithInvalidToken() throws Exception {
    mockMvc
        .perform(get("/test/protected").header("Authorization", "Bearer invalid_token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Deve retornar 403 ao acessar endpoint admin sem role ADMIN")
  void shouldReturn403ForAdminEndpointWithoutAdminRole() throws Exception {
    // Arrange
    String token = jwtTokenProvider.generateAccessToken("test@example.com", UserRole.CONTADOR);

    // Act & Assert
    mockMvc
        .perform(get("/test/admin").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Deve retornar 200 ao acessar endpoint admin com role ADMIN")
  void shouldReturn200ForAdminEndpointWithAdminRole() throws Exception {
    // Arrange
    String token = jwtTokenProvider.generateAccessToken("admin@example.com", UserRole.ADMIN);

    // Act & Assert
    mockMvc
        .perform(get("/test/admin").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Deve permitir acesso ao health check sem autenticação")
  void shouldAllowHealthCheckWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
