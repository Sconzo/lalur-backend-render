package br.com.lalurecf.domain.model;

import java.time.LocalDate;

/**
 * Interface marcadora para entidades que possuem competência (data).
 *
 * <p>Entidades que implementam esta interface estão sujeitas ao bloqueio temporal
 * do Período Contábil. Operações de edição e exclusão em registros com competência
 * anterior ao Período Contábil da empresa serão bloqueadas.
 *
 * <p>Exemplo de uso:
 * <pre>
 * public class AccountingEntry implements TemporalEntity {
 *   private LocalDate competencia;
 *
 *   &#64;Override
 *   public LocalDate getCompetencia() {
 *     return this.competencia;
 *   }
 * }
 * </pre>
 *
 * @see br.com.lalurecf.infrastructure.aspect.PeriodoContabilAspect
 */
public interface TemporalEntity {

  /**
   * Retorna a data de competência do registro.
   *
   * <p>Esta data será comparada com o Período Contábil da empresa para determinar
   * se o registro pode ser editado ou excluído.
   *
   * @return data de competência do registro
   */
  LocalDate getCompetencia();
}
