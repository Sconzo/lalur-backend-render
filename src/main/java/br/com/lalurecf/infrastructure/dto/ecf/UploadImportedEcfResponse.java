package br.com.lalurecf.infrastructure.dto.ecf;

/**
 * Response DTO para upload do ECF Importado.
 *
 * <p>Retorna metadados do arquivo armazenado após upload bem-sucedido.
 */
public class UploadImportedEcfResponse {

  private boolean success;
  private String message;
  private Long ecfFileId;
  private String fileName;
  private Long fileSizeBytes;
  private Integer lineCount;

  public UploadImportedEcfResponse() {
  }

  /**
   * Cria response com todos os campos.
   *
   * @param success indica se o upload foi bem-sucedido
   * @param message mensagem descritiva do resultado
   * @param ecfFileId ID do arquivo ECF persistido
   * @param fileName nome do arquivo armazenado
   * @param fileSizeBytes tamanho do arquivo em bytes
   * @param lineCount número de linhas do arquivo
   */
  public UploadImportedEcfResponse(
      boolean success, String message, Long ecfFileId, String fileName,
      Long fileSizeBytes, Integer lineCount) {
    this.success = success;
    this.message = message;
    this.ecfFileId = ecfFileId;
    this.fileName = fileName;
    this.fileSizeBytes = fileSizeBytes;
    this.lineCount = lineCount;
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

  public Integer getLineCount() {
    return lineCount;
  }
}
