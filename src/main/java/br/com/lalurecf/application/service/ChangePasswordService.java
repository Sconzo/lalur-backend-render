package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.ChangePasswordUseCase;
import br.com.lalurecf.application.port.out.UserRepositoryPort;
import br.com.lalurecf.domain.exception.BusinessRuleViolationException;
import br.com.lalurecf.domain.exception.InvalidCredentialsException;
import br.com.lalurecf.domain.exception.InvalidCurrentPasswordException;
import br.com.lalurecf.domain.model.User;
import br.com.lalurecf.infrastructure.dto.auth.ChangePasswordRequest;
import br.com.lalurecf.infrastructure.dto.auth.ChangePasswordResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de troca de senha.
 *
 * <p>Implementa caso de uso de troca de senha temporária para usuários com mustChangePassword=true.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChangePasswordService implements ChangePasswordUseCase {

  private final UserRepositoryPort userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public ChangePasswordResponse changePassword(ChangePasswordRequest request) {
    String email = request.getEmail();
    log.debug("Alterando senha para usuário: {}", email);

    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new InvalidCredentialsException("Credenciais inválidas"));

    // Valida senha temporária (currentPassword)
    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
      log.warn("Tentativa de troca de senha com senha temporária incorreta: {}", email);
      throw new InvalidCurrentPasswordException("Senha temporária inválida");
    }

    // Valida que nova senha é diferente da temporária
    if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
      log.warn("Tentativa de usar mesma senha ao trocar: {}", email);
      throw new BusinessRuleViolationException("Nova senha não pode ser igual à senha temporária");
    }

    // Atualiza senha e libera login normal
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    user.setMustChangePassword(false);
    userRepository.save(user);

    log.info("Senha alterada com sucesso para usuário: {}", email);

    return ChangePasswordResponse.builder()
        .success(true)
        .message("Senha alterada com sucesso. Você pode fazer login com a nova senha.")
        .build();
  }
}
