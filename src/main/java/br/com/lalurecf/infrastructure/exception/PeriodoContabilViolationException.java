package br.com.lalurecf.infrastructure.exception;

import java.time.LocalDate;

/**
 * Exception lançada quando há tentativa de editar ou excluir registro com competência
 * anterior ao Período Contábil da empresa.
 *
 * <p>Esta exception resulta em HTTP 400 Bad Request quando tratada pelo
 * {@link br.com.lalurecf.infrastructure.exception.GlobalExceptionHandler}.
 */
public class PeriodoContabilViolationException extends RuntimeException {

  private final LocalDate competencia;
  private final LocalDate periodoContabil;

  /**
   * Construtor da exception.
   *
   * @param competencia data de competência do registro que tentou-se editar
   * @param periodoContabil período contábil da empresa (data de corte)
   */
  public PeriodoContabilViolationException(LocalDate competencia, LocalDate periodoContabil) {
    super(String.format(
        "Não é possível editar dados com competência (%s) anterior ao Período Contábil (%s)",
        competencia,
        periodoContabil
    ));
    this.competencia = competencia;
    this.periodoContabil = periodoContabil;
  }

  public LocalDate getCompetencia() {
    return competencia;
  }

  public LocalDate getPeriodoContabil() {
    return periodoContabil;
  }
}
