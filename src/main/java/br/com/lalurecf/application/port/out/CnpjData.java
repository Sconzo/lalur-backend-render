package br.com.lalurecf.application.port.out;

/**
 * Record representando dados de uma empresa retornados pela API de CNPJ.
 *
 * <p>Usado como DTO para transporte de dados entre o adapter externo
 * e o application layer.
 *
 * @param cnpj CNPJ da empresa (14 dígitos, apenas números)
 * @param razaoSocial Razão social da empresa
 * @param cnae Código CNAE fiscal (atividade econômica)
 * @param qualificacaoPj Qualificação da pessoa jurídica responsável
 * @param naturezaJuridica Código da natureza jurídica
 */
public record CnpjData(
    String cnpj,
    String razaoSocial,
    String cnae,
    String qualificacaoPj,
    String naturezaJuridica
) {
}
