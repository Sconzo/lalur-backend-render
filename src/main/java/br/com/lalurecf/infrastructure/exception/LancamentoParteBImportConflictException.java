package br.com.lalurecf.infrastructure.exception;

/**
 * Exception lançada quando uma tentativa de importação de Lançamentos da Parte B detecta
 * que já existem registros para a empresa naquele ano de referência.
 *
 * <p>Resulta em HTTP 409 Conflict, contendo dados para o frontend exibir um modal de
 * confirmação de sobrescrita.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LancamentoParteBImportConflictException extends RuntimeException {

  private final long existingCount;
  private final Integer anoReferencia;

  /**
   * Construtor.
   *
   * @param existingCount quantidade de lançamentos já existentes
   * @param anoReferencia ano de referência em conflito
   */
  public LancamentoParteBImportConflictException(long existingCount, Integer anoReferencia) {
    super(
        "Já existem "
            + existingCount
            + " lançamento(s) da Parte B para o ano "
            + anoReferencia
            + ". Confirme a sobrescrita para substituir os dados existentes.");
    this.existingCount = existingCount;
    this.anoReferencia = anoReferencia;
  }

  public long getExistingCount() {
    return existingCount;
  }

  public Integer getAnoReferencia() {
    return anoReferencia;
  }
}
