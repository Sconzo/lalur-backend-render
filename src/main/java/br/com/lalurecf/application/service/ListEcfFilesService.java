package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.ecf.ListEcfFilesUseCase;
import br.com.lalurecf.application.port.out.EcfFileRepositoryPort;
import br.com.lalurecf.domain.enums.EcfFileStatus;
import br.com.lalurecf.domain.enums.EcfFileType;
import br.com.lalurecf.domain.model.EcfFile;
import br.com.lalurecf.infrastructure.dto.ecf.EcfFileListResponse;
import br.com.lalurecf.infrastructure.dto.ecf.EcfFileSummary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de listagem de arquivos ECF por empresa e ano fiscal.
 *
 * <p>Busca todos os EcfFile da empresa+ano, separa por tipo, e mapeia para DTOs de sumário.
 * O campo {@code content} não é exposto no response (somente metadados).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ListEcfFilesService implements ListEcfFilesUseCase {

  private final EcfFileRepositoryPort ecfFileRepositoryPort;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional(readOnly = true)
  public EcfFileListResponse list(Long companyId, Integer fiscalYear, String fileType) {
    log.info("Listando arquivos ECF: companyId={}, fiscalYear={}, fileType={}",
        companyId, fiscalYear, fileType);

    List<EcfFile> files = ecfFileRepositoryPort.findByCompanyAndFiscalYear(companyId, fiscalYear);

    EcfFileSummary parcial = null;
    EcfFileSummary importado = null;
    EcfFileSummary completo = null;

    for (EcfFile file : files) {
      EcfFileSummary summary = toSummary(file);
      switch (file.getFileType()) {
        case ARQUIVO_PARCIAL -> parcial = summary;
        case IMPORTED_ECF -> importado = summary;
        case COMPLETE_ECF -> completo = summary;
        default -> log.warn("Tipo de arquivo ECF desconhecido: {}", file.getFileType());
      }
    }

    EcfFileListResponse response = new EcfFileListResponse();
    response.setArquivoParcial(parcial);
    response.setEcfImportado(importado);
    response.setEcfCompleto(completo);

    applyFileTypeFilter(response, fileType);

    return response;
  }

  private void applyFileTypeFilter(EcfFileListResponse response, String fileType) {
    if (fileType == null || fileType.isBlank()) {
      return;
    }
    try {
      EcfFileType.valueOf(fileType);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "fileType inválido: '" + fileType + "'. Valores aceitos: ARQUIVO_PARCIAL, "
              + "IMPORTED_ECF, COMPLETE_ECF");
    }
    if (!EcfFileType.ARQUIVO_PARCIAL.name().equals(fileType)) {
      response.setArquivoParcial(null);
    }
    if (!EcfFileType.IMPORTED_ECF.name().equals(fileType)) {
      response.setEcfImportado(null);
    }
    if (!EcfFileType.COMPLETE_ECF.name().equals(fileType)) {
      response.setEcfCompleto(null);
    }
  }

  private EcfFileSummary toSummary(EcfFile file) {
    EcfFileSummary summary = new EcfFileSummary();
    summary.setId(file.getId());
    summary.setFiscalYear(file.getFiscalYear());
    summary.setFileType(file.getFileType() != null ? file.getFileType().name() : null);
    summary.setFileName(file.getFileName());
    summary.setFileSizeBytes(
        file.getContent() != null ? (long) file.getContent().length() : 0L);
    summary.setFileStatus(file.getFileStatus() != null ? file.getFileStatus().name() : null);
    summary.setGeneratedAt(file.getGeneratedAt());
    summary.setGeneratedBy(file.getGeneratedBy());
    summary.setSourceImportedEcfId(file.getSourceImportedEcfId());
    summary.setSourceParcialFileId(file.getSourceParcialFileId());
    summary.setValidationErrors(deserializeErrors(file));
    return summary;
  }

  private List<String> deserializeErrors(EcfFile file) {
    if (file.getFileStatus() != EcfFileStatus.ERROR
        || file.getValidationErrors() == null
        || file.getValidationErrors().isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(
          file.getValidationErrors(), new TypeReference<List<String>>() {});
    } catch (Exception e) {
      log.warn("Falha ao desserializar validationErrors do arquivo {}: {}", file.getId(),
          e.getMessage());
      return null;
    }
  }
}
