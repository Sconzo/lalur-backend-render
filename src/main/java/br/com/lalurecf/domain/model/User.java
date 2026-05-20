package br.com.lalurecf.domain.model;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.UserRole;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain Model - Usuário do sistema.
 *
 * <p>POJO puro sem dependências de frameworks (Spring/JPA). Representa usuário no contexto de
 * domínio seguindo princípios da arquitetura hexagonal.
 *
 * <p>Usuários podem ter papéis ADMIN ou CONTADOR e são autenticados via email/senha.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class User {
  private Long id;
  private String firstName;
  private String lastName;
  private String email;
  private String password;
  private UserRole role;
  private Boolean mustChangePassword;
  private Status status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long createdBy;
  private Long updatedBy;
}
