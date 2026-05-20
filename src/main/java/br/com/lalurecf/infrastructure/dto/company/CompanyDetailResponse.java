package br.com.lalurecf.infrastructure.dto.company;

import br.com.lalurecf.domain.model.CompanyStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para resposta detalhada de empresa.
 *
 * <p>Inclui todos os campos da empresa, incluindo dados de auditoria e relacionamentos.
 * Parâmetros tributários são retornados como lista genérica agrupável por tipo.
 */
public record CompanyDetailResponse(
    Long id,
    String cnpj,  // Formatado: 00.000.000/0000-00
    CompanyStatus status,
    String razaoSocial,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate periodoContabil,

    String mascaraNiveis,

    List<TaxParameterSummary> parameters,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt
) {
}
