package br.com.lalurecf.infrastructure.adapter.in.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller de teste para validar autenticação e autorização.
 *
 * <p>Endpoints protegidos que exigem autenticação JWT válida.
 */
@RestController
@RequestMapping("/test")
public class TestController {

  /**
   * Endpoint protegido que requer autenticação.
   *
   * @param authentication contexto de autenticação Spring Security
   * @return mensagem com userId do usuário autenticado
   */
  @GetMapping("/protected")
  @PreAuthorize("isAuthenticated()")
  public String protectedEndpoint(Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    return "Access granted for userId: " + userId;
  }

  /**
   * Endpoint protegido que requer role ADMIN.
   *
   * @param authentication contexto de autenticação
   * @return mensagem de acesso admin
   */
  @GetMapping("/admin")
  @PreAuthorize("hasRole('ADMIN')")
  public String adminEndpoint(Authentication authentication) {
    Long userId = (Long) authentication.getPrincipal();
    return "Admin access granted for userId: " + userId;
  }
}
