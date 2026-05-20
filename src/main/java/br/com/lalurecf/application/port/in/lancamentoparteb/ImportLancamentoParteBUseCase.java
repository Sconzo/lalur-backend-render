package br.com.lalurecf.application.port.in.lancamentoparteb;

import br.com.lalurecf.infrastructure.dto.lancamentoparteb.ImportLancamentoParteBResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Use case para importação de lançamentos da Parte B via arquivo CSV/TXT.
 *
 * <p>Permite importar lançamentos da Parte B em massa, validando cada linha e retornando relatório
 * detalhado.
 *
 * <p>Formato CSV esperado:
 * mesReferencia;anoReferencia;tipoApuracao;tipoRelacionamento;contaContabilCode;contaParteBCode;
 * parametroTributarioCodigo;tipoAjuste;descricao;valor
 *
 * <p>Funcionalidades:
 *
 * <ul>
 *   <li>Parsing de arquivo CSV/TXT com auto-detecção de separador (; ou ,)
 *   <li>Validação de contas contábeis e contas Parte B por código
 *   <li>Validação de parâmetro tributário por código
 *   <li>Validação condicional de FKs conforme tipoRelacionamento
 *   <li>Modo dry-run para preview sem persistir
 * </ul>
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface ImportLancamentoParteBUseCase {

  /**
   * Importa lançamentos da Parte B de arquivo CSV/TXT.
   *
   * @param file arquivo CSV/TXT com lançamentos (max 50MB)
   * @param companyId ID da empresa (obtido via CompanyContext)
   * @param dryRun se true, apenas retorna preview sem persistir
   * @return relatório detalhado da importação
   */
  ImportLancamentoParteBResponse importLancamentos(
      MultipartFile file, Long companyId, boolean dryRun);
}
