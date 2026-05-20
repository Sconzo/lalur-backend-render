package br.com.lalurecf.infrastructure.dto.auth;

import br.com.lalurecf.domain.enums.UserRole;
import lombok.Builder;
import lombok.Data;

/**
 * DTO para resposta de login.
 *
 * <p>Contém access token JWT (válido por 7 dias) e dados do usuário autenticado.
 * RefreshToken incluído por compatibilidade, mas não é mais necessário - basta usar o accessToken.
 */
@Data
@Builder
public class LoginResponse {
  /** Access token JWT com validade de 7 dias - suficiente para uso direto sem refresh. */
  private String accessToken;

  /** Mantido por compatibilidade com frontend (mesmo valor que accessToken). */
  private String refreshToken;

  private String email;
  private String firstName;
  private String lastName;
  private UserRole role;
  private Boolean mustChangePassword;
}
