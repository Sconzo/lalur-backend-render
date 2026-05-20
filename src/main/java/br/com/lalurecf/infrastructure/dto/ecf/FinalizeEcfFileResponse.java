package br.com.lalurecf.infrastructure.dto.ecf;

/**
 * DTO de resposta para finalização de arquivo ECF.
 */
public class FinalizeEcfFileResponse {

  private boolean success;
  private String message;
  private String newStatus;
  private String fileType;

  /**
   * Construtor completo.
   *
   * @param success   true se a finalização foi bem-sucedida
   * @param message   mensagem descritiva do resultado
   * @param newStatus novo status do arquivo (FINALIZED)
   * @param fileType  tipo do arquivo finalizado
   */
  public FinalizeEcfFileResponse(
      boolean success, String message, String newStatus, String fileType) {
    this.success = success;
    this.message = message;
    this.newStatus = newStatus;
    this.fileType = fileType;
  }

  public FinalizeEcfFileResponse() {
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getNewStatus() {
    return newStatus;
  }

  public void setNewStatus(String newStatus) {
    this.newStatus = newStatus;
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }
}
