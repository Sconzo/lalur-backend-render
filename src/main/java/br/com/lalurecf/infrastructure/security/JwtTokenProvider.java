package br.com.lalurecf.infrastructure.security;

import br.com.lalurecf.domain.enums.UserRole;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Provedor de tokens JWT para autenticação stateless.
 *
 * <p>Gera e valida tokens JWT usando HMAC256. Tokens contêm email (subject) e role (claim).
 * Access tokens expiram em 7 dias para conveniência do usuário (sistema corporativo interno).
 * Refresh tokens mantidos por compatibilidade mas não são mais usados.
 */
@Component
public class JwtTokenProvider {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.access-token-expiration}")
  private Long accessTokenExpiration;

  @Value("${jwt.refresh-token-expiration}")
  private Long refreshTokenExpiration;

  /**
   * Gera access token JWT com email e role.
   *
   * @param email email do usuário (usado como subject)
   * @param role papel do usuário (ADMIN ou CONTADOR)
   * @return token JWT assinado
   */
  public String generateAccessToken(String email, UserRole role) {
    return JWT.create()
        .withSubject(email)
        .withClaim("role", role.name())
        .withIssuedAt(new Date())
        .withExpiresAt(new Date(System.currentTimeMillis() + accessTokenExpiration))
        .sign(Algorithm.HMAC256(secret));
  }

  /**
   * Gera refresh token JWT apenas com email.
   *
   * @param email email do usuário
   * @return token JWT assinado
   */
  public String generateRefreshToken(String email) {
    return JWT.create()
        .withSubject(email)
        .withIssuedAt(new Date())
        .withExpiresAt(new Date(System.currentTimeMillis() + refreshTokenExpiration))
        .sign(Algorithm.HMAC256(secret));
  }

  /**
   * Valida token JWT.
   *
   * @param token token a ser validado
   * @return true se válido, false se inválido ou expirado
   */
  public boolean validateToken(String token) {
    try {
      JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
      return true;
    } catch (JWTVerificationException e) {
      return false;
    }
  }

  /**
   * Extrai email do token.
   *
   * @param token token JWT
   * @return email (subject do token)
   */
  public String getEmailFromToken(String token) {
    DecodedJWT jwt = JWT.decode(token);
    return jwt.getSubject();
  }

  /**
   * Extrai role do token.
   *
   * @param token token JWT
   * @return role do usuário
   */
  public UserRole getRoleFromToken(String token) {
    DecodedJWT jwt = JWT.decode(token);
    String role = jwt.getClaim("role").asString();
    return UserRole.valueOf(role);
  }
}
