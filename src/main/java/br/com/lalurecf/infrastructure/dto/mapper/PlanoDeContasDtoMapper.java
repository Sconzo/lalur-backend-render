package br.com.lalurecf.infrastructure.dto.mapper;

import br.com.lalurecf.domain.model.PlanoDeContas;
import br.com.lalurecf.infrastructure.dto.planodecontas.PlanoDeContasResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper para conversão entre PlanoDeContas (domain) e DTOs.
 *
 * <p>Converte objetos de domínio para DTOs de resposta.
 */
@Component
public class PlanoDeContasDtoMapper {

  /**
   * Converte PlanoDeContas domain para PlanoDeContasResponse DTO.
   *
   * @param account objeto de domínio
   * @param contaReferencialCodigo código da Conta Referencial RFB
   * @return DTO de resposta
   */
  public PlanoDeContasResponse toResponse(PlanoDeContas account, String contaReferencialCodigo) {
    if (account == null) {
      return null;
    }

    return PlanoDeContasResponse.builder()
        .id(account.getId())
        .code(account.getCode())
        .name(account.getName())
        .fiscalYear(account.getFiscalYear())
        .accountType(account.getAccountType())
        .contaReferencialId(account.getContaReferencialId())
        .contaReferencialCodigo(contaReferencialCodigo)
        .classe(account.getClasse())
        .nivel(account.getNivel())
        .natureza(account.getNatureza())
        .afetaResultado(account.getAfetaResultado())
        .dedutivel(account.getDedutivel())
        .status(account.getStatus())
        .createdAt(account.getCreatedAt())
        .updatedAt(account.getUpdatedAt())
        .build();
  }
}
