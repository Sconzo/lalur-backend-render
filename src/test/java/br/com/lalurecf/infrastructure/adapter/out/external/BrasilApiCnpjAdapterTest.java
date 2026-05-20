package br.com.lalurecf.infrastructure.adapter.out.external;

import br.com.lalurecf.application.port.out.CnpjData;
import java.io.IOException;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de integração para BrasilApiCnpjAdapter.
 *
 * <p>Usa MockWebServer para simular respostas da API BrasilAPI.
 */
@DisplayName("BrasilApiCnpjAdapter Integration Tests")
class BrasilApiCnpjAdapterTest {

  private MockWebServer mockWebServer;
  private BrasilApiCnpjAdapter adapter;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    // Configurar WebClient para apontar para o MockWebServer
    String baseUrl = mockWebServer.url("/api").toString();
    WebClient webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .build();

    adapter = new BrasilApiCnpjAdapter(webClient);
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  @DisplayName("Should return CNPJ data when API returns 200 OK")
  void shouldReturnCnpjDataWhenApiReturns200() {
    // Arrange
    String cnpj = "00000000000191";
    String responseBody = """
        {
          "cnpj": "00000000000191",
          "razao_social": "BANCO DO BRASIL S.A.",
          "cnae_fiscal": "6421200",
          "qualificacao_do_responsavel": "Diretor",
          "natureza_juridica": "205-1"
        }
        """;

    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setBody(responseBody)
        .addHeader("Content-Type", "application/json"));

    // Act
    Optional<CnpjData> result = adapter.searchByCnpj(cnpj);

    // Assert
    assertTrue(result.isPresent());
    assertEquals(cnpj, result.get().cnpj());
    assertEquals("BANCO DO BRASIL S.A.", result.get().razaoSocial());
    assertEquals("6421200", result.get().cnae());
    assertEquals("Diretor", result.get().qualificacaoPj());
    assertEquals("205-1", result.get().naturezaJuridica());
  }

  @Test
  @DisplayName("Should return empty Optional when API returns 404 Not Found")
  void shouldReturnEmptyWhenApiReturns404() {
    // Arrange
    String cnpj = "99999999999999";
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(404)
        .setBody("{\"message\":\"CNPJ 99999999999999 não encontrado\"}")
        .addHeader("Content-Type", "application/json"));

    // Act
    Optional<CnpjData> result = adapter.searchByCnpj(cnpj);

    // Assert
    assertFalse(result.isPresent());
  }

  @Test
  @DisplayName("Should return empty Optional when API returns 500 Server Error")
  void shouldReturnEmptyWhenApiReturns500() {
    // Arrange
    String cnpj = "00000000000191";
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(500)
        .setBody("Internal Server Error"));

    // Segundo mock para o retry
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(500)
        .setBody("Internal Server Error"));

    // Act
    Optional<CnpjData> result = adapter.searchByCnpj(cnpj);

    // Assert
    assertFalse(result.isPresent());
  }

  @Test
  @DisplayName("Should retry once on 5xx error and succeed on second attempt")
  void shouldRetryOnceAndSucceed() {
    // Arrange
    String cnpj = "00000000000191";
    String responseBody = """
        {
          "cnpj": "00000000000191",
          "razao_social": "BANCO DO BRASIL S.A.",
          "cnae_fiscal": "6421200",
          "qualificacao_do_responsavel": "Diretor",
          "natureza_juridica": "205-1"
        }
        """;

    // Primeira tentativa: erro 500
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(500)
        .setBody("Internal Server Error"));

    // Retry: sucesso
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setBody(responseBody)
        .addHeader("Content-Type", "application/json"));

    // Act
    Optional<CnpjData> result = adapter.searchByCnpj(cnpj);

    // Assert
    assertTrue(result.isPresent());
    assertEquals(cnpj, result.get().cnpj());
  }

  @Test
  @DisplayName("Should return empty Optional when response body is invalid JSON")
  void shouldReturnEmptyWhenResponseBodyIsInvalid() {
    // Arrange
    String cnpj = "00000000000191";
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setBody("invalid json")
        .addHeader("Content-Type", "application/json"));

    // Act
    Optional<CnpjData> result = adapter.searchByCnpj(cnpj);

    // Assert
    assertFalse(result.isPresent());
  }

  @Test
  @DisplayName("Should format CNAE with 7 digits padding zeros to the left")
  void shouldFormatCnaeWith7Digits() {
    // Arrange - CNPJ real que retorna CNAE sem zero à esquerda
    String cnpj = "09377436000179";
    String responseBody = """
        {
          "cnpj": "09377436000179",
          "razao_social": "EMPRESA TESTE",
          "cnae_fiscal": "111301",
          "qualificacao_do_responsavel": "Sócio-Administrador",
          "natureza_juridica": "206-2"
        }
        """;

    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setBody(responseBody)
        .addHeader("Content-Type", "application/json"));

    // Act
    Optional<CnpjData> result = adapter.searchByCnpj(cnpj);

    // Assert
    assertTrue(result.isPresent());
    assertEquals("0111301", result.get().cnae(),
        "CNAE deve ter 7 dígitos com zeros à esquerda (111301 → 0111301)");
  }

  @Test
  @DisplayName("Should keep CNAE with 7 digits unchanged")
  void shouldKeepCnaeWith7DigitsUnchanged() {
    // Arrange - CNAE já com 7 dígitos
    String cnpj = "00000000000191";
    String responseBody = """
        {
          "cnpj": "00000000000191",
          "razao_social": "BANCO DO BRASIL S.A.",
          "cnae_fiscal": "6421200",
          "qualificacao_do_responsavel": "Diretor",
          "natureza_juridica": "205-1"
        }
        """;

    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setBody(responseBody)
        .addHeader("Content-Type", "application/json"));

    // Act
    Optional<CnpjData> result = adapter.searchByCnpj(cnpj);

    // Assert
    assertTrue(result.isPresent());
    assertEquals("6421200", result.get().cnae(),
        "CNAE com 7 dígitos deve permanecer inalterado");
  }
}
