package br.com.lalurecf.application.port.in.ecf;

import br.com.lalurecf.infrastructure.dto.ecf.GenerateCompleteEcfResponse;

/**
 * Port IN para geração do ECF Completo via merge.
 *
 * <p>Mescla o IMPORTED_ECF com o ARQUIVO_PARCIAL usando algoritmo de merge por chave
 * (codigoApuracao + codigoEnquadramento) para produzir o COMPLETE_ECF final.
 */
public interface GenerateCompleteEcfUseCase {

  /**
   * Gera o ECF Completo fazendo merge do ECF Importado com o Arquivo Parcial.
   *
   * @param fiscalYear ano fiscal de referência (do contexto X-Fiscal-Year)
   * @param companyId ID da empresa
   * @param generatedBy identificador do usuário autenticado
   * @return metadados do ECF Completo gerado
   * @throws IllegalArgumentException se IMPORTED_ECF ou ARQUIVO_PARCIAL não existirem
   */
  GenerateCompleteEcfResponse generate(
      Integer fiscalYear, Long companyId, String generatedBy);
}
