package br.com.lalurecf.infrastructure.adapter.in.rest;

import br.com.lalurecf.application.port.in.AuthenticateUserUseCase;
import br.com.lalurecf.application.port.in.ChangePasswordUseCase;
import br.com.lalurecf.infrastructure.dto.auth.ChangePasswordRequest;
import br.com.lalurecf.infrastructure.dto.auth.ChangePasswordResponse;
import br.com.lalurecf.infrastructure.dto.auth.LoginRequest;
import br.com.lalurecf.infrastructure.dto.auth.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller para endpoints de autenticação.
 *
 * <p>Endpoints para login e troca de senha. Access tokens têm validade de 7 dias,
 * não sendo necessário refresh (login semanal para conveniência em sistema corporativo).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints de autenticação")
public class AuthController {

  private final AuthenticateUserUseCase authenticateUserUseCase;
  private final ChangePasswordUseCase changePasswordUseCase;

  /**
   * Endpoint de login.
   *
   * @param request credenciais de login
   * @return tokens JWT e dados do usuário
   */
  @PostMapping("/login")
  @Operation(summary = "Login", description = "Autenticar usuário com email e senha")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    LoginResponse response = authenticateUserUseCase.authenticate(request);
    return ResponseEntity.ok(response);
  }

  /**
   * Endpoint de troca de senha.
   *
   * @param request requisição com email, senha temporária e nova senha
   * @return resposta indicando sucesso
   */
  @PostMapping("/change-password")
  @Operation(
      summary = "Trocar senha",
      description = "Trocar senha temporária (usuário com mustChangePassword=true)")
  public ResponseEntity<ChangePasswordResponse> changePassword(
      @Valid @RequestBody ChangePasswordRequest request) {
    ChangePasswordResponse response = changePasswordUseCase.changePassword(request);
    return ResponseEntity.ok(response);
  }
}
