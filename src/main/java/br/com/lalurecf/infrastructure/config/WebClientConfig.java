package br.com.lalurecf.infrastructure.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Configuração do WebClient para integrações com APIs externas.
 *
 * <p>Configura timeout, headers padrão e connection pool para chamadas HTTP.
 */
@Configuration
public class WebClientConfig {

  /**
   * WebClient configurado para BrasilAPI.
   *
   * <p>Configurações:
   * <ul>
   *   <li>Base URL: https://brasilapi.com.br/api
   *   <li>Connection timeout: 10 segundos
   *   <li>Read timeout: 10 segundos
   *   <li>Write timeout: 10 segundos
   * </ul>
   *
   * @param builder builder do WebClient fornecido pelo Spring
   * @return WebClient configurado para BrasilAPI
   */
  @Bean
  public WebClient brasilApiWebClient(WebClient.Builder builder) {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
        .responseTimeout(Duration.ofSeconds(10))
        .doOnConnected(conn -> conn
            .addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
            .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS))
        );

    return builder
        .baseUrl("https://brasilapi.com.br/api")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }
}
