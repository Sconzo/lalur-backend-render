package br.com.lalurecf.infrastructure.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para resposta da BrasilAPI - endpoint /cnpj/v1/{cnpj}.
 *
 * <p>Mapeia o JSON retornado pela API BrasilAPI para objeto Java.
 * Documentação: https://brasilapi.com.br/docs
 *
 * <p>Exemplo de resposta:
 * <pre>
 * {
 *   "cnpj": "00000000000191",
 *   "razao_social": "BANCO DO BRASIL S.A.",
 *   "cnae_fiscal": "6421200",
 *   "qualificacao_do_responsavel": "Diretor",
 *   "natureza_juridica": "205-1"
 * }
 * </pre>
 */
public record BrasilApiCnpjResponse(
    @JsonProperty("cnpj")
    String cnpj,

    @JsonProperty("razao_social")
    String razaoSocial,

    @JsonProperty("cnae_fiscal")
    String cnaeFiscal,

    @JsonProperty("qualificacao_do_responsavel")
    String qualificacaoResponsavel,

    @JsonProperty("natureza_juridica")
    String naturezaJuridica
) {
}
