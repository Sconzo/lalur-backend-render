package br.com.lalurecf.application.port.in;

import br.com.lalurecf.infrastructure.dto.auth.LoginRequest;
import br.com.lalurecf.infrastructure.dto.auth.LoginResponse;

/**
 * Port IN para caso de uso de autenticação de usuário.
 *
 * <p>Define contrato para autenticação de usuário com email e senha, retornando tokens JWT.
 */
public interface AuthenticateUserUseCase {

  /**
   * Autentica usuário com credenciais.
   *
   * @param request credenciais de login (email e senha)
   * @return resposta contendo tokens JWT e dados do usuário
   * @throws br.com.lalurecf.domain.exception.InvalidCredentialsException se credenciais inválidas
   */
  LoginResponse authenticate(LoginRequest request);
}
