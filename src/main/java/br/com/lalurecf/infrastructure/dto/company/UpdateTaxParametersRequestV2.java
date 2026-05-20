package br.com.lalurecf.infrastructure.dto.company;

import jakarta.validation.Valid;
import java.util.List;

/**
 * DTO para atualização de parâmetros tributários de uma empresa (versão 2).
 *
 * <p>Suporta dois tipos de parâmetros tributários:
 * <ul>
 *   <li><b>Globais:</b> Aplicam-se ao ano inteiro
 *       (ex: CNAE, Qualificação PJ, Natureza Jurídica)
 *   <li><b>Periódicos:</b> Precisam de mês/trimestre específico
 *       (ex: parâmetros que mudam durante o ano)
 * </ul>
 *
 * <p>A lista substitui completamente os parâmetros anteriores (não acumula).
 *
 * @param globalParameterIds IDs dos parâmetros globais (aplicam-se ao ano todo)
 * @param periodicParameters parâmetros periódicos com seus valores temporais
 */
public record UpdateTaxParametersRequestV2(
    List<Long> globalParameterIds,
    @Valid List<PeriodicParameterRequest> periodicParameters) {}
