package br.com.lalurecf.infrastructure.dto.ecf;

/**
 * Response DTO para geração do ECF Completo.
 *
 * <p>Retorna metadados do ECF Completo gerado via merge, incluindo IDs dos arquivos-fonte.
 */
public class GenerateCompleteEcfResponse {

  private boolean success;
  private String message;
  private Long ecfFileId;
  private String fileName;
  private Long fileSizeBytes;
  private Long sourceImportedEcfId;
  private Long sourceParcialFileId;
  private Integer totalLinhas;

  public GenerateCompleteEcfResponse() {
  }

  /**
   * Cria response com todos os campos.
   *
   * @param success indica se a geração foi bem-sucedida
   * @param message mensagem descritiva do resultado
   * @param ecfFileId ID do arquivo ECF Completo persistido
   * @param fileName nome do arquivo gerado
   * @param fileSizeBytes tamanho do conteúdo em bytes
   * @param sourceImportedEcfId ID do ECF Importado usado como base
   * @param sourceParcialFileId ID do Arquivo Parcial usado como base
   * @param totalLinhas total de linhas do bloco M no resultado
   */
  public GenerateCompleteEcfResponse(
      boolean success, String message, Long ecfFileId, String fileName,
      Long fileSizeBytes, Long sourceImportedEcfId, Long sourceParcialFileId,
      Integer totalLinhas) {
    this.success = success;
    this.message = message;
    this.ecfFileId = ecfFileId;
    this.fileName = fileName;
    this.fileSizeBytes = fileSizeBytes;
    this.sourceImportedEcfId = sourceImportedEcfId;
    this.sourceParcialFileId = sourceParcialFileId;
    this.totalLinhas = totalLinhas;
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

  public Long getFileSizeBytes() {
    return fileSizeBytes;
  }

  public Long getSourceImportedEcfId() {
    return sourceImportedEcfId;
  }

  public Long getSourceParcialFileId() {
    return sourceParcialFileId;
  }

  public Integer getTotalLinhas() {
    return totalLinhas;
  }
}
