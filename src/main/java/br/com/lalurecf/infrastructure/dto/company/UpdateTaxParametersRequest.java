package br.com.lalurecf.infrastructure.dto.company;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * DTO para atualização de parâmetros tributários de uma empresa.
 *
 * <p>Lista de IDs dos parâmetros tributários a serem associados. A lista substitui completamente
 * os parâmetros anteriores (não acumula).
 */
public record UpdateTaxParametersRequest(
    @NotNull(message = "Lista de IDs dos parâmetros tributários é obrigatória")
        List<Long> taxParameterIds) {}
