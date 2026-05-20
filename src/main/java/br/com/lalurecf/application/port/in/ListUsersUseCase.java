package br.com.lalurecf.application.port.in;

import br.com.lalurecf.infrastructure.dto.user.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port IN para caso de uso de listagem de usuários.
 *
 * <p>Define contrato para listagem paginada de usuários com filtros.
 */
public interface ListUsersUseCase {

  /**
   * Lista usuários com paginação e filtros.
   *
   * @param search termo de busca para nome/sobrenome (opcional)
   * @param includeInactive se deve incluir usuários inativos
   * @param pageable configuração de paginação
   * @return página de usuários
   */
  Page<UserResponse> listUsers(String search, Boolean includeInactive, Pageable pageable);
}
