package br.com.lalurecf.application.port.in;

import br.com.lalurecf.infrastructure.dto.lancamentocontabil.ImportLancamentoContabilResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Use case para importação de lançamentos contábeis via arquivo CSV/TXT.
 *
 * <p>Permite importar lançamentos contábeis em massa com partidas dobradas, validando cada linha e
 * retornando relatório detalhado.
 *
 * <p>Funcionalidades:
 *
 * <ul>
 *   <li>Parsing de arquivo CSV/TXT com auto-detecção de separador (; ou ,)
 *   <li>Validação de contas (devem existir no plano de contas da empresa)
 *   <li>Validação de partidas dobradas (débito != crédito)
 *   <li>Validação de Período Contábil (data >= company.periodoContabil)
 *   <li>Modo dry-run para preview sem persistir
 * </ul>
 */
public interface ImportLancamentoContabilUseCase {

  /**
   * Importa lançamentos contábeis de arquivo CSV/TXT.
   *
   * @param file arquivo CSV/TXT com lançamentos (max 50MB)
   * @param companyId ID da empresa (obtido via CompanyContext)
   * @param fiscalYear ano fiscal dos lançamentos
   * @param dryRun se true, apenas retorna preview sem persistir
   * @return relatório detalhado da importação
   */
  ImportLancamentoContabilResponse importLancamentos(
      MultipartFile file, Long companyId, Integer fiscalYear, boolean dryRun);
}
