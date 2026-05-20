package br.com.lalurecf.infrastructure.dto.mapper;

import br.com.lalurecf.domain.model.User;
import br.com.lalurecf.infrastructure.dto.user.UserResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper para conversão entre User (domain) e UserResponse (DTO).
 *
 * <p>Converte objetos de domínio para DTOs de resposta.
 */
@Component
public class UserDtoMapper {

  /**
   * Converte User domain para UserResponse DTO.
   *
   * @param user objeto de domínio
   * @return DTO de resposta
   */
  public UserResponse toResponse(User user) {
    if (user == null) {
      return null;
    }

    return UserResponse.builder()
        .id(user.getId())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .email(user.getEmail())
        .role(user.getRole())
        .status(user.getStatus())
        .mustChangePassword(user.getMustChangePassword())
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }
}
