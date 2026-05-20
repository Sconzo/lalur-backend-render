package br.com.lalurecf.infrastructure.dto.mapper;

import br.com.lalurecf.domain.model.LancamentoParteB;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.LancamentoParteBResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper para conversão entre LancamentoParteB (domain) e LancamentoParteBResponse (DTO).
 */
@Component
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LancamentoParteBDtoMapper {

  public LancamentoParteBResponse toResponse(LancamentoParteB lancamento) {
    return toResponse(lancamento, null, null, null);
  }

  /**
   * Converte LancamentoParteB para response incluindo códigos resolvidos.
   *
   * @param lancamento objeto de domínio
   * @param contaContabilCode código da conta contábil (nullable)
   * @param contaParteBCode código da conta Parte B (nullable)
   * @param parametroTributarioCodigo código do parâmetro tributário (nullable)
   * @return DTO de resposta
   */
  public LancamentoParteBResponse toResponse(
      LancamentoParteB lancamento,
      String contaContabilCode,
      String contaParteBCode,
      String parametroTributarioCodigo) {
    if (lancamento == null) {
      return null;
    }

    return LancamentoParteBResponse.builder()
        .id(lancamento.getId())
        .mesReferencia(lancamento.getMesReferencia())
        .anoReferencia(lancamento.getAnoReferencia())
        .tipoApuracao(lancamento.getTipoApuracao())
        .tipoRelacionamento(lancamento.getTipoRelacionamento())
        .contaContabilId(lancamento.getContaContabilId())
        .contaContabilCode(contaContabilCode)
        .contaParteBId(lancamento.getContaParteBId())
        .contaParteBCode(contaParteBCode)
        .parametroTributarioId(lancamento.getParametroTributarioId())
        .parametroTributarioCodigo(parametroTributarioCodigo)
        .tipoAjuste(lancamento.getTipoAjuste())
        .descricao(lancamento.getDescricao())
        .valor(lancamento.getValor())
        .status(lancamento.getStatus())
        .createdAt(lancamento.getCreatedAt())
        .updatedAt(lancamento.getUpdatedAt())
        .build();
  }
}
