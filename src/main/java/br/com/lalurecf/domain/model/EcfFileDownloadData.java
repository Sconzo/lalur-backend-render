package br.com.lalurecf.domain.model;

/**
 * Dado de domínio para download de arquivo ECF.
 *
 * <p>Encapsula o conteúdo em bytes (ISO-8859-1), nome e tamanho do arquivo
 * para entrega ao controller como ResponseEntity.
 */
public class EcfFileDownloadData {

  private final byte[] content;
  private final String fileName;
  private final long fileSizeBytes;

  /**
   * Cria EcfFileDownloadData com todos os campos obrigatórios.
   *
   * @param content bytes do arquivo em encoding ISO-8859-1
   * @param fileName nome do arquivo para o header Content-Disposition
   * @param fileSizeBytes tamanho do arquivo em bytes
   */
  public EcfFileDownloadData(byte[] content, String fileName, long fileSizeBytes) {
    this.content = content;
    this.fileName = fileName;
    this.fileSizeBytes = fileSizeBytes;
  }

  public byte[] getContent() {
    return content;
  }

  public String getFileName() {
    return fileName;
  }

  public long getFileSizeBytes() {
    return fileSizeBytes;
  }
}
