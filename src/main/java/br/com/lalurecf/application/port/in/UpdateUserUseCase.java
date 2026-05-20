package br.com.lalurecf.application.port.in;

import br.com.lalurecf.infrastructure.dto.user.UpdateUserRequest;
import br.com.lalurecf.infrastructure.dto.user.UserResponse;

/**
 * Port IN para caso de uso de atualização de usuário.
 *
 * <p>Define contrato para atualizar dados de um usuário existente.
 */
public interface UpdateUserUseCase {

  /**
   * Atualiza dados de um usuário.
   *
   * @param id identificador do usuário
   * @param request dados atualizados
   * @return dados do usuário atualizado
   * @throws br.com.lalurecf.infrastructure.exception.ResourceNotFoundException se usuário não
   *     encontrado
   */
  UserResponse updateUser(Long id, UpdateUserRequest request);
}
