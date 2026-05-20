package br.com.lalurecf.infrastructure.dto.taxparameter;

import br.com.lalurecf.domain.enums.ParameterNature;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.infrastructure.dto.taxparametertype.TaxParameterTypeResponse;
import java.time.LocalDateTime;

/**
 * DTO para resposta de parâmetro tributário.
 *
 * @param id ID do parâmetro
 * @param code código único
 * @param type tipo do parâmetro (inclui descrição e natureza)
 * @param description descrição do parâmetro
 * @param status status (ACTIVE/INACTIVE)
 * @param createdAt data de criação
 * @param updatedAt data de última atualização
 */
public record TaxParameterResponse(
    Long id,
    String code,
    TaxParameterTypeResponse type,
    String description,
    Status status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
