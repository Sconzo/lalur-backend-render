package br.com.lalurecf.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.lalurecf.application.port.out.UserRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.UserRole;
import br.com.lalurecf.domain.model.User;
import br.com.lalurecf.infrastructure.dto.auth.LoginRequest;
import br.com.lalurecf.infrastructure.dto.auth.LoginResponse;
import br.com.lalurecf.util.IntegrationTestBase;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Testes de integração para os 4 endpoints GET /{entidade}/import-schema, que retornam
 * o schema de importação em formato CSV (para visualização em Excel pelo usuário final).
 */
@DisplayName("Import Schema CSV Integration Tests")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ImportSchemaCsvIntegrationTest extends IntegrationTestBase {

  private static final String UTF8_BOM = "﻿";
  private static final String EXPECTED_HEADER =
      "Campo;Tipo;Obrigatório;Formato;Valores permitidos;"
          + "Observação;Tamanho máximo;Exemplo";

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepositoryPort userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private String contadorToken;
  private String adminToken;

  @BeforeEach
  void setup() {
    String contadorEmail = "contador" + System.currentTimeMillis() + "@test.com";
    String adminEmail = "admin" + System.currentTimeMillis() + "@test.com";

    userRepository.save(
        User.builder()
            .email(contadorEmail)
            .password(passwordEncoder.encode("password123"))
            .firstName("Contador")
            .lastName("Test")
            .role(UserRole.CONTADOR)
            .mustChangePassword(false)
            .status(Status.ACTIVE)
            .build());
    userRepository.save(
        User.builder()
            .email(adminEmail)
            .password(passwordEncoder.encode("password123"))
            .firstName("Admin")
            .lastName("Test")
            .role(UserRole.ADMIN)
            .mustChangePassword(false)
            .status(Status.ACTIVE)
            .build());

    contadorToken = login(contadorEmail);
    adminToken = login(adminEmail);
  }

  private String login(String email) {
    ResponseEntity<LoginResponse> response =
        restTemplate.postForEntity(
            "/api/v1/auth/login", new LoginRequest(email, "password123"), LoginResponse.class);
    if (response.getBody() == null) {
      throw new IllegalStateException("Login failed: status=" + response.getStatusCode());
    }
    return response.getBody().getAccessToken();
  }

  private ResponseEntity<byte[]> getCsv(String url, String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
  }

  @Test
  @DisplayName("GET /plano-de-contas/import-schema - CSV renderiza tipos PT-BR, enums e booleanos")
  void planoDeContasSchemaRendersFriendlyCsv() {
    ResponseEntity<byte[]> response =
        getCsv("/api/v1/plano-de-contas/import-schema", contadorToken);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType().toString())
        .startsWith("text/csv");
    assertThat(response.getHeaders().getContentDisposition().getFilename())
        .isEqualTo("schema-plano-de-contas.csv");

    String body = new String(response.getBody(), StandardCharsets.UTF_8);

    // BOM UTF-8 presente para Excel reconhecer acentuação
    assertThat(body).startsWith(UTF8_BOM);

    // Header em PT-BR
    assertThat(body).contains(EXPECTED_HEADER);

    // 1 linha de header + 8 fields = 9 linhas (mais newline trailing)
    long lineCount = body.lines().count();
    assertThat(lineCount).isEqualTo(9);

    // Tipos traduzidos: String → Texto
    assertThat(body).contains(";Texto;");
    // Enums preservados como "Enum"
    assertThat(body).contains(";Enum;");
    // Boolean → Sim/Não
    assertThat(body).contains(";Sim/Não;");

    // required=true → "Sim", required=false → "Não" (campo contaReferencialCodigo)
    assertThat(body).contains("contaReferencialCodigo;Texto;Não;");

    // Enum AccountType com valores
    assertThat(body).contains("ATIVO, PASSIVO");

    // allowedValues dos booleanos contém vírgulas mas NÃO o separador (;),
    // então não precisa de escape com aspas.
    assertThat(body).contains(";true, false, sim, não, nao, yes, no, 1, 0;");

    // Exemplo preservado
    assertThat(body).contains(";1.1.01.001\n");
  }

  @Test
  @DisplayName("GET /conta-referencial/import-schema - retorna CSV com Integer e formato")
  void contaReferencialSchemaReturnsCsv() {
    ResponseEntity<byte[]> response =
        getCsv("/api/v1/conta-referencial/import-schema", adminToken);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType().toString()).startsWith("text/csv");
    assertThat(response.getHeaders().getContentDisposition().getFilename())
        .isEqualTo("schema-conta-referencial.csv");

    String body = new String(response.getBody(), StandardCharsets.UTF_8);
    assertThat(body).startsWith(UTF8_BOM);
    assertThat(body).contains(EXPECTED_HEADER);

    // header + 3 fields
    assertThat(body.lines().count()).isEqualTo(4);

    // Integer → Número inteiro
    assertThat(body).contains("anoValidade;Número inteiro;Não;");
    // String → Texto, com maxLength preenchido (1000) na coluna correta
    assertThat(body).contains("descricao;Texto;Sim;;;;1000;Disponibilidades");
  }

  @Test
  @DisplayName("GET /lancamento-contabil/import-schema - retorna CSV com Data e Número decimal")
  void lancamentoContabilSchemaReturnsCsv() {
    ResponseEntity<byte[]> response =
        getCsv("/api/v1/lancamento-contabil/import-schema", contadorToken);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType().toString()).startsWith("text/csv");
    assertThat(response.getHeaders().getContentDisposition().getFilename())
        .isEqualTo("schema-lancamento-contabil.csv");

    String body = new String(response.getBody(), StandardCharsets.UTF_8);
    assertThat(body).startsWith(UTF8_BOM);
    assertThat(body).contains(EXPECTED_HEADER);

    // header + 6 fields
    assertThat(body.lines().count()).isEqualTo(7);

    // Date → Data, Decimal → Número decimal
    assertThat(body).contains("data;Data;Sim;YYYY-MM-DD;");
    assertThat(body).contains("valor;Número decimal;Sim;");
  }

  @Test
  @DisplayName("GET /lancamento-parte-b/import-schema - retorna CSV com 9 fields")
  void lancamentoParteBSchemaReturnsCsv() {
    ResponseEntity<byte[]> response =
        getCsv("/api/v1/lancamento-parte-b/import-schema", contadorToken);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType().toString()).startsWith("text/csv");
    assertThat(response.getHeaders().getContentDisposition().getFilename())
        .isEqualTo("schema-lancamento-parte-b.csv");

    String body = new String(response.getBody(), StandardCharsets.UTF_8);
    assertThat(body).startsWith(UTF8_BOM);
    assertThat(body).contains(EXPECTED_HEADER);

    // header + 9 fields
    assertThat(body.lines().count()).isEqualTo(10);

    // Cobertura de tipos: Integer, Enum, Texto, Número decimal
    assertThat(body).contains("mesReferencia;Número inteiro;Sim;1 a 12;");
    assertThat(body).contains("tipoApuracao;Enum;Sim;;IRPJ");
    assertThat(body).contains("valor;Número decimal;Sim;");
  }

  @Test
  @DisplayName("GET /plano-de-contas/import-schema - 401 sem autenticação")
  void planoDeContasSchemaRequiresAuth() {
    ResponseEntity<byte[]> response =
        restTemplate.exchange(
            "/api/v1/plano-de-contas/import-schema",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            byte[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
