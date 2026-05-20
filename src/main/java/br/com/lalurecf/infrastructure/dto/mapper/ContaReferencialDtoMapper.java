package br.com.lalurecf.infrastructure.dto.mapper;

import br.com.lalurecf.domain.model.ContaReferencial;
import br.com.lalurecf.infrastructure.dto.contareferencial.ContaReferencialResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper para conversão entre ContaReferencial (domain) e ContaReferencialResponse (DTO).
 *
 * <p>Converte objetos de domínio para DTOs de resposta.
 */
@Component
public class ContaReferencialDtoMapper {

  /**
   * Converte ContaReferencial domain para ContaReferencialResponse DTO.
   *
   * @param conta objeto de domínio
   * @return DTO de resposta
   */
  public ContaReferencialResponse toResponse(ContaReferencial conta) {
    if (conta == null) {
      return null;
    }

    return ContaReferencialResponse.builder()
        .id(conta.getId())
        .codigoRfb(conta.getCodigoRfb())
        .descricao(conta.getDescricao())
        .anoValidade(conta.getAnoValidade())
        .status(conta.getStatus())
        .createdAt(conta.getCreatedAt())
        .updatedAt(conta.getUpdatedAt())
        .build();
  }
}
