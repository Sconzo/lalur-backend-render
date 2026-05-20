package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import br.com.lalurecf.domain.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entidade JPA para usuários do sistema.
 *
 * <p>Mapeada para tabela tb_usuario seguindo convenção snake_case (ADR-001). Estende BaseEntity
 * para herdar auditoria automática e soft delete.
 */
@Entity
@Table(name = "tb_usuario")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class UserEntity extends BaseEntity {

  @Column(name = "primeiro_nome", nullable = false)
  private String firstName;

  @Column(name = "sobrenome", nullable = false)
  private String lastName;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "senha", nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(name = "funcao", nullable = false)
  private UserRole role;

  @Column(name = "deve_mudar_senha", nullable = false)
  private Boolean mustChangePassword = true;
}
