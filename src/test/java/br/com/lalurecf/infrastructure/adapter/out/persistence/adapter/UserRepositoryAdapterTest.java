package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.lalurecf.application.port.out.UserRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.UserRole;
import br.com.lalurecf.domain.model.User;
import br.com.lalurecf.util.IntegrationTestBase;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/** Testes de integração para UserRepositoryAdapter. */
@DisplayName("UserRepositoryAdapter - Testes de Integração")
class UserRepositoryAdapterTest extends IntegrationTestBase {

  @Autowired private UserRepositoryPort userRepository;

  @Test
  @DisplayName("Deve salvar usuário e recuperar por email")
  void shouldSaveAndFindUserByEmail() {
    // Arrange
    User user =
        User.builder()
            .firstName("João")
            .lastName("Silva")
            .email("joao@test.com")
            .password("hashed_password")
            .role(UserRole.CONTADOR)
            .mustChangePassword(true)
            .status(Status.ACTIVE)
            .build();

    // Act
    User saved = userRepository.save(user);
    Optional<User> found = userRepository.findByEmail("joao@test.com");

    // Assert
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getCreatedBy()).isEqualTo("system");
    assertThat(found).isPresent();
    assertThat(found.get().getFirstName()).isEqualTo("João");
    assertThat(found.get().getLastName()).isEqualTo("Silva");
    assertThat(found.get().getRole()).isEqualTo(UserRole.CONTADOR);
  }

  @Test
  @DisplayName("Deve lançar exception ao criar usuário com email duplicado")
  void shouldThrowExceptionOnDuplicateEmail() {
    // Arrange
    User user1 =
        User.builder()
            .email("duplicate@test.com")
            .firstName("User")
            .lastName("One")
            .password("pass")
            .role(UserRole.ADMIN)
            .mustChangePassword(false)
            .status(Status.ACTIVE)
            .build();

    userRepository.save(user1);

    User user2 =
        user1.toBuilder()
            .id(null) // novo usuário
            .firstName("User Two")
            .build();

    // Act & Assert
    assertThatThrownBy(() -> userRepository.save(user2))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("Deve persistir soft delete (status INACTIVE)")
  void shouldPersistSoftDelete() {
    // Arrange
    User user =
        User.builder()
            .firstName("Delete")
            .lastName("Test")
            .email("delete@test.com")
            .password("pass")
            .role(UserRole.CONTADOR)
            .mustChangePassword(true)
            .status(Status.ACTIVE)
            .build();

    User saved = userRepository.save(user);

    // Act - soft delete
    saved.setStatus(Status.INACTIVE);
    User deleted = userRepository.save(saved);

    // Assert
    Optional<User> found = userRepository.findById(deleted.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getStatus()).isEqualTo(Status.INACTIVE);
    assertThat(found.get().getId()).isEqualTo(saved.getId());
  }

  @Test
  @DisplayName("Deve listar todos os usuários")
  void shouldFindAllUsers() {
    // Arrange
    User user1 =
        User.builder()
            .firstName("User1")
            .lastName("Test1")
            .email("user1@test.com")
            .password("pass1")
            .role(UserRole.ADMIN)
            .mustChangePassword(false)
            .status(Status.ACTIVE)
            .build();

    User user2 =
        User.builder()
            .firstName("User2")
            .lastName("Test2")
            .email("user2@test.com")
            .password("pass2")
            .role(UserRole.CONTADOR)
            .mustChangePassword(true)
            .status(Status.ACTIVE)
            .build();

    userRepository.save(user1);
    userRepository.save(user2);

    // Act
    var users = userRepository.findAll();

    // Assert
    assertThat(users).hasSizeGreaterThanOrEqualTo(2);
    assertThat(users).extracting(User::getEmail).contains("user1@test.com", "user2@test.com");
  }
}
