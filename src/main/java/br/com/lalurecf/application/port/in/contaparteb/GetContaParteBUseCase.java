package br.com.lalurecf.application.port.in.contaparteb;

import br.com.lalurecf.infrastructure.dto.contaparteb.ContaParteBResponse;

/**
 * Port IN para caso de uso de consulta de conta da Parte B (e-Lalur/e-Lacs) por ID.
 *
 * <p>Define contrato para busca de conta fiscal específica.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface GetContaParteBUseCase {

  /**
   * Busca conta da Parte B por ID.
   *
   * @param id ID da conta Parte B
   * @return dados da conta encontrada
   * @throws br.com.lalurecf.infrastructure.exception.ResourceNotFoundException se conta não existir
   */
  ContaParteBResponse getContaParteBById(Long id);
}
