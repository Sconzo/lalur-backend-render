package br.com.lalurecf.application.port.in.contareferencial;

import br.com.lalurecf.infrastructure.dto.contareferencial.ContaReferencialResponse;
import br.com.lalurecf.infrastructure.dto.contareferencial.UpdateContaReferencialRequest;

/**
 * Port IN para caso de uso de atualização de conta referencial RFB.
 *
 * <p>Define contrato para editar descrição e ano de validade. Código RFB não pode ser alterado.
 */
public interface UpdateContaReferencialUseCase {

  /**
   * Atualiza uma conta referencial.
   *
   * @param id ID da conta a atualizar
   * @param request dados atualizados
   * @return conta referencial atualizada
   * @throws br.com.lalurecf.infrastructure.exception.ResourceNotFoundException se conta não
   *     encontrada
   */
  ContaReferencialResponse updateContaReferencial(Long id, UpdateContaReferencialRequest request);
}
