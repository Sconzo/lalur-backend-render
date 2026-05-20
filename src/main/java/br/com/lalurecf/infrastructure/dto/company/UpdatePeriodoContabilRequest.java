package br.com.lalurecf.infrastructure.dto.company;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * DTO para requisição de atualização do Período Contábil de uma empresa.
 *
 * @param novoPeriodoContabil Nova data do período contábil (formato ISO 8601: YYYY-MM-DD)
 */
public record UpdatePeriodoContabilRequest(
    @NotNull(message = "Novo período contábil é obrigatório")
        LocalDate novoPeriodoContabil) {}
