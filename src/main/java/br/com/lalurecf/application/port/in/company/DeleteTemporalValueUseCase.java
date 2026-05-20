package br.com.lalurecf.application.port.in.company;

/**
 * Use case para deleção de valor temporal de parâmetro tributário.
 */
public interface DeleteTemporalValueUseCase {

  /**
   * Deleta um valor temporal específico.
   *
   * @param companyId ID da empresa
   * @param taxParameterId ID do parâmetro tributário
   * @param valorId ID do valor temporal
   * @throws jakarta.persistence.EntityNotFoundException se valor temporal não existir
   */
  void deleteTemporalValue(Long companyId, Long taxParameterId, Long valorId);
}
