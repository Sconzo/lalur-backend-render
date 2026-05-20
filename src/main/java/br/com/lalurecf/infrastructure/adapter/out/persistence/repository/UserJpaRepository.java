package br.com.lalurecf.infrastructure.adapter.out.persistence.repository;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository para UserEntity.
 *
 * <p>Interface do Spring Data que fornece operações CRUD automáticas. Trabalha com UserEntity
 * (entidade JPA), não com domain objects.
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

  /**
   * Busca usuário por email.
   *
   * @param email email do usuário
   * @return Optional contendo UserEntity se encontrado
   */
  Optional<UserEntity> findByEmail(String email);

  /**
   * Busca usuários por status com paginação.
   *
   * @param status status a filtrar
   * @param pageable configuração de paginação
   * @return página de usuários com o status especificado
   */
  Page<UserEntity> findByStatus(Status status, Pageable pageable);

  /**
   * Busca usuários por nome ou sobrenome contendo termo com paginação.
   *
   * @param firstName termo de busca no firstName
   * @param lastName termo de busca no lastName
   * @param pageable configuração de paginação
   * @return página de usuários que contém o termo no nome ou sobrenome
   */
  Page<UserEntity> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
      String firstName, String lastName, Pageable pageable);

  /**
   * Busca usuários por nome ou sobrenome contendo termo e status com paginação.
   *
   * @param firstName termo de busca no firstName
   * @param lastName termo de busca no lastName
   * @param status status a filtrar
   * @param pageable configuração de paginação
   * @return página de usuários que contém o termo no nome/sobrenome e tem o status especificado
   */
  Page<UserEntity> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseAndStatus(
      String firstName, String lastName, Status status, Pageable pageable);
}
