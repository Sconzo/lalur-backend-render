package br.com.lalurecf.infrastructure.adapter.in.rest;

import br.com.lalurecf.application.port.in.CreateUserUseCase;
import br.com.lalurecf.application.port.in.GetUserUseCase;
import br.com.lalurecf.application.port.in.ListUsersUseCase;
import br.com.lalurecf.application.port.in.ResetUserPasswordUseCase;
import br.com.lalurecf.application.port.in.ToggleUserStatusUseCase;
import br.com.lalurecf.application.port.in.UpdateUserUseCase;
import br.com.lalurecf.infrastructure.dto.user.CreateUserRequest;
import br.com.lalurecf.infrastructure.dto.user.ResetPasswordRequest;
import br.com.lalurecf.infrastructure.dto.user.ResetPasswordResponse;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.dto.user.UpdateUserRequest;
import br.com.lalurecf.infrastructure.dto.user.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller para gerenciamento de usuários (ADMIN apenas).
 *
 * <p>Endpoints CRUD protegidos com @PreAuthorize("hasRole('ADMIN')").
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gerenciamento de usuários (ADMIN apenas)")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

  private final CreateUserUseCase createUserUseCase;
  private final ListUsersUseCase listUsersUseCase;
  private final GetUserUseCase getUserUseCase;
  private final UpdateUserUseCase updateUserUseCase;
  private final ToggleUserStatusUseCase toggleUserStatusUseCase;
  private final ResetUserPasswordUseCase resetUserPasswordUseCase;

  /**
   * Cria um novo usuário.
   *
   * @param request dados do usuário a ser criado
   * @return usuário criado
   */
  @PostMapping
  @Operation(summary = "Criar usuário", description = "Cria um novo usuário (ADMIN apenas)")
  public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    UserResponse response = createUserUseCase.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Lista usuários com paginação e filtros.
   *
   * @param search termo de busca para nome/sobrenome (opcional)
   * @param includeInactive se deve incluir usuários inativos
   * @param pageable configuração de paginação
   * @return página de usuários
   */
  @GetMapping
  @Operation(
      summary = "Listar usuários",
      description = "Lista usuários com paginação e filtros (ADMIN apenas)")
  public ResponseEntity<Page<UserResponse>> listUsers(
      @RequestParam(required = false) String search,
      @RequestParam(name = "include_inactive", required = false, defaultValue = "false")
          Boolean includeInactive,
      @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Page<UserResponse> response = listUsersUseCase.listUsers(search, includeInactive, pageable);
    return ResponseEntity.ok(response);
  }

  /**
   * Obtém usuário por ID.
   *
   * @param id ID do usuário
   * @return dados do usuário
   */
  @GetMapping("/{id}")
  @Operation(summary = "Obter usuário", description = "Obtém usuário por ID (ADMIN apenas)")
  public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
    UserResponse response = getUserUseCase.getUserById(id);
    return ResponseEntity.ok(response);
  }

  /**
   * Atualiza dados de um usuário.
   *
   * @param id ID do usuário
   * @param request dados atualizados
   * @return usuário atualizado
   */
  @PutMapping("/{id}")
  @Operation(
      summary = "Atualizar usuário",
      description = "Atualiza dados de um usuário (ADMIN apenas)")
  public ResponseEntity<UserResponse> updateUser(
      @PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
    UserResponse response = updateUserUseCase.updateUser(id, request);
    return ResponseEntity.ok(response);
  }

  /**
   * Altera status de um usuário.
   *
   * @param id ID do usuário
   * @param request novo status
   * @return resposta com novo status
   */
  @PatchMapping("/{id}/status")
  @Operation(
      summary = "Alternar status",
      description = "Altera status do usuário entre ACTIVE e INACTIVE (ADMIN apenas)")
  public ResponseEntity<ToggleStatusResponse> toggleStatus(
      @PathVariable Long id, @Valid @RequestBody ToggleStatusRequest request) {
    ToggleStatusResponse response = toggleUserStatusUseCase.toggleStatus(id, request);
    return ResponseEntity.ok(response);
  }

  /**
   * Reseta senha de um usuário.
   *
   * @param id ID do usuário
   * @param request nova senha temporária
   * @return resposta indicando sucesso
   */
  @PostMapping("/{id}/reset-password")
  @Operation(
      summary = "Resetar senha",
      description = "Reseta senha do usuário com senha temporária (ADMIN apenas)")
  public ResponseEntity<ResetPasswordResponse> resetPassword(
      @PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
    ResetPasswordResponse response = resetUserPasswordUseCase.resetPassword(id, request);
    return ResponseEntity.ok(response);
  }
}
