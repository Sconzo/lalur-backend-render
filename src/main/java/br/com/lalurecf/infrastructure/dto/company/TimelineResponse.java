package br.com.lalurecf.infrastructure.dto.company;

import java.util.List;
import java.util.Map;

/**
 * Response DTO para timeline de parâmetros tributários de uma empresa.
 *
 * <p>Agrupa parâmetros por tipo e mostra os períodos em que cada um está ativo.
 *
 * @param ano ano fiscal
 * @param timeline mapa de tipo de parâmetro para lista de parâmetros com períodos
 */
public record TimelineResponse(Integer ano, Map<String, List<ParameterTimeline>> timeline) {

  /**
   * Representa um parâmetro tributário com seus períodos ativos.
   *
   * @param codigo código do parâmetro
   * @param descricao descrição do parâmetro
   * @param periodos lista de períodos formatados (ex: ["Jan/2024", "Fev/2024"])
   */
  public record ParameterTimeline(String codigo, String descricao, List<String> periodos) {}
}
