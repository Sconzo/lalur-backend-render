package br.com.lalurecf.domain.model;

import br.com.lalurecf.domain.enums.Status;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model para Lançamento Contábil (partidas dobradas).
 *
 * <p>Representa um lançamento contábil com método de partidas dobradas (débito e crédito).
 * Cada lançamento possui uma conta de débito e uma conta de crédito, ambas referenciando
 * PlanoDeContas.
 *
 * <p>Implementa TemporalEntity para sujeição ao bloqueio temporal do Período Contábil.
 *
 * <p>Regras de negócio:
 *
 * <ul>
 *   <li>Valor sempre positivo (> 0)
 *   <li>Conta de débito deve ser diferente da conta de crédito
 *   <li>Data do lançamento deve estar dentro do período contábil da empresa
 *   <li>Ambas as contas devem pertencer ao mesmo ano fiscal (fiscalYear)
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LancamentoContabil implements TemporalEntity {

  /** Identificador único do lançamento. */
  private Long id;

  /** ID da empresa dona do lançamento. */
  private Long companyId;

  /** ID da conta de débito. */
  private Long contaDebitoId;

  /** Código da conta de débito (para exportação). */
  private String contaDebitoCode;

  /** Nome da conta de débito (para exportação). */
  private String contaDebitoName;

  /** ID da conta de crédito. */
  private Long contaCreditoId;

  /** Código da conta de crédito (para exportação). */
  private String contaCreditoCode;

  /** Nome da conta de crédito (para exportação). */
  private String contaCreditoName;

  /**
   * Data do lançamento contábil.
   *
   * <p>Também utilizada como data de competência para validação de Período Contábil.
   */
  private LocalDate data;

  /**
   * Valor do lançamento (sempre positivo).
   *
   * <p>Este valor será debitado na conta de débito e creditado na conta de crédito.
   */
  private BigDecimal valor;

  /**
   * Histórico do lançamento.
   *
   * <p>Descrição detalhada do fato contábil que originou o lançamento.
   */
  private String historico;

  /**
   * Número do documento que originou o lançamento (opcional).
   *
   * <p>Exemplo: número da nota fiscal, número do cheque, etc.
   */
  private String numeroDocumento;

  /**
   * Ano fiscal do lançamento.
   *
   * <p>Deve corresponder ao ano fiscal das contas de débito e crédito.
   */
  private Integer fiscalYear;

  /** Status do lançamento (ACTIVE ou INACTIVE para soft delete). */
  private Status status;

  /** Data de criação do registro. */
  private LocalDateTime createdAt;

  /** Data de última atualização do registro. */
  private LocalDateTime updatedAt;

  /**
   * Retorna a data de competência para validação de Período Contábil.
   *
   * <p>Implementação de TemporalEntity - retorna o campo 'data' como competência.
   *
   * @return data do lançamento
   */
  @Override
  public LocalDate getCompetencia() {
    return this.data;
  }
}
