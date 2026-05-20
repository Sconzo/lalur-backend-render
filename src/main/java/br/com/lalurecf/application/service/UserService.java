package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.CreateUserUseCase;
import br.com.lalurecf.application.port.in.GetUserUseCase;
import br.com.lalurecf.application.port.in.ListUsersUseCase;
import br.com.lalurecf.application.port.in.ResetUserPasswordUseCase;
import br.com.lalurecf.application.port.in.ToggleUserStatusUseCase;
import br.com.lalurecf.application.port.in.UpdateUserUseCase;
import br.com.lalurecf.application.port.out.UserRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.exception.BusinessRuleViolationException;
import br.com.lalurecf.domain.model.User;
import br.com.lalurecf.infrastructure.dto.mapper.UserDtoMapper;
import br.com.lalurecf.infrastructure.dto.user.CreateUserRequest;
import br.com.lalurecf.infrastructure.dto.user.ResetPasswordRequest;
import br.com.lalurecf.infrastructure.dto.user.ResetPasswordResponse;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.dto.user.UpdateUserRequest;
import br.com.lalurecf.infrastructure.dto.user.UserResponse;
import br.com.lalurecf.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de gerenciamento de usuários.
 *
 * <p>Implementa casos de uso para criar, listar, visualizar, editar e alternar status de usuários.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService
    implements CreateUserUseCase,
        ListUsersUseCase,
        GetUserUseCase,
        UpdateUserUseCase,
        ToggleUserStatusUseCase,
        ResetUserPasswordUseCase {

  private final UserRepositoryPort userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserDtoMapper userDtoMapper;

  @Override
  @Transactional
  public UserResponse createUser(CreateUserRequest request) {
    log.debug("Criando usuário com email: {}", request.getEmail());

    // Validar email único
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      log.warn("Tentativa de criar usuário com email já existente: {}", request.getEmail());
      throw new BusinessRuleViolationException("Email já cadastrado");
    }

    // Criar usuário com senha hashada e mustChangePassword = true
    User user =
        User.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(request.getRole())
            .mustChangePassword(true)
            .status(Status.ACTIVE)
            .build();

    User saved = userRepository.save(user);
    log.info("Usuário criado com sucesso: id={}, email={}", saved.getId(), saved.getEmail());

    return userDtoMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<UserResponse> listUsers(String search, Boolean includeInactive, Pageable pageable) {
    log.debug(
        "Listando usuários: search={}, includeInactive={}, page={}",
        search,
        includeInactive,
        pageable.getPageNumber());

    Page<User> users;

    boolean hasSearch = search != null && !search.trim().isEmpty();
    boolean shouldIncludeInactive = includeInactive != null && includeInactive;

    if (hasSearch && shouldIncludeInactive) {
      // Busca por nome sem filtro de status
      users = userRepository.findByNameContaining(search, pageable);
    } else if (hasSearch) {
      // Busca por nome apenas ACTIVE
      users = userRepository.findByNameContainingAndStatus(search, Status.ACTIVE, pageable);
    } else if (shouldIncludeInactive) {
      // Todos os usuários (sem filtro)
      users = userRepository.findAll(pageable);
    } else {
      // Apenas ACTIVE (padrão)
      users = userRepository.findByStatus(Status.ACTIVE, pageable);
    }

    return users.map(userDtoMapper::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponse getUserById(Long id) {
    log.debug("Buscando usuário por ID: {}", id);

    User user =
        userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário", id));

    return userDtoMapper.toResponse(user);
  }

  @Override
  @Transactional
  public UserResponse updateUser(Long id, UpdateUserRequest request) {
    log.debug("Atualizando usuário: id={}", id);

    User user =
        userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário", id));

    // Atualizar campos permitidos (não permite alterar email/password)
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    user.setRole(request.getRole());

    User updated = userRepository.save(user);
    log.info("Usuário atualizado com sucesso: id={}", updated.getId());

    return userDtoMapper.toResponse(updated);
  }

  @Override
  @Transactional
  public ToggleStatusResponse toggleStatus(Long id, ToggleStatusRequest request) {
    log.debug("Alterando status do usuário: id={}, newStatus={}", id, request.getStatus());

    User user =
        userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário", id));

    user.setStatus(request.getStatus());
    userRepository.save(user);

    log.info(
        "Status do usuário alterado com sucesso: id={}, newStatus={}", id, request.getStatus());

    return ToggleStatusResponse.builder()
        .success(true)
        .message("Status do usuário alterado com sucesso")
        .newStatus(request.getStatus())
        .build();
  }

  @Override
  @Transactional
  public ResetPasswordResponse resetPassword(Long userId, ResetPasswordRequest request) {
    log.debug("Resetando senha do usuário: id={}", userId);

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));

    // Validar que usuário está ativo
    if (user.getStatus() != Status.ACTIVE) {
      log.warn("Tentativa de resetar senha de usuário inativo: id={}", userId);
      throw new BusinessRuleViolationException("Não é possível resetar senha de usuário inativo");
    }

    // Atualizar senha e forçar troca no próximo login
    user.setPassword(passwordEncoder.encode(request.getTemporaryPassword()));
    user.setMustChangePassword(true);
    userRepository.save(user);

    log.info("Senha resetada com sucesso para usuário: id={}, email={}", userId, user.getEmail());

    return ResetPasswordResponse.builder()
        .success(true)
        .message("Senha redefinida. Usuário deve trocar no próximo login.")
        .build();
  }
}
