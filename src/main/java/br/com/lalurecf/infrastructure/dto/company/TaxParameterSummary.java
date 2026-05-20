package br.com.lalurecf.infrastructure.dto.company;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Summary representation of a TaxParameter associated with a Company.
 *
 * <p>Used in Company responses to show associated tax parameters with audit information.
 *
 * @param id the tax parameter ID
 * @param codigo the unique tax parameter code
 * @param tipo the tax parameter type/category
 * @param descricao the tax parameter description
 * @param associatedAt when the parameter was associated with the company
 * @param associatedBy email of the user who associated the parameter
 * @param hasTemporalValues indicates if this parameter has any temporal values configured
 * @param temporalValues list of temporal values if this is a periodic parameter
 */
public record TaxParameterSummary(
    Long id,
    String codigo,
    String tipo,
    String descricao,
    Integer displayOrder,
    LocalDateTime associatedAt,
    String associatedBy,
    boolean hasTemporalValues,
    List<TemporalValueResponse> temporalValues) {}
