package br.com.lalurecf.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.util.IntegrationTestBase;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Testes de integração para BaseEntity e auditoria automática. */
@DisplayName("BaseEntity - Auditoria Automática")
class BaseEntityAuditingTest extends IntegrationTestBase {

  @Autowired private TestEntityRepository repository;

  @Test
  @DisplayName("Deve preencher campos de auditoria automaticamente ao criar entidade")
  void shouldPopulateAuditFieldsOnCreate() {
    // Arrange
    TestEntity entity = new TestEntity("Test Name");

    // Act
    TestEntity saved = repository.save(entity);

    // Assert
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
    assertThat(saved.getCreatedBy()).isEqualTo("system");
    assertThat(saved.getUpdatedBy()).isEqualTo("system");
    assertThat(saved.getStatus()).isEqualTo(Status.ACTIVE);
  }

  @Test
  @DisplayName("Deve atualizar updatedAt e updatedBy ao modificar entidade")
  void shouldUpdateAuditFieldsOnModify() throws InterruptedException {
    // Arrange
    TestEntity entity = repository.save(new TestEntity("Original"));
    LocalDateTime originalCreatedAt = entity.getCreatedAt();
    LocalDateTime originalUpdatedAt = entity.getUpdatedAt();

    Thread.sleep(100); // Garantir diferença de timestamp

    // Act
    entity.setName("Modified");
    TestEntity updated = repository.save(entity);

    // Assert
    assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
    assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt); // não muda
    assertThat(updated.getName()).isEqualTo("Modified");
  }

  @Test
  @DisplayName("Deve permitir soft delete alterando status para INACTIVE")
  void shouldSupportSoftDelete() {
    // Arrange
    TestEntity entity = repository.save(new TestEntity("To Delete"));

    // Act
    entity.setStatus(Status.INACTIVE);
    TestEntity deleted = repository.save(entity);

    // Assert
    assertThat(deleted.getStatus()).isEqualTo(Status.INACTIVE);
    assertThat(deleted.getId()).isNotNull(); // ainda existe no banco
  }
}
