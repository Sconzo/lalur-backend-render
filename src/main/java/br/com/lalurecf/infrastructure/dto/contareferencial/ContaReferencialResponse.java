package br.com.lalurecf.infrastructure.dto.contareferencial;

import br.com.lalurecf.domain.enums.Status;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resposta de conta referencial RFB.
 *
 * <p>Contém todos os dados da conta referencial incluindo metadados de auditoria.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContaReferencialResponse {

  private Long id;
  private String codigoRfb;
  private String descricao;
  private Integer anoValidade;
  private Status status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
