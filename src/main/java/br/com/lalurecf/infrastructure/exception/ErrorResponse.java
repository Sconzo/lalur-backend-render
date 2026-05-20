package br.com.lalurecf.infrastructure.exception;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * DTO para respostas de erro padronizadas.
 *
 * <p>Formato inspirado em RFC 7807 (Problem Details for HTTP APIs).
 */
@Data
@Builder
public class ErrorResponse {
  private LocalDateTime timestamp;
  private Integer status;
  private String error;
  private String message;
  private Map<String, String> validationErrors;
}
