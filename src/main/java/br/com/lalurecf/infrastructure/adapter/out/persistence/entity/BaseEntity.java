package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import br.com.lalurecf.domain.enums.Status;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Entidade base abstrata com auditoria e soft delete.
 *
 * <p>Todas as entidades JPA devem estender esta classe para herdar:
 *
 * <ul>
 *   <li>ID auto-incrementado (IDENTITY strategy)
 *   <li>Status (ACTIVE/INACTIVE) para soft delete pattern
 *   <li>Campos de auditoria automática (criado_em, atualizado_em, criado_por, atualizado_por)
 * </ul>
 *
 * <p>Auditoria é gerenciada automaticamente por Spring Data JPA com @EnableJpaAuditing. Campos de
 * auditoria seguem convenção snake_case conforme ADR-001.
 */
@Getter
@Setter
@MappedSuperclass
@SuperBuilder
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

  @Id
  @Setter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  protected Long id;

  @Setter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  protected Status status = Status.ACTIVE;

  @CreatedDate
  @Column(name = "criado_em", nullable = false, updatable = false)
  protected LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "atualizado_em")
  protected LocalDateTime updatedAt;

  @CreatedBy
  @Column(name = "criado_por", nullable = false, updatable = false)
  protected Long createdBy;

  @LastModifiedBy
  @Column(name = "atualizado_por")
  protected Long updatedBy;

}
