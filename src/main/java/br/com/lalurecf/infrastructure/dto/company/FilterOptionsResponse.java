package br.com.lalurecf.infrastructure.dto.company;

import java.util.List;

/**
 * DTO para resposta de opções de filtro.
 *
 * <p>Usado para popular dropdowns de filtros nos endpoints /filter-options/*.
 */
public record FilterOptionsResponse(
    List<String> options
) {
}
