package br.com.lalurecf.infrastructure.dto.lancamentoparteb;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta para importação de lançamentos da Parte B via CSV/TXT.
 *
 * <p>Contém relatório detalhado do processamento incluindo estatísticas e lista de erros por
 * linha.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class ImportLancamentoParteBResponse {

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

  /** Preview dos lançamentos que seriam criados (apenas se dryRun=true). */
  private List<LancamentoParteBPreview> preview;

  /** Classe interna para representar erro em linha do CSV. */
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

  /** Classe interna para preview de lançamento Parte B (dry run). */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  public static class LancamentoParteBPreview {

    private String mesReferencia;
    private String anoReferencia;
    private String tipoApuracao;
    private String tipoRelacionamento;
    private String contaContabilCode;
    private String contaParteBCode;
    private String parametroTributarioCodigo;
    private String tipoAjuste;
    private String descricao;
    private String valor;
  }
}
