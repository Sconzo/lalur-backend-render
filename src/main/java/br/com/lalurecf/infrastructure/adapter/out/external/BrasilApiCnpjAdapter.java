package br.com.lalurecf.infrastructure.adapter.out.external;

import br.com.lalurecf.application.port.out.CnpjData;
import br.com.lalurecf.application.port.out.CnpjSearchPort;
import br.com.lalurecf.infrastructure.dto.external.BrasilApiCnpjResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Adapter para busca de dados de CNPJ na BrasilAPI.
 *
 * <p>Implementa {@link CnpjSearchPort} utilizando a BrasilAPI como provedor.
 * Documentação: https://brasilapi.com.br/docs
 *
 * <p>Features:
 * <ul>
 *   <li>Timeout configurado: 10 segundos
 *   <li>Retry: 1 tentativa adicional em caso de erro 5xx ou timeout
 *   <li>Cache: 24 horas (via Spring Cache)
 *   <li>Tratamento gracioso de erros (retorna Optional.empty)
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BrasilApiCnpjAdapter implements CnpjSearchPort {

  private final WebClient brasilApiWebClient;

  /**
   * Busca dados de empresa por CNPJ na BrasilAPI.
   *
   * <p>Endpoint: GET /cnpj/v1/{cnpj}
   *
   * <p>Comportamento:
   * <ul>
   *   <li>200 OK → converte e retorna dados
   *   <li>404 Not Found → retorna Optional.empty
   *   <li>5xx ou timeout → retry 1x, depois retorna Optional.empty
   *   <li>Outros erros → loga e retorna Optional.empty
   * </ul>
   *
   * @param cnpj CNPJ da empresa (14 dígitos, apenas números)
   * @return Optional contendo dados se encontrado, ou empty se não encontrado/erro
   */
  @Override
  @Cacheable(value = "cnpj-data", key = "#cnpj")
  public Optional<CnpjData> searchByCnpj(String cnpj) {
    log.info("Buscando dados do CNPJ {} na BrasilAPI", cnpj);

    try {
      BrasilApiCnpjResponse response = brasilApiWebClient
          .get()
          .uri("/cnpj/v1/{cnpj}", cnpj)
          .retrieve()
          .onStatus(
              status -> status.is5xxServerError(),
              clientResponse -> {
                log.warn("BrasilAPI retornou erro 5xx para CNPJ {}", cnpj);
                return Mono.error(new RuntimeException("BrasilAPI server error"));
              }
          )
          .bodyToMono(BrasilApiCnpjResponse.class)
          .retryWhen(
              Retry.max(1)
                  .filter(throwable -> !(throwable instanceof WebClientResponseException.NotFound))
                  .doBeforeRetry(retrySignal ->
                      log.warn("Retentando busca de CNPJ {} após erro: {}",
                          cnpj, retrySignal.failure().getMessage())
                  )
          )
          .block();

      if (response == null) {
        log.warn("BrasilAPI retornou resposta vazia para CNPJ {}", cnpj);
        return Optional.empty();
      }

      log.info("Dados do CNPJ {} encontrados com sucesso na BrasilAPI", cnpj);
      return Optional.of(mapToCnpjData(response));

    } catch (WebClientResponseException.NotFound e) {
      log.info("CNPJ {} não encontrado na BrasilAPI", cnpj);
      return Optional.empty();

    } catch (Exception e) {
      log.error("Erro ao buscar CNPJ {} na BrasilAPI: {}", cnpj, e.getMessage(), e);
      return Optional.empty();
    }
  }

  /**
   * Converte BrasilApiCnpjResponse para CnpjData.
   *
   * <p>Garante que o CNAE fiscal sempre tenha 7 dígitos com padding de zeros à esquerda,
   * pois a BrasilAPI pode retornar sem os zeros (ex: "111301" ao invés de "0111301").
   *
   * @param response resposta da BrasilAPI
   * @return dados convertidos
   */
  private CnpjData mapToCnpjData(BrasilApiCnpjResponse response) {
    // CNAE fiscal brasileiro sempre tem 7 dígitos - adiciona zeros à esquerda se necessário
    String cnaeFormatted = formatCnae(response.cnaeFiscal());

    return new CnpjData(
        response.cnpj(),
        response.razaoSocial(),
        cnaeFormatted,
        response.qualificacaoResponsavel(),
        response.naturezaJuridica()
    );
  }

  /**
   * Formata CNAE fiscal garantindo 7 dígitos com padding de zeros à esquerda.
   *
   * <p>Exemplos:
   * <ul>
   *   <li>"111301" → "0111301"
   *   <li>"6421200" → "6421200"
   *   <li>null → null
   * </ul>
   *
   * @param cnae CNAE fiscal retornado pela API
   * @return CNAE com 7 dígitos ou null se entrada for null
   */
  private String formatCnae(String cnae) {
    if (cnae == null || cnae.isBlank()) {
      return null;
    }

    // Remove caracteres não numéricos (caso existam)
    String onlyNumbers = cnae.replaceAll("\\D", "");

    // Adiciona zeros à esquerda até completar 7 dígitos
    return String.format("%07d", Long.parseLong(onlyNumbers));
  }
}
