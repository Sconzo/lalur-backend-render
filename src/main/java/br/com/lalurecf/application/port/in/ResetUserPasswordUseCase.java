package br.com.lalurecf.application.port.in;

import br.com.lalurecf.infrastructure.dto.user.ResetPasswordRequest;
import br.com.lalurecf.infrastructure.dto.user.ResetPasswordResponse;

/**
 * Port IN para caso de uso de reset de senha.
 *
 * <p>Define contrato para resetar senha de um usuário (ADMIN apenas).
 */
public interface ResetUserPasswordUseCase {

  /**
   * Reseta senha de um usuário.
   *
   * @param userId identificador do usuário
   * @param request nova senha temporária
   * @return resposta indicando sucesso
   * @throws br.com.lalurecf.infrastructure.exception.ResourceNotFoundException se usuário não
   *     encontrado
   * @throws br.com.lalurecf.domain.exception.BusinessRuleViolationException se usuário inativo
   */
  ResetPasswordResponse resetPassword(Long userId, ResetPasswordRequest request);
}
