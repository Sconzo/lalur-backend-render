package br.com.lalurecf.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.lalurecf.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Testes unitários para JwtTokenProvider. */
@DisplayName("JwtTokenProvider - Testes Unitários")
class JwtTokenProviderTest {

  private JwtTokenProvider jwtTokenProvider;

  @BeforeEach
  void setUp() {
    jwtTokenProvider = new JwtTokenProvider();
    ReflectionTestUtils.setField(
        jwtTokenProvider, "secret", "test-secret-key-minimum-256-bits-required-for-hmac256");
    ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", 604800000L); // 7 days
    ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", 604800000L); // 7 days (mantido por compatibilidade)
  }

  @Test
  @DisplayName("Deve gerar access token válido com claims corretos")
  void shouldGenerateValidAccessToken() {
    // Arrange
    String email = "test@example.com";
    UserRole role = UserRole.ADMIN;

    // Act
    String token = jwtTokenProvider.generateAccessToken(email, role);

    // Assert
    assertThat(token).isNotNull();
    assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo(email);
    assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo(role);
  }

  @Test
  @DisplayName("Deve gerar refresh token válido")
  void shouldGenerateValidRefreshToken() {
    // Arrange
    String email = "test@example.com";

    // Act
    String token = jwtTokenProvider.generateRefreshToken(email);

    // Assert
    assertThat(token).isNotNull();
    assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo(email);
  }

  @Test
  @DisplayName("Deve rejeitar token expirado")
  void shouldRejectExpiredToken() {
    // Arrange
    JwtTokenProvider expiredProvider = new JwtTokenProvider();
    ReflectionTestUtils.setField(
        expiredProvider, "secret", "test-secret-key-minimum-256-bits-required-for-hmac256");
    ReflectionTestUtils.setField(expiredProvider, "accessTokenExpiration", -1000L); // já expirado
    ReflectionTestUtils.setField(expiredProvider, "refreshTokenExpiration", 604800000L);

    String email = "test@example.com";
    String expiredToken = expiredProvider.generateAccessToken(email, UserRole.CONTADOR);

    // Act & Assert
    assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
  }

  @Test
  @DisplayName("Deve rejeitar token com assinatura inválida")
  void shouldRejectInvalidSignature() {
    // Arrange
    String email = "test@example.com";
    String token = jwtTokenProvider.generateAccessToken(email, UserRole.ADMIN);

    // Modificar secret para simular assinatura inválida
    ReflectionTestUtils.setField(jwtTokenProvider, "secret", "different-secret-key");

    // Act & Assert
    assertThat(jwtTokenProvider.validateToken(token)).isFalse();
  }

  @Test
  @DisplayName("Deve extrair email e role corretamente de access token")
  void shouldExtractEmailAndRoleFromToken() {
    // Arrange
    String email = "contador@example.com";
    UserRole role = UserRole.CONTADOR;
    String token = jwtTokenProvider.generateAccessToken(email, role);

    // Act
    String extractedEmail = jwtTokenProvider.getEmailFromToken(token);
    UserRole extractedRole = jwtTokenProvider.getRoleFromToken(token);

    // Assert
    assertThat(extractedEmail).isEqualTo(email);
    assertThat(extractedRole).isEqualTo(role);
  }
}
