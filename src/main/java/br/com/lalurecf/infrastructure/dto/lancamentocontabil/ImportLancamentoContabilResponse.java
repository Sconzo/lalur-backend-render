package br.com.lalurecf.infrastructure.dto.lancamentocontabil;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta para importação de lançamentos contábeis via CSV/TXT.
 *
 * <p>Contém relatório detalhado do processamento incluindo estatísticas e lista de erros por
 * linha.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportLancamentoContabilResponse {

  /** Indica se a importação foi bem-sucedida (todas linhas processadas sem erros). */
  private boolean success;

  /** Mensagem resumo da importação. */
  private String message;

  /** Total de linhas no arquivo (excluindo header). */
  private int totalLines;

  /** Linhas processadas com sucesso. */
  private int processedLines;

  /** Linhas puladas por erro. */
  private int skippedLines;

  /** Lista de erros por linha. */
  @Builder.Default private List<ImportError> errors = new ArrayList<>();

  /** Preview das contas que seriam criadas (apenas se dryRun=true). */
  private List<LancamentoContabilPreview> preview;

  /**
   * Classe interna para representar erro em linha do CSV.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ImportError {

    /** Número da linha com erro (1-based, excluindo header). */
    private int lineNumber;

    /** Mensagem de erro. */
    private String error;
  }

  /**
   * Classe interna para preview de lançamento contábil (dry run).
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LancamentoContabilPreview {

    /** Código da conta de débito. */
    private String contaDebitoCode;

    /** Código da conta de crédito. */
    private String contaCreditoCode;

    /** Data do lançamento (formato ISO 8601). */
    private String data;

    /** Valor do lançamento. */
    private String valor;

    /** Histórico do lançamento. */
    private String historico;

    /** Número do documento (opcional). */
    private String numeroDocumento;
  }
}
