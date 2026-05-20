package br.com.lalurecf.infrastructure.dto.mapper;

import br.com.lalurecf.domain.model.ContaParteB;
import br.com.lalurecf.infrastructure.dto.contaparteb.ContaParteBResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper para conversão entre ContaParteB (domain) e ContaParteBResponse (DTO).
 *
 * <p>Converte objetos de domínio para DTOs de resposta.
 */
@Component
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class ContaParteBDtoMapper {

  /**
   * Converte ContaParteB domain para ContaParteBResponse DTO.
   *
   * @param conta objeto de domínio
   * @return DTO de resposta
   */
  public ContaParteBResponse toResponse(ContaParteB conta) {
    if (conta == null) {
      return null;
    }

    return ContaParteBResponse.builder()
        .id(conta.getId())
        .codigoConta(conta.getCodigoConta())
        .descricao(conta.getDescricao())
        .anoBase(conta.getAnoBase())
        .dataVigenciaInicio(conta.getDataVigenciaInicio())
        .dataVigenciaFim(conta.getDataVigenciaFim())
        .tipoTributo(conta.getTipoTributo())
        .saldoInicial(conta.getSaldoInicial())
        .tipoSaldo(conta.getTipoSaldo())
        .status(conta.getStatus())
        .createdAt(conta.getCreatedAt())
        .updatedAt(conta.getUpdatedAt())
        .build();
  }
}
