package br.com.lalurecf.infrastructure.dto.taxparametertype;

import br.com.lalurecf.domain.enums.ParameterNature;
import br.com.lalurecf.domain.enums.Status;
import java.time.LocalDateTime;

/**
 * DTO para resposta de tipo de parâmetro tributário.
 *
 * @param id ID do tipo
 * @param description descrição do tipo
 * @param nature natureza do tipo (GLOBAL, MONTHLY, QUARTERLY)
 * @param status status (ACTIVE/INACTIVE)
 * @param createdAt data de criação
 * @param updatedAt data de última atualização
 */
public record TaxParameterTypeResponse(
    Long id,
    String description,
    ParameterNature nature,
    Status status,
    Boolean required,
    Integer displayOrder,
    Boolean fiscalMovementExclusive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
