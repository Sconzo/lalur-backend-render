package br.com.lalurecf.infrastructure.dto.company;

import java.util.List;

/**
 * DTO de resposta para atualização de parâmetros tributários.
 *
 * <p>Contém a confirmação da operação e a lista atualizada de parâmetros associados.
 */
public record UpdateTaxParametersResponse(
    boolean success, String message, List<TaxParameterSummary> taxParameters) {}
