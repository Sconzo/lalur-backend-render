package br.com.lalurecf.infrastructure.dto.company;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para atualização de empresa.
 *
 * <p>Permite editar todos os campos exceto o CNPJ (imutável).
 *
 * <p>Parâmetros tributários:
 * <ul>
 *   <li><b>globalParameterIds:</b> IDs de parâmetros globais (aplicam-se ao ano todo).
 *       Deve conter pelo menos um parâmetro de cada tipo obrigatório: CNAE, QUALIFICACAO_PJ,
 *       NATUREZA_JURIDICA.
 *   <li><b>periodicParameters:</b> Parâmetros periódicos com valores temporais (mês/trimestre).
 * </ul>
 */
public record UpdateCompanyRequest(

    @NotBlank(message = "Razão Social é obrigatória")
    @Size(max = 255, message = "Razão Social deve ter no máximo 255 caracteres")
    String razaoSocial,

    @NotNull(message = "Período Contábil é obrigatório")
    @PastOrPresent(message = "Período Contábil não pode ser no futuro")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate periodoContabil,

    @NotBlank(message = "Máscara de níveis é obrigatória")
    @Pattern(
        regexp = "^9+(\\.9+){0,5}$",
        message = "Máscara de níveis inválida. Use apenas '9' e '.', ex: '99.999.99.999999'")
    String mascaraNiveis,

    @NotNull(message = "Número de níveis é obrigatório")
    @Min(value = 1, message = "Número de níveis deve ser entre 1 e 6")
    @Max(value = 6, message = "Número de níveis deve ser entre 1 e 6")
    Integer numNiveis,

    @NotEmpty(message = "Lista de parâmetros globais é obrigatória")
    List<Long> globalParameterIds,

    @Valid
    List<PeriodicParameterRequest> periodicParameters
) {
}
