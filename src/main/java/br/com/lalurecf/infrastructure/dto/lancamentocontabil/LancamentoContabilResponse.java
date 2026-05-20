package br.com.lalurecf.infrastructure.dto.lancamentocontabil;

import br.com.lalurecf.domain.enums.Status;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta para lançamento contábil.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LancamentoContabilResponse {

  private Long id;
  private Long contaDebitoId;
  private String contaDebitoCodigo;
  private String contaDebitoNome;
  private Long contaCreditoId;
  private String contaCreditoCodigo;
  private String contaCreditoNome;
  private LocalDate data;
  private BigDecimal valor;
  private String historico;
  private String numeroDocumento;
  private Integer fiscalYear;
  private Status status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
