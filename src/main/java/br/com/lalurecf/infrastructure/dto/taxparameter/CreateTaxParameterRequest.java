package br.com.lalurecf.infrastructure.dto.taxparameter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para requisição de criação de parâmetro tributário.
 *
 * @param code código único do parâmetro (alfanumérico com hífens)
 * @param typeId ID do tipo de parâmetro tributário
 * @param description descrição detalhada (opcional)
 */
public record CreateTaxParameterRequest(
    @NotBlank(message = "Código é obrigatório")
    @Pattern(
        regexp = "^[A-Z0-9-]+$",
        message = "Código deve conter apenas letras maiúsculas, números e hífens")
    String code,

    @NotNull(message = "Tipo é obrigatório")
    Long typeId,

    String description
) {}
