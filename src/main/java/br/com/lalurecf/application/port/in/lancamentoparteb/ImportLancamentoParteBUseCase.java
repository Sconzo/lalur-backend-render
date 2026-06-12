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
   * <p>Se já existirem lançamentos para a empresa no ano de referência (X-Fiscal-Year):
   *
   * <ul>
   *   <li>{@code overwrite=false} e {@code dryRun=false}: lança
   *       {@link br.com.lalurecf.infrastructure.exception.LancamentoParteBImportConflictException}
   *       (HTTP 409) para que o frontend exiba modal de confirmação;
   *   <li>{@code overwrite=true} e {@code dryRun=false}: deleta os lançamentos existentes
   *       daquele ano antes de inserir os novos (operação atômica na mesma transação);
   *   <li>{@code dryRun=true}: o flag {@code overwrite} é ignorado (nenhuma persistência).
   * </ul>
   *
   * @param file arquivo CSV/TXT com lançamentos (max 50MB)
   * @param companyId ID da empresa (obtido via CompanyContext)
   * @param dryRun se true, apenas retorna preview sem persistir
   * @param overwrite se true e há dados existentes, deleta antes de inserir
   * @return relatório detalhado da importação
   */
  ImportLancamentoParteBResponse importLancamentos(
      MultipartFile file, Long companyId, boolean dryRun, boolean overwrite);
}
