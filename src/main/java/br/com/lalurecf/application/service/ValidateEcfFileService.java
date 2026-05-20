package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.ecf.ValidateEcfFileUseCase;
import br.com.lalurecf.application.port.out.EcfFileRepositoryPort;
import br.com.lalurecf.domain.enums.EcfFileStatus;
import br.com.lalurecf.domain.model.EcfFile;
import br.com.lalurecf.infrastructure.dto.ecf.ValidationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por orquestrar a validação de arquivos ECF.
 *
 * <p>Busca o EcfFile, verifica ownership, delega ao EcfValidatorService
 * e persiste o resultado (fileStatus + validationErrors).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidateEcfFileService implements ValidateEcfFileUseCase {

  private final EcfFileRepositoryPort ecfFileRepositoryPort;
  private final EcfValidatorService ecfValidatorService;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public ValidationResult validate(Long ecfFileId, Long companyId) {
    log.info("Validando EcfFile: id={}, companyId={}", ecfFileId, companyId);

    EcfFile ecfFile = ecfFileRepositoryPort.findById(ecfFileId)
        .orElseThrow(() -> new EntityNotFoundException(
            "EcfFile não encontrado: " + ecfFileId));

    if (!ecfFile.getCompanyId().equals(companyId)) {
      throw new AccessDeniedException("Arquivo ECF não pertence à empresa informada");
    }

    ValidationResult result = switch (ecfFile.getFileType()) {
      case ARQUIVO_PARCIAL -> ecfValidatorService.validateArquivoParcial(ecfFile.getContent());
      case IMPORTED_ECF -> ecfValidatorService.validateImportedEcf(ecfFile.getContent());
      case COMPLETE_ECF -> ecfValidatorService.validateCompleteEcf(ecfFile.getContent());
    };

    // Atualizar fileStatus e validationErrors
    if (result.isValid()) {
      ecfFile.setFileStatus(EcfFileStatus.VALIDATED);
      ecfFile.setValidationErrors(null);
    } else {
      ecfFile.setFileStatus(EcfFileStatus.ERROR);
      ecfFile.setValidationErrors(serializeErrors(result.getErrors()));
    }

    ecfFileRepositoryPort.saveOrReplace(ecfFile);
    log.info("EcfFile {} validado: valid={}, errors={}, warnings={}",
        ecfFileId, result.isValid(), result.getErrors().size(), result.getWarnings().size());

    return result;
  }

  private String serializeErrors(java.util.List<String> errors) {
    try {
      return objectMapper.writeValueAsString(errors);
    } catch (JsonProcessingException e) {
      log.warn("Falha ao serializar erros de validação: {}", e.getMessage());
      return "[\"Erro ao serializar erros de validação\"]";
    }
  }
}
