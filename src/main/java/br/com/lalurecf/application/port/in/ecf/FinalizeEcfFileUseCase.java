package br.com.lalurecf.application.port.in.ecf;

import br.com.lalurecf.infrastructure.dto.ecf.FinalizeEcfFileResponse;

/**
 * Use case para finalização de arquivo ECF Completo.
 *
 * <p>Marca o COMPLETE_ECF como FINALIZED, indicando que foi transmitido ao SPED.
 * Apenas arquivos do tipo COMPLETE_ECF com status VALIDATED podem ser finalizados.
 */
public interface FinalizeEcfFileUseCase {

  /**
   * Finaliza um arquivo ECF Completo.
   *
   * @param ecfFileId ID do arquivo ECF a finalizar
   * @param companyId ID da empresa (para verificar ownership)
   * @param userId    identificador do usuário autenticado (auditoria)
   * @return DTO com confirmação e novo status
   */
  FinalizeEcfFileResponse finalize(Long ecfFileId, Long companyId, String userId);
}
