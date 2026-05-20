package br.com.lalurecf.infrastructure.dto.ecf;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de resumo de arquivo ECF para listagem.
 *
 * <p>Não inclui o campo {@code content} (que pode ser vários MB).
 * O download é feito via endpoint dedicado (GET /ecf/{id}/download).
 */
public class EcfFileSummary {

  private Long id;
  private Integer fiscalYear;
  private String fileType;
  private String fileName;
  private Long fileSizeBytes;
  private String fileStatus;
  private LocalDateTime generatedAt;
  private String generatedBy;

  /**Lista de erros de validação — presente apenas se fileStatus = ERROR. */
  private List<String> validationErrors;

  /**ID do ECF Importado base — presente apenas se COMPLETE_ECF. */
  private Long sourceImportedEcfId;

  /**ID do Arquivo Parcial base — presente apenas se COMPLETE_ECF. */
  private Long sourceParcialFileId;

  public EcfFileSummary() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Integer getFiscalYear() {
    return fiscalYear;
  }

  public void setFiscalYear(Integer fiscalYear) {
    this.fiscalYear = fiscalYear;
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public Long getFileSizeBytes() {
    return fileSizeBytes;
  }

  public void setFileSizeBytes(Long fileSizeBytes) {
    this.fileSizeBytes = fileSizeBytes;
  }

  public String getFileStatus() {
    return fileStatus;
  }

  public void setFileStatus(String fileStatus) {
    this.fileStatus = fileStatus;
  }

  public LocalDateTime getGeneratedAt() {
    return generatedAt;
  }

  public void setGeneratedAt(LocalDateTime generatedAt) {
    this.generatedAt = generatedAt;
  }

  public String getGeneratedBy() {
    return generatedBy;
  }

  public void setGeneratedBy(String generatedBy) {
    this.generatedBy = generatedBy;
  }

  public List<String> getValidationErrors() {
    return validationErrors;
  }

  public void setValidationErrors(List<String> validationErrors) {
    this.validationErrors = validationErrors;
  }

  public Long getSourceImportedEcfId() {
    return sourceImportedEcfId;
  }

  public void setSourceImportedEcfId(Long sourceImportedEcfId) {
    this.sourceImportedEcfId = sourceImportedEcfId;
  }

  public Long getSourceParcialFileId() {
    return sourceParcialFileId;
  }

  public void setSourceParcialFileId(Long sourceParcialFileId) {
    this.sourceParcialFileId = sourceParcialFileId;
  }
}
