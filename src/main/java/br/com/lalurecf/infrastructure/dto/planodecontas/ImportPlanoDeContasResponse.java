package br.com.lalurecf.infrastructure.dto.planodecontas;

import br.com.lalurecf.domain.enums.AccountType;
import br.com.lalurecf.domain.enums.ClasseContabil;
import br.com.lalurecf.domain.enums.NaturezaConta;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta para importação de plano de contas via CSV.
 *
 * <p>Contém relatório detalhado da importação: total de linhas, linhas processadas, erros e
 * preview (modo dry-run).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportPlanoDeContasResponse {

  /** Indica se a importação foi bem-sucedida. */
  private boolean success;

  /** Mensagem geral sobre o resultado da importação. */
  private String message;

  /** Total de linhas no arquivo (excluindo header). */
  private int totalLines;

  /** Número de linhas processadas com sucesso. */
  private int processedLines;

  /** Número de linhas ignoradas (por erro ou duplicata). */
  private int skippedLines;

  /** Lista de erros encontrados durante a importação (linha + mensagem). */
  @Builder.Default private List<ImportError> errors = new ArrayList<>();

  /**
   * Preview das contas que seriam criadas (apenas em modo dry-run).
   *
   * <p>Null se dry-run = false.
   */
  private List<PlanoDeContasPreview> preview;

  /**
   * Representa um erro encontrado durante a importação.
   *
   * <p>Contém o número da linha e a mensagem de erro.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ImportError {
    /** Número da linha no arquivo onde ocorreu o erro (1-based). */
    private int lineNumber;

    /** Mensagem descritiva do erro. */
    private String error;
  }

  /**
   * Preview de uma conta contábil que seria criada (modo dry-run).
   *
   * <p>Usado para mostrar ao usuário o que será importado antes de persistir.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PlanoDeContasPreview {
    private String code;
    private String name;
    private Integer fiscalYear;
    private AccountType accountType;
    private String contaReferencialCodigo;
    private ClasseContabil classe;
    private Integer nivel;
    private NaturezaConta natureza;
    private Boolean afetaResultado;
    private Boolean dedutivel;
  }
}
