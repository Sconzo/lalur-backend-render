package br.com.lalurecf.application.port.in.company;

import br.com.lalurecf.infrastructure.dto.company.CreateTemporalValueRequest;
import br.com.lalurecf.infrastructure.dto.company.TemporalValueResponse;

/**
 * Use case para criação de valor temporal de parâmetro tributário.
 *
 * <p>Valida que a associação empresa-parâmetro existe e cria o registro temporal.
 */
public interface CreateTemporalValueUseCase {

  /**
   * Cria um valor temporal para um parâmetro tributário de uma empresa.
   *
   * @param companyId ID da empresa
   * @param taxParameterId ID do parâmetro tributário
   * @param request dados do período (ano + mes OU trimestre)
   * @return resposta com dados do valor temporal criado
   * @throws jakarta.persistence.EntityNotFoundException se associação não existir
   * @throws IllegalStateException se constraint XOR for violado (ambos ou nenhum preenchido)
   * @throws IllegalArgumentException se valores estiverem fora do range permitido
   * @throws org.springframework.dao.DataIntegrityViolationException se duplicata (unique
   *     constraint)
   */
  TemporalValueResponse createTemporalValue(
      Long companyId, Long taxParameterId, CreateTemporalValueRequest request);
}
