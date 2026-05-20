package br.com.lalurecf.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.lalurecf.application.port.out.UserRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.UserRole;
import br.com.lalurecf.domain.model.User;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Testes de integração para AuthController. */
@DisplayName("AuthController - Login Integration Tests")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest extends IntegrationTestBase {

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private UserRepositoryPort userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private String testEmail;

  @BeforeEach
  void setup() {
    // Usa timestamp para email único em cada teste
    testEmail = "test" + System.currentTimeMillis() + "@test.com";

    // Seed usuário de teste
    User user =
        User.builder()
            .email(testEmail)
            .password(passwordEncoder.encode("password123"))
            .firstName("Test")
            .lastName("User")
            .role(UserRole.ADMIN)
            .mustChangePassword(false)
            .status(Status.ACTIVE)
            .build();
    userRepository.save(user);
  }

  @Test
  @DisplayName("Deve retornar tokens válidos em login bem-sucedido")
  void shouldReturnValidTokensOnSuccessfulLogin() {
    // Arrange
    LoginRequest request = new LoginRequest(testEmail, "password123");

    // Act
    ResponseEntity<LoginResponse> response =
        restTemplate.postForEntity("/api/v1/api/v1/auth/login", request, LoginResponse.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getAccessToken()).isNotNull();
    assertThat(response.getBody().getRefreshToken()).isNotNull();
    assertThat(response.getBody().getEmail()).isEqualTo(testEmail);
    assertThat(response.getBody().getFirstName()).isEqualTo("Test");
    assertThat(response.getBody().getLastName()).isEqualTo("User");
    assertThat(response.getBody().getRole()).isEqualTo(UserRole.ADMIN);
    assertThat(response.getBody().getMustChangePassword()).isFalse();
  }

  @Test
  @DisplayName("Deve retornar 401 com senha incorreta")
  void shouldReturn401WithWrongPassword() {
    // Arrange
    LoginRequest request = new LoginRequest(testEmail, "wrongpassword");

    // Act
    ResponseEntity<ErrorResponse> response =
        restTemplate.postForEntity("/api/v1/auth/login", request, ErrorResponse.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("Credenciais inválidas");
  }

  @Test
  @DisplayName("Deve retornar 401 com email inexistente")
  void shouldReturn401WithNonExistentEmail() {
    // Arrange
    LoginRequest request = new LoginRequest("nonexistent@test.com", "password123");

    // Act
    ResponseEntity<ErrorResponse> response =
        restTemplate.postForEntity("/api/v1/auth/login", request, ErrorResponse.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("Credenciais inválidas");
  }

  @Test
  @DisplayName("Deve retornar 400 com campos vazios")
  void shouldReturn400WithEmptyFields() {
    // Arrange
    LoginRequest request = new LoginRequest("", "");

    // Act
    ResponseEntity<ErrorResponse> response =
        restTemplate.postForEntity("/api/v1/auth/login", request, ErrorResponse.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getValidationErrors()).isNotEmpty();
  }

  @Test
  @DisplayName("Deve retornar 400 com email inválido")
  void shouldReturn400WithInvalidEmail() {
    // Arrange
    LoginRequest request = new LoginRequest("invalid-email", "password123");

    // Act
    ResponseEntity<ErrorResponse> response =
        restTemplate.postForEntity("/api/v1/auth/login", request, ErrorResponse.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getValidationErrors()).containsKey("email");
  }
}
