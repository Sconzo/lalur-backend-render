package br.com.lalurecf.application.port.in.company;

import br.com.lalurecf.infrastructure.dto.company.PeriodoContabilAuditResponse;
import java.util.List;

/**
 * Use case para consulta do histórico de alterações do Período Contábil.
 */
public interface GetPeriodoContabilAuditUseCase {

  /**
   * Retorna histórico completo de alterações do Período Contábil de uma empresa.
   *
   * @param companyId ID da empresa
   * @return lista de registros de auditoria ordenada do mais recente ao mais antigo
   */
  List<PeriodoContabilAuditResponse> getAuditHistory(Long companyId);
}
