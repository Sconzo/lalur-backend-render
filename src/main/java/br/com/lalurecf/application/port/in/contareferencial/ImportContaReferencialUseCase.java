package br.com.lalurecf.application.port.in.contareferencial;

import br.com.lalurecf.infrastructure.dto.contareferencial.ImportContaReferencialResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Use case port para importação de contas referenciais RFB via arquivo CSV/TXT.
 *
 * <p>Permite carregar rapidamente centenas de contas referenciais oficiais da Receita Federal,
 * facilitando a carga inicial ou atualização em massa.
 *
 * <p>Formato esperado: codigoRfb;descricao;anoValidade
 */
public interface ImportContaReferencialUseCase {

  /**
   * Importa contas referenciais de arquivo CSV/TXT.
   *
   * @param file arquivo CSV/TXT com contas referenciais
   * @param dryRun se true, apenas retorna preview sem persistir
   * @return relatório detalhado da importação
   */
  ImportContaReferencialResponse importContasReferenciais(MultipartFile file, boolean dryRun);
}
