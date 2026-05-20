package br.com.lalurecf.application.port.in.company;

import br.com.lalurecf.infrastructure.dto.company.UpdatePeriodoContabilRequest;
import br.com.lalurecf.infrastructure.dto.company.UpdatePeriodoContabilResponse;

/**
 * Use case para atualização do Período Contábil de uma empresa.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Validar que nova data não é no futuro
 *   <li>Validar que nova data é posterior à data atual (não pode retroagir)
 *   <li>Registrar alteração em log de auditoria
 *   <li>Atualizar campo periodoContabil da empresa
 * </ul>
 */
public interface UpdatePeriodoContabilUseCase {

  /**
   * Atualiza o Período Contábil de uma empresa.
   *
   * @param companyId ID da empresa
   * @param request dados da atualização (novo período contábil)
   * @return resposta com sucesso e dados anterior/novo
   * @throws IllegalArgumentException se nova data for no futuro ou retroagir
   * @throws jakarta.persistence.EntityNotFoundException se empresa não existir
   */
  UpdatePeriodoContabilResponse update(Long companyId, UpdatePeriodoContabilRequest request);
}
