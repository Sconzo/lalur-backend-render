package br.com.lalurecf.infrastructure.dto.company;

import java.time.LocalDate;

/**
 * DTO para resposta de atualização do Período Contábil de uma empresa.
 *
 * @param success Indica se operação foi bem-sucedida
 * @param message Mensagem descritiva
 * @param periodoContabilAnterior Data anterior do período contábil
 * @param periodoContabilNovo Nova data do período contábil
 */
public record UpdatePeriodoContabilResponse(
    boolean success,
    String message,
    LocalDate periodoContabilAnterior,
    LocalDate periodoContabilNovo) {}
