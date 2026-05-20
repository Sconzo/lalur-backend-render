package br.com.lalurecf.application.port.in.company;

import br.com.lalurecf.infrastructure.dto.company.UpdateTaxParametersRequestV2;
import br.com.lalurecf.infrastructure.dto.company.UpdateTaxParametersResponse;

/**
 * Use case para atualização de parâmetros tributários de uma empresa.
 *
 * <p>Suporta dois tipos de parâmetros tributários:
 * <ul>
 *   <li><b>Globais:</b> Aplicam-se ao ano inteiro (ex: CNAE, Qualificação PJ)
 *   <li><b>Periódicos:</b> Precisam de mês/trimestre específico (mudam durante o ano)
 * </ul>
 *
 * <p>A lista fornecida substitui completamente a lista anterior (operação atômica).
 */
public interface UpdateCompanyTaxParametersUseCase {

  /**
   * Atualiza parâmetros tributários de uma empresa (suporta periódicos e globais).
   *
   * <p>IMPORTANTE: A lista fornecida substitui completamente a lista anterior.
   *
   * @param companyId ID da empresa
   * @param request parâmetros globais e periódicos com valores temporais
   * @return resposta com a lista atualizada de parâmetros
   * @throws jakarta.persistence.EntityNotFoundException se empresa não existir
   * @throws br.com.lalurecf.domain.exception.BusinessRuleViolationException se algum parâmetro não
   *     existir ou estiver INACTIVE
   */
  UpdateTaxParametersResponse updateTaxParameters(
      Long companyId, UpdateTaxParametersRequestV2 request);
}
