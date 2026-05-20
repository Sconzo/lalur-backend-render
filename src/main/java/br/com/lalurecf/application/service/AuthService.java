package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.AuthenticateUserUseCase;
import br.com.lalurecf.application.port.out.UserRepositoryPort;
import br.com.lalurecf.domain.exception.InvalidCredentialsException;
import br.com.lalurecf.domain.exception.MustChangePasswordException;
import br.com.lalurecf.domain.model.User;
import br.com.lalurecf.infrastructure.dto.auth.LoginRequest;
import br.com.lalurecf.infrastructure.dto.auth.LoginResponse;
import br.com.lalurecf.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de autenticação de usuários.
 *
 * <p>Implementa caso de uso de autenticação validando credenciais e gerando tokens JWT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements AuthenticateUserUseCase {

  private final UserRepositoryPort userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  @Override
  @Transactional(readOnly = true)
  public LoginResponse authenticate(LoginRequest request) {
    log.debug("Autenticando usuário: {}", request.getEmail());

    User user =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new InvalidCredentialsException("Credenciais inválidas"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      log.warn("Tentativa de login com senha incorreta: {}", request.getEmail());
      throw new InvalidCredentialsException("Credenciais inválidas");
    }

    // CRÍTICO: Bloqueia login se usuário deve trocar senha
    if (user.getMustChangePassword()) {
      log.warn("Tentativa de login com mustChangePassword=true: {}", request.getEmail());
      throw new MustChangePasswordException(
          "Você deve trocar sua senha temporária antes de fazer login.");
    }

    String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole());
    // RefreshToken gerado por compatibilidade com frontend, mas não é mais necessário
    // (accessToken tem validade de 7 dias - login semanal)
    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

    log.info("Login bem-sucedido: {} (role: {})", user.getEmail(), user.getRole());

    return LoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .role(user.getRole())
        .mustChangePassword(user.getMustChangePassword())
        .build();
  }
}
