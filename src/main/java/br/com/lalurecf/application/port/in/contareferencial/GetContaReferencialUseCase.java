package br.com.lalurecf.application.port.in.contareferencial;

import br.com.lalurecf.infrastructure.dto.contareferencial.ContaReferencialResponse;

/**
 * Port IN para caso de uso de consulta de conta referencial RFB por ID.
 *
 * <p>Define contrato para obter uma conta referencial específica.
 */
public interface GetContaReferencialUseCase {

  /**
   * Obtém conta referencial por ID.
   *
   * @param id ID da conta referencial
   * @return dados da conta referencial
   * @throws br.com.lalurecf.infrastructure.exception.ResourceNotFoundException se conta não
   *     encontrada
   */
  ContaReferencialResponse getContaReferencialById(Long id);
}
