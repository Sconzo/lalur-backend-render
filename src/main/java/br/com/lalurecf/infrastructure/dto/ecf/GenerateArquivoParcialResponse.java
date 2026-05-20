package br.com.lalurecf.infrastructure.dto.ecf;

/**
 * Response DTO para geração do Arquivo Parcial ECF.
 *
 * <p>Retorna metadados da geração: ID do arquivo, nome, contagem de períodos e lançamentos.
 */
public class GenerateArquivoParcialResponse {

  private boolean success;
  private String message;
  private Long ecfFileId;
  private String fileName;

  /**Número de registros M030 gerados (períodos mensais). */
  private Integer periodoCount;

  /**Total de lançamentos processados. */
  private Integer lancamentosCount;

  public GenerateArquivoParcialResponse() {
  }

  /**
   * Cria response com todos os campos.
   *
   * @param success indica se a geração foi bem-sucedida
   * @param message mensagem descritiva do resultado
   * @param ecfFileId ID do arquivo ECF persistido
   * @param fileName nome do arquivo gerado
   * @param periodoCount número de períodos mensais (M030) gerados
   * @param lancamentosCount total de lançamentos processados
   */
  public GenerateArquivoParcialResponse(
      boolean success, String message, Long ecfFileId, String fileName,
      Integer periodoCount, Integer lancamentosCount) {
    this.success = success;
    this.message = message;
    this.ecfFileId = ecfFileId;
    this.fileName = fileName;
    this.periodoCount = periodoCount;
    this.lancamentosCount = lancamentosCount;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }

  public Long getEcfFileId() {
    return ecfFileId;
  }

  public String getFileName() {
    return fileName;
  }

  public Integer getPeriodoCount() {
    return periodoCount;
  }

  public Integer getLancamentosCount() {
    return lancamentosCount;
  }
}
