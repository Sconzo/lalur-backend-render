package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.ecf.FinalizeEcfFileUseCase;
import br.com.lalurecf.application.port.out.EcfFileRepositoryPort;
import br.com.lalurecf.domain.enums.EcfFileStatus;
import br.com.lalurecf.domain.enums.EcfFileType;
import br.com.lalurecf.domain.model.EcfFile;
import br.com.lalurecf.infrastructure.dto.ecf.FinalizeEcfFileResponse;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável pela finalização de arquivos ECF Completo.
 *
 * <p>Valida que o arquivo é COMPLETE_ECF com status VALIDATED antes de marcar como FINALIZED.
 * Registra auditoria com userId e timestamp da finalização.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinalizeEcfFileService implements FinalizeEcfFileUseCase {

  private final EcfFileRepositoryPort ecfFileRepositoryPort;

  @Override
  @Transactional
  public FinalizeEcfFileResponse finalize(Long ecfFileId, Long companyId, String userId) {
    log.info("Finalizando EcfFile: id={}, companyId={}", ecfFileId, companyId);

    EcfFile ecfFile = ecfFileRepositoryPort.findById(ecfFileId)
        .orElseThrow(() -> new EntityNotFoundException(
            "EcfFile não encontrado: " + ecfFileId));

    if (!ecfFile.getCompanyId().equals(companyId)) {
      throw new AccessDeniedException("Arquivo não pertence à empresa informada");
    }

    if (ecfFile.getFileType() != EcfFileType.COMPLETE_ECF) {
      throw new IllegalArgumentException("Apenas o ECF Completo pode ser finalizado");
    }

    if (ecfFile.getFileStatus() != EcfFileStatus.VALIDATED) {
      throw new IllegalArgumentException("Apenas arquivos validados podem ser finalizados");
    }

    ecfFile.setFileStatus(EcfFileStatus.FINALIZED);
    ecfFile.setGeneratedBy(userId);
    ecfFile.setGeneratedAt(LocalDateTime.now());
    ecfFileRepositoryPort.saveOrReplace(ecfFile);

    log.info("EcfFile finalizado: id={}, fileType={}", ecfFileId, ecfFile.getFileType());

    return new FinalizeEcfFileResponse(
        true,
        "ECF Completo finalizado com sucesso",
        EcfFileStatus.FINALIZED.name(),
        ecfFile.getFileType().name());
  }
}
