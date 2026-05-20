package br.com.lalurecf.application.port.out;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port OUT para persistência de usuários.
 *
 * <p>Interface de saída seguindo arquitetura hexagonal. Define contrato para repositório de
 * usuários sem conhecer detalhes de implementação (JPA, JDBC, etc).
 *
 * <p>Trabalha com domain objects (User) e não com entidades JPA.
 */
public interface UserRepositoryPort {

  /**
   * Busca usuário por email.
   *
   * @param email email do usuário
   * @return Optional contendo usuário se encontrado
   */
  Optional<User> findByEmail(String email);

  /**
   * Salva usuário (insert ou update).
   *
   * @param user usuário a ser salvo
   * @return usuário salvo com ID preenchido
   */
  User save(User user);

  /**
   * Busca usuário por ID.
   *
   * @param id ID do usuário
   * @return Optional contendo usuário se encontrado
   */
  Optional<User> findById(Long id);

  /**
   * Lista todos os usuários.
   *
   * @return lista de usuários
   */
  List<User> findAll();

  /**
   * Lista todos os usuários com paginação.
   *
   * @param pageable configuração de paginação
   * @return página de usuários
   */
  Page<User> findAll(Pageable pageable);

  /**
   * Busca usuários por status com paginação.
   *
   * @param status status a filtrar
   * @param pageable configuração de paginação
   * @return página de usuários com o status especificado
   */
  Page<User> findByStatus(Status status, Pageable pageable);

  /**
   * Busca usuários por nome ou sobrenome contendo termo.
   *
   * @param search termo de busca
   * @param pageable configuração de paginação
   * @return página de usuários que contém o termo no nome ou sobrenome
   */
  Page<User> findByNameContaining(String search, Pageable pageable);

  /**
   * Busca usuários por nome ou sobrenome contendo termo e status.
   *
   * @param search termo de busca
   * @param status status a filtrar
   * @param pageable configuração de paginação
   * @return página de usuários que contém o termo e tem o status especificado
   */
  Page<User> findByNameContainingAndStatus(String search, Status status, Pageable pageable);
}
