package br.com.lalurecf.infrastructure.dto.mapper;

import br.com.lalurecf.domain.model.LancamentoContabil;
import br.com.lalurecf.infrastructure.dto.lancamentocontabil.LancamentoContabilResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper para conversão entre LancamentoContabil (domain) e DTOs.
 */
@Component
public class LancamentoContabilDtoMapper {

  /**
   * Converte LancamentoContabil domain para LancamentoContabilResponse DTO.
   *
   * @param lancamento objeto de domínio
   * @return DTO de resposta
   */
  public LancamentoContabilResponse toResponse(LancamentoContabil lancamento) {
    if (lancamento == null) {
      return null;
    }

    return LancamentoContabilResponse.builder()
        .id(lancamento.getId())
        .contaDebitoId(lancamento.getContaDebitoId())
        .contaDebitoCodigo(lancamento.getContaDebitoCode())
        .contaDebitoNome(lancamento.getContaDebitoName())
        .contaCreditoId(lancamento.getContaCreditoId())
        .contaCreditoCodigo(lancamento.getContaCreditoCode())
        .contaCreditoNome(lancamento.getContaCreditoName())
        .data(lancamento.getData())
        .valor(lancamento.getValor())
        .historico(lancamento.getHistorico())
        .numeroDocumento(lancamento.getNumeroDocumento())
        .fiscalYear(lancamento.getFiscalYear())
        .status(lancamento.getStatus())
        .createdAt(lancamento.getCreatedAt())
        .updatedAt(lancamento.getUpdatedAt())
        .build();
  }
}
