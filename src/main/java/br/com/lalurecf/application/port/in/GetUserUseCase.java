package br.com.lalurecf.application.port.in;

import br.com.lalurecf.infrastructure.dto.user.UserResponse;

/**
 * Port IN para caso de uso de obtenção de usuário por ID.
 *
 * <p>Define contrato para buscar um usuário específico.
 */
public interface GetUserUseCase {

  /**
   * Obtém usuário por ID.
   *
   * @param id identificador do usuário
   * @return dados do usuário
   * @throws br.com.lalurecf.infrastructure.exception.ResourceNotFoundException se usuário não
   *     encontrado
   */
  UserResponse getUserById(Long id);
}
