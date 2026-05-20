package br.com.lalurecf.application.port.in;

import br.com.lalurecf.infrastructure.dto.user.CreateUserRequest;
import br.com.lalurecf.infrastructure.dto.user.UserResponse;

/**
 * Port IN para caso de uso de criação de usuário.
 *
 * <p>Define contrato para criação de novos usuários no sistema.
 */
public interface CreateUserUseCase {

  /**
   * Cria um novo usuário.
   *
   * @param request dados do novo usuário
   * @return dados do usuário criado
   * @throws br.com.lalurecf.domain.exception.BusinessRuleViolationException se email já existe
   */
  UserResponse createUser(CreateUserRequest request);
}
