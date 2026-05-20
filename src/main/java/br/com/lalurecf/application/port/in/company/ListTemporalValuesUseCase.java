package br.com.lalurecf.application.port.in.company;

import br.com.lalurecf.infrastructure.dto.company.TemporalValueResponse;
import java.util.List;

/**
 * Use case para listagem de valores temporais de um parâmetro tributário.
 *
 * <p>Permite filtrar por ano (opcional).
 */
public interface ListTemporalValuesUseCase {

  /**
   * Lista todos os valores temporais de um parâmetro tributário de uma empresa.
   *
   * @param companyId ID da empresa
   * @param taxParameterId ID do parâmetro tributário
   * @param ano ano fiscal (opcional, null para listar todos)
   * @return lista de valores temporais (vazia se não encontrar)
   * @throws jakarta.persistence.EntityNotFoundException se associação não existir
   */
  List<TemporalValueResponse> listTemporalValues(
      Long companyId, Long taxParameterId, Integer ano);
}
