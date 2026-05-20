package br.com.lalurecf.application.port.in;

import br.com.lalurecf.infrastructure.dto.auth.ChangePasswordRequest;
import br.com.lalurecf.infrastructure.dto.auth.ChangePasswordResponse;

/**
 * Port IN para caso de uso de troca de senha.
 *
 * <p>Define contrato para troca de senha do usuário autenticado.
 */
public interface ChangePasswordUseCase {

  /**
   * Troca senha do usuário autenticado.
   *
   * @param request requisição contendo senha atual e nova senha
   * @return resposta indicando sucesso ou falha
   * @throws br.com.lalurecf.domain.exception.InvalidCurrentPasswordException se senha atual
   *     incorreta
   * @throws br.com.lalurecf.domain.exception.BusinessRuleViolationException se nova senha igual à
   *     atual
   */
  ChangePasswordResponse changePassword(ChangePasswordRequest request);
}
