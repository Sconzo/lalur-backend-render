package br.com.lalurecf.infrastructure.dto.lancamentocontabil;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta para deleção em lote de lançamentos contábeis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteLancamentoContabilBatchResponse {

  private int quantidadeDeletada;
  private Integer mes;
  private Integer ano;
  private String mensagem;
}
