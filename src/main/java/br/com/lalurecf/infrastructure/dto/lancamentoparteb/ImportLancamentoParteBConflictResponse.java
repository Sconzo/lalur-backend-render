package br.com.lalurecf.infrastructure.dto.lancamentoparteb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta para conflitos na importação de Lançamentos da Parte B.
 *
 * <p>Retornado com HTTP 409 quando já existem lançamentos da Parte B para a empresa no
 * ano de referência informado. O frontend usa esta resposta para exibir um modal de
 * confirmação de sobrescrita; ao confirmar, deve refazer a chamada de importação com
 * {@code ?overwrite=true}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class ImportLancamentoParteBConflictResponse {

  /** Quantidade de lançamentos já existentes para a empresa no ano informado. */
  private long existingCount;

  /** Ano de referência em conflito. */
  private Integer anoReferencia;

  /** Mensagem descritiva do conflito. */
  private String message;
}
