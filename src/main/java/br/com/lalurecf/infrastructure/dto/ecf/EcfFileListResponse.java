package br.com.lalurecf.infrastructure.dto.ecf;

/**
 * DTO de resposta para listagem de arquivos ECF por ano fiscal.
 *
 * <p>Retorna um sumário de cada tipo de arquivo (ou null se não existir).
 * O campo {@code content} não é incluído — use GET /ecf/{id}/download para baixar.
 */
public class EcfFileListResponse {

  /**Arquivo Parcial ECF (ARQUIVO_PARCIAL). Null se não gerado ainda. */
  private EcfFileSummary arquivoParcial;

  /**ECF Importado de sistema externo (IMPORTED_ECF). Null se não importado ainda. */
  private EcfFileSummary ecfImportado;

  /**ECF Completo gerado via merge (COMPLETE_ECF). Null se não gerado ainda. */
  private EcfFileSummary ecfCompleto;

  public EcfFileListResponse() {
  }

  public EcfFileSummary getArquivoParcial() {
    return arquivoParcial;
  }

  public void setArquivoParcial(EcfFileSummary arquivoParcial) {
    this.arquivoParcial = arquivoParcial;
  }

  public EcfFileSummary getEcfImportado() {
    return ecfImportado;
  }

  public void setEcfImportado(EcfFileSummary ecfImportado) {
    this.ecfImportado = ecfImportado;
  }

  public EcfFileSummary getEcfCompleto() {
    return ecfCompleto;
  }

  public void setEcfCompleto(EcfFileSummary ecfCompleto) {
    this.ecfCompleto = ecfCompleto;
  }
}
