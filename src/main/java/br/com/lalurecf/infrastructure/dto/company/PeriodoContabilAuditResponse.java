package br.com.lalurecf.infrastructure.dto.company;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO para resposta de registro de auditoria do Período Contábil.
 *
 * @param id ID do registro de auditoria
 * @param periodoContabilAnterior Data anterior do período contábil
 * @param periodoContabilNovo Nova data do período contábil
 * @param changedBy Email do usuário que fez a alteração
 * @param changedAt Data e hora da alteração
 */
public record PeriodoContabilAuditResponse(
    Long id,
    LocalDate periodoContabilAnterior,
    LocalDate periodoContabilNovo,
    String changedBy,
    LocalDateTime changedAt) {}
