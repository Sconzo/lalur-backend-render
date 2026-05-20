package br.com.lalurecf.application.port.out;

import java.util.Optional;

/**
 * Port para busca de dados de empresas por CNPJ em API externa.
 *
 * <p>Define o contrato para integração com serviços externos de consulta CNPJ
 * (ex: BrasilAPI, ReceitaWS). Segue princípios de Hexagonal Architecture,
 * mantendo o core da aplicação independente de implementações externas.
 *
 * <p>Implementações devem:
 * <ul>
 *   <li>Realizar validação de formato do CNPJ
 *   <li>Tratar erros de rede/timeout graciosamente
 *   <li>Retornar {@code Optional.empty()} quando CNPJ não encontrado ou erro
 *   <li>Implementar cache quando apropriado
 * </ul>
 */
public interface CnpjSearchPort {

  /**
   * Busca dados de uma empresa por CNPJ em API externa.
   *
   * @param cnpj CNPJ da empresa (14 dígitos, apenas números)
   * @return {@code Optional} contendo dados da empresa se encontrada,
   *         ou {@code Optional.empty()} se não encontrada ou erro
   */
  Optional<CnpjData> searchByCnpj(String cnpj);
}
