package br.com.lalurecf.application.port.in.ecf;

import br.com.lalurecf.infrastructure.dto.ecf.ValidationResult;

/**
 * Port IN para validação de arquivo ECF.
 *
 * <p>Delega ao método correto do EcfValidatorService conforme o tipo do arquivo,
 * e atualiza o fileStatus (VALIDATED ou ERROR) no banco.
 */
public interface ValidateEcfFileUseCase {

  /**
   * Valida o arquivo ECF pelo ID, verifica ownership e atualiza fileStatus.
   *
   * @param ecfFileId ID do arquivo ECF a validar
   * @param companyId ID da empresa (para verificar ownership)
   * @return resultado da validação com erros e avisos
   * @throws jakarta.persistence.EntityNotFoundException se arquivo não encontrado
   * @throws org.springframework.security.access.AccessDeniedException se arquivo não pertence
   *     à empresa
   */
  ValidationResult validate(Long ecfFileId, Long companyId);
}
