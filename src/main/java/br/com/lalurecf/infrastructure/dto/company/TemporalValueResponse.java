package br.com.lalurecf.infrastructure.dto.company;

/**
 * Response DTO para valor temporal de parâmetro tributário.
 *
 * <p>Inclui campo `periodo` formatado para facilitar exibição no frontend.
 *
 * @param id ID do registro
 * @param ano ano fiscal
 * @param mes mês (1-12) se mensal, null se trimestral
 * @param trimestre trimestre (1-4) se trimestral, null se mensal
 * @param periodo período formatado (ex: "Jan/2024" ou "1º Tri/2024")
 */
public record TemporalValueResponse(
    Long id, Integer ano, Integer mes, Integer trimestre, String periodo) {}
