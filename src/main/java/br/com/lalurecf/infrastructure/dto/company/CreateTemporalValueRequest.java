package br.com.lalurecf.infrastructure.dto.company;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO para criação de valor temporal de parâmetro tributário.
 *
 * <p>Representa um período (mensal ou trimestral) em que um parâmetro tributário está ativo para
 * uma empresa.
 *
 * <p>Constraint XOR: Exatamente UM campo deve estar preenchido (mes OU trimestre).
 *
 * @param ano ano fiscal (obrigatório)
 * @param mes mês (1-12) se mensal, null se trimestral
 * @param trimestre trimestre (1-4) se trimestral, null se mensal
 */
public record CreateTemporalValueRequest(
    @NotNull(message = "Ano é obrigatório") Integer ano,
    @Min(value = 1, message = "Mês deve estar entre 1 e 12")
        @Max(value = 12, message = "Mês deve estar entre 1 e 12")
        Integer mes,
    @Min(value = 1, message = "Trimestre deve estar entre 1 e 4")
        @Max(value = 4, message = "Trimestre deve estar entre 1 e 4")
        Integer trimestre) {}
