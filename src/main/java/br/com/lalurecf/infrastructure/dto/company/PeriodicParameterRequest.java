package br.com.lalurecf.infrastructure.dto.company;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * DTO para parâmetro tributário periódico com valores temporais.
 *
 * <p>Representa um parâmetro que precisa ser associado a períodos específicos (mês ou trimestre).
 *
 * @param taxParameterId ID do parâmetro tributário
 * @param temporalValues lista de valores temporais (pelo menos um obrigatório)
 */
public record PeriodicParameterRequest(
    @NotNull(message = "ID do parâmetro tributário é obrigatório") Long taxParameterId,
    @NotEmpty(message = "Lista de valores temporais deve ter pelo menos um elemento")
        @Valid
        List<TemporalValueInput> temporalValues) {}
