package br.com.lalurecf.infrastructure.dto.auth;

import lombok.Builder;
import lombok.Data;

/**
 * DTO para resposta de troca de senha.
 *
 * <p>Indica sucesso ou falha da operação com mensagem.
 */
@Data
@Builder
public class ChangePasswordResponse {
  private Boolean success;
  private String message;
}
