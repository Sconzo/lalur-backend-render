package br.com.lalurecf.application.port.in.lancamentocontabil;

import br.com.lalurecf.domain.model.LancamentoContabil;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Use case para listar lançamentos contábeis com filtros.
 */
public interface ListLancamentoContabilUseCase {

  /**
   * Lista lançamentos contábeis da empresa no contexto.
   * Suporta filtros por conta débito/crédito, data, range de data, fiscalYear e status.
   *
   * @param contaDebitoId ID da conta de débito (opcional)
   * @param contaCreditoId ID da conta de crédito (opcional)
   * @param data data específica (opcional)
   * @param dataInicio data início (opcional)
   * @param dataFim data fim (opcional)
   * @param fiscalYear ano fiscal (opcional)
   * @param includeInactive incluir inativos (default false)
   * @param pageable configuração de paginação
   * @return página de lançamentos
   */
  Page<LancamentoContabil> list(
      Long contaDebitoId,
      Long contaCreditoId,
      LocalDate data,
      LocalDate dataInicio,
      LocalDate dataFim,
      Integer fiscalYear,
      Boolean includeInactive,
      Pageable pageable
  );
}
