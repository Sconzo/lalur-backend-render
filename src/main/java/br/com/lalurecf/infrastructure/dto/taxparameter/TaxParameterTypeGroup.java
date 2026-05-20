package br.com.lalurecf.infrastructure.dto.taxparameter;

import br.com.lalurecf.domain.enums.ParameterNature;
import java.util.List;

/**
 * DTO para agrupar parâmetros tributários por tipo com sua natureza.
 */
public record TaxParameterTypeGroup(
    ParameterNature nature,
    Boolean required,
    Integer displayOrder,
    List<TaxParameterOption> parameters) {
}
