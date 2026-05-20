package br.com.lalurecf.application.port.in.company;

import br.com.lalurecf.infrastructure.dto.company.CompanyDetailResponse;
import br.com.lalurecf.infrastructure.dto.company.CreateCompanyRequest;

/**
 * Use case para criação de empresa.
 *
 * <p>Valida:
 * <ul>
 *   <li>CNPJ válido (formato e dígitos verificadores)</li>
 *   <li>CNPJ único (não pode já existir empresa ACTIVE com mesmo CNPJ)</li>
 *   <li>Período Contábil não pode ser no futuro</li>
 *   <li>Parâmetros tributários devem existir (se fornecidos)</li>
 * </ul>
 */
public interface CreateCompanyUseCase {

  /**
   * Cria uma nova empresa no sistema.
   *
   * @param request dados da empresa a ser criada
   * @return empresa criada com todos os detalhes
   * @throws IllegalArgumentException se CNPJ inválido ou já existir
   */
  CompanyDetailResponse create(CreateCompanyRequest request);
}
