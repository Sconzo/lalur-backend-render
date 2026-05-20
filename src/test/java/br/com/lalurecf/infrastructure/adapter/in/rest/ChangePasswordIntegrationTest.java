package br.com.lalurecf.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.lalurecf.application.port.out.UserRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.UserRole;
import br.com.lalurecf.domain.model.User;
import br.com.lalurecf.infrastructure.dto.auth.ChangePasswordRequest;
import br.com.lalurecf.infrastructure.dto.auth.ChangePasswordResponse;
import br.com.lalurecf.infrastructure.dto.auth.LoginRequest;
import br.com.lalurecf.infrastructure.dto.auth.LoginResponse;
import br.com.lalurecf.infrastructure.exception.ErrorResponse;
import br.com.lalurecf.util.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Testes de integração para endpoint de troca de senha. */
@DisplayName("Change Password Integration Tests")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChangePasswordIntegrationTest extends IntegrationTestBase {

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private UserRepositoryPort userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private String accessToken;
  private String testEmail;

  @BeforeEach
  void setup() {
    // Usa timestamp para email único em cada teste
    testEmail = "changepass" + System.currentTimeMillis() + "@test.com";

    // Seed usuário de teste
    User user =
        User.builder()
            .email(testEmail)
            .password(passwordEncoder.encode("oldPassword123"))
            .firstName("Change")
            .lastName("Password")
            .role(UserRole.ADMIN)
            .mustChangePassword(true)
            .status(Status.ACTIVE)
            .build();
    userRepository.save(user);

    // Faz login para obter token
    LoginRequest loginRequest = new LoginRequest(testEmail, "oldPassword123");
    ResponseEntity<LoginResponse> loginResponse =
        restTemplate.postForEntity("/api/v1/auth/login", loginRequest, LoginResponse.class);

    // Verifica se login foi bem-sucedido
    if (loginResponse.getBody() == null) {
      throw new IllegalStateException("Login failed with status " + loginResponse.getStatusCode());
    }

    accessToken = loginResponse.getBody().getAccessToken();
  }

  @Test
  @DisplayName("Deve trocar senha com sucesso e mudar mustChangePassword para false")
  void shouldChangePasswordSuccessfullyAndUpdateMustChangePasswordFlag() {
    // Arrange
    ChangePasswordRequest request =
        new ChangePasswordRequest(testEmail, "oldPassword123", "newPassword123");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    HttpEntity<ChangePasswordRequest> entity = new HttpEntity<>(request, headers);

    // Act
    ResponseEntity<ChangePasswordResponse> response =
        restTemplate.exchange(
            "/api/v1/auth/change-password", HttpMethod.POST, entity, ChangePasswordResponse.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getSuccess()).isTrue();
    assertThat(response.getBody().getMessage()).isEqualTo("Senha alterada com sucesso");

    // Verifica que mustChangePassword foi atualizado
    User updatedUser = userRepository.findByEmail(testEmail).orElseThrow();
    assertThat(updatedUser.getMustChangePassword()).isFalse();

    // Verifica que pode fazer login com nova senha
    LoginRequest newLoginRequest = new LoginRequest(testEmail, "newPassword123");
    ResponseEntity<LoginResponse> newLoginResponse =
        restTemplate.postForEntity("/api/v1/auth/login", newLoginRequest, LoginResponse.class);
    assertThat(newLoginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName("Deve retornar 400 quando senha atual está incorreta")
  void shouldReturn400WhenCurrentPasswordIsIncorrect() {
    // Arrange
    ChangePasswordRequest request =
        new ChangePasswordRequest(testEmail, "wrongPassword", "newPassword123");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    HttpEntity<ChangePasswordRequest> entity = new HttpEntity<>(request, headers);

    // Act
    ResponseEntity<ErrorResponse> response =
        restTemplate.exchange(
            "/api/v1/auth/change-password", HttpMethod.POST, entity, ErrorResponse.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("Senha atual inválida");
    assertThat(response.getBody().getError()).isEqualTo("Bad Request");
  }

  @Test
  @DisplayName("Deve retornar 400 quando nova senha tem menos de 8 caracteres")
  void shouldReturn400WhenNewPasswordIsTooShort() {
    // Arrange
    ChangePasswordRequest request =
        new ChangePasswordRequest(testEmail, "oldPassword123", "short");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    HttpEntity<ChangePasswordRequest> entity = new HttpEntity<>(request, headers);

    // Act
    ResponseEntity<ErrorResponse> response =
        restTemplate.exchange(
            "/api/v1/auth/change-password", HttpMethod.POST, entity, ErrorResponse.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("Validation Error");
    assertThat(response.getBody().getValidationErrors()).containsKey("newPassword");
    assertThat(response.getBody().getValidationErrors().get("newPassword"))
        .isEqualTo("Nova senha deve ter no mínimo 8 caracteres");
  }

  @Test
  @DisplayName("Deve retornar 400 quando nova senha é igual à atual")
  void shouldReturn400WhenNewPasswordIsSameAsCurrent() {
    // Arrange
    ChangePasswordRequest request =
        new ChangePasswordRequest(testEmail, "oldPassword123", "oldPassword123");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    HttpEntity<ChangePasswordRequest> entity = new HttpEntity<>(request, headers);

    // Act
    ResponseEntity<ErrorResponse> response =
        restTemplate.exchange(
            "/api/v1/auth/change-password", HttpMethod.POST, entity, ErrorResponse.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("Nova senha não pode ser igual à atual");
    assertThat(response.getBody().getError()).isEqualTo("Bad Request");
  }

  @Test
  @DisplayName("Deve retornar 400 quando campos estão vazios")
  void shouldReturn400WhenFieldsAreEmpty() {
    // Arrange
    ChangePasswordRequest request =
        new ChangePasswordRequest("", "", "");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    HttpEntity<ChangePasswordRequest> entity = new HttpEntity<>(request, headers);

    // Act
    ResponseEntity<ErrorResponse> response =
        restTemplate.exchange(
            "/api/v1/auth/change-password", HttpMethod.POST, entity, ErrorResponse.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("Validation Error");
    assertThat(response.getBody().getValidationErrors()).containsKey("currentPassword");
    assertThat(response.getBody().getValidationErrors()).containsKey("newPassword");
  }

  @Test
  @DisplayName("Deve retornar 401 quando usuário não está autenticado")
  void shouldReturn401WhenUserIsNotAuthenticated() {
    // Arrange
    ChangePasswordRequest request =
        new ChangePasswordRequest(testEmail, "oldPassword123", "newPassword123");
    HttpEntity<ChangePasswordRequest> entity = new HttpEntity<>(request);

    // Act
    ResponseEntity<ErrorResponse> response =
        restTemplate.exchange(
            "/api/v1/auth/change-password", HttpMethod.POST, entity, ErrorResponse.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Deve retornar 401 quando token JWT é inválido")
  void shouldReturn401WhenJwtTokenIsInvalid() {
    // Arrange
    ChangePasswordRequest request =
        new ChangePasswordRequest(testEmail, "oldPassword123", "newPassword123");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth("invalid.token.here");
    HttpEntity<ChangePasswordRequest> entity = new HttpEntity<>(request, headers);

    // Act
    ResponseEntity<ErrorResponse> response =
        restTemplate.exchange(
            "/api/v1/auth/change-password", HttpMethod.POST, entity, ErrorResponse.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
