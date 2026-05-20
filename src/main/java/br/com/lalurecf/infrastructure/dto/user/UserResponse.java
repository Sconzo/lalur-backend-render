package br.com.lalurecf.infrastructure.dto.user;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.UserRole;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resposta de usuário.
 *
 * <p>Contém todos os dados do usuário exceto a senha.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

  private Long id;
  private String firstName;
  private String lastName;
  private String email;
  private UserRole role;
  private Status status;
  private Boolean mustChangePassword;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
