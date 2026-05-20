package br.com.lalurecf.infrastructure.dto.taxparametertype;

import br.com.lalurecf.domain.enums.ParameterNature;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para requisição de criação de tipo de parâmetro tributário.
 *
 * @param description descrição do tipo (deve ser única)
 * @param nature natureza do tipo (GLOBAL, MONTHLY, QUARTERLY)
 */
public record CreateTaxParameterTypeRequest(
    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    String description,

    @NotNull(message = "Natureza é obrigatória")
    ParameterNature nature,

    Boolean required,

    @Min(value = 1, message = "Ordem de exibição deve ser maior que zero")
    Integer displayOrder,

    Boolean fiscalMovementExclusive
) {}
