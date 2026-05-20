package br.com.lalurecf.application.port.in.company;

import br.com.lalurecf.infrastructure.dto.company.TimelineResponse;

/**
 * Use case para obter timeline agregada de parâmetros tributários de uma empresa.
 *
 * <p>Agrupa parâmetros por tipo e mostra os períodos em que cada um está ativo.
 */
public interface GetCompanyTaxParametersTimelineUseCase {

  /**
   * Obtém timeline de todos os parâmetros tributários de uma empresa em um ano específico.
   *
   * <p>Retorna mapa agrupado por tipo de parâmetro, com lista de períodos ativos para cada
   * parâmetro.
   *
   * @param companyId ID da empresa
   * @param ano ano fiscal
   * @return timeline agrupada por tipo de parâmetro
   * @throws jakarta.persistence.EntityNotFoundException se empresa não existir
   */
  TimelineResponse getTimeline(Long companyId, Integer ano);
}
