package br.com.lalurecf.infrastructure.adapter.out.persistence;

import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Entidade de teste para validar funcionalidade da BaseEntity.
 *
 * <p>Usada apenas em testes de integração para verificar auditoria automática.
 */
@Entity
@Table(name = "test_entity")
public class TestEntity extends BaseEntity {

  @Column(nullable = false)
  private String name;

  protected TestEntity() {
    // JPA requires default constructor
  }

  public TestEntity(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
