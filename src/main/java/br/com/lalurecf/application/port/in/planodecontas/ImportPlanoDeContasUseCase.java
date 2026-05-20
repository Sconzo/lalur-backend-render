package br.com.lalurecf.application.port.in.planodecontas;

import br.com.lalurecf.infrastructure.dto.planodecontas.ImportPlanoDeContasResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Use case port para importação de plano de contas via arquivo CSV/TXT.
 *
 * <p>Permite carregar rapidamente centenas de contas contábeis de sistemas ERP externos,
 * vinculando cada conta a uma Conta Referencial RFB oficial.
 *
 * <p>Formato esperado: code;name;accountType;contaReferencialCodigo;classe;nivel;natureza;
 * afetaResultado;dedutivel
 */
public interface ImportPlanoDeContasUseCase {

  /**
   * Importa plano de contas de arquivo CSV/TXT.
   *
   * @param file arquivo CSV/TXT com contas contábeis
   * @param companyId ID da empresa dona das contas
   * @param fiscalYear ano fiscal das contas
   * @param dryRun se true, apenas retorna preview sem persistir
   * @return relatório detalhado da importação
   */
  ImportPlanoDeContasResponse importPlanoDeContas(
      MultipartFile file, Long companyId, Integer fiscalYear, boolean dryRun);
}
