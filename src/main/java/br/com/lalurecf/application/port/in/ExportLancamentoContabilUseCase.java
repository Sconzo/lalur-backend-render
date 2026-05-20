package br.com.lalurecf.application.port.in;

import java.time.LocalDate;

/**
 * Use case para exportação de lançamentos contábeis para arquivo CSV.
 *
 * <p>Permite exportar lançamentos contábeis em formato CSV para backup, análises externas ou
 * compartilhamento com outros sistemas.
 *
 * <p>Funcionalidades:
 *
 * <ul>
 *   <li>Exportação de todos lançamentos de uma empresa/ano fiscal
 *   <li>Filtro opcional por range de data
 *   <li>Formato CSV com encoding UTF-8
 *   <li>Ordenação por data ASC
 * </ul>
 */
public interface ExportLancamentoContabilUseCase {

  /**
   * Exporta lançamentos contábeis para arquivo CSV.
   *
   * @param companyId ID da empresa (obtido via CompanyContext)
   * @param fiscalYear ano fiscal dos lançamentos
   * @param dataInicio data inicial do filtro (opcional)
   * @param dataFim data final do filtro (opcional)
   * @return conteúdo do arquivo CSV como String
   */
  String exportLancamentos(
      Long companyId, Integer fiscalYear, LocalDate dataInicio, LocalDate dataFim);
}
