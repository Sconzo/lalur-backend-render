package br.com.lalurecf.infrastructure.adapter.in.rest;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lalurecf.util.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes de integração para ContaReferencialController.
 *
 * <p>Valida todos os endpoints do CRUD de contas referenciais RFB, incluindo:
 *
 * <ul>
 *   <li>Criação de contas (ADMIN only)
 *   <li>Listagem com filtros e paginação (ADMIN e CONTADOR)
 *   <li>Visualização individual (ADMIN e CONTADOR)
 *   <li>Atualização de contas (ADMIN only)
 *   <li>Toggle de status (ADMIN only)
 *   <li>Controle de acesso (CONTADOR pode ler mas não escrever)
 * </ul>
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("ContaReferencialController - Testes de Integração")
class ContaReferencialControllerTest extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  @DisplayName("ADMIN deve conseguir criar conta referencial")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldCreateContaReferencialAsAdmin() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "codigoRfb": "1.01.01",
          "descricao": "Receita de Vendas",
          "anoValidade": 2024
        }
        """;

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.codigoRfb").value("1.01.01"))
        .andExpect(jsonPath("$.descricao").value("Receita de Vendas"))
        .andExpect(jsonPath("$.anoValidade").value(2024))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  @DisplayName("Deve retornar 400 quando código duplicado com mesmo ano de validade")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldReturn400WhenCodeAndYearAreDuplicate() throws Exception {
    // Arrange - criar primeira conta
    String requestBody =
        """
        {
          "codigoRfb": "2.01.01",
          "descricao": "Custo de Mercadorias",
          "anoValidade": 2024
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated());

    // Act & Assert - tentar criar conta com mesmo código e ano
    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value("Já existe conta referencial com código 2.01.01 para o ano 2024"));
  }

  @Test
  @DisplayName("ADMIN deve conseguir criar mesmo código para anos diferentes")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldAllowSameCodeForDifferentYears() throws Exception {
    // Arrange - criar conta para 2024
    String requestBody2024 =
        """
        {
          "codigoRfb": "3.01.01",
          "descricao": "Despesa Administrativa 2024",
          "anoValidade": 2024
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody2024))
        .andExpect(status().isCreated());

    // Act & Assert - criar mesmo código para 2025
    String requestBody2025 =
        """
        {
          "codigoRfb": "3.01.01",
          "descricao": "Despesa Administrativa 2025",
          "anoValidade": 2025
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody2025))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.codigoRfb").value("3.01.01"))
        .andExpect(jsonPath("$.anoValidade").value(2025));
  }

  @Test
  @DisplayName("CONTADOR deve conseguir listar contas referenciais")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldAllowContadorToListContas() throws Exception {
    // Act & Assert
    mockMvc
        .perform(get("/api/v1/conta-referencial"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  @DisplayName("CONTADOR deve conseguir visualizar conta referencial")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldAllowContadorToViewConta() throws Exception {
    // Arrange - criar conta como ADMIN
    String requestBody =
        """
        {
          "codigoRfb": "4.01.01",
          "descricao": "Outras Despesas",
          "anoValidade": 2024
        }
        """;

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/conta-referencial")
                    .with(request -> {
                      request.setRemoteUser("admin@test.com");
                      return request;
                    })
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin@test.com").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = createResult.getResponse().getContentAsString();
    Long contaId =
        objectMapper.readTree(responseBody).get("id").asLong();

    // Act & Assert - CONTADOR consegue visualizar
    mockMvc
        .perform(get("/api/v1/conta-referencial/" + contaId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(contaId))
        .andExpect(jsonPath("$.codigoRfb").value("4.01.01"));
  }

  @Test
  @DisplayName("CONTADOR deve receber 403 ao tentar criar conta referencial")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn403WhenContadorTriesToCreate() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "codigoRfb": "5.01.01",
          "descricao": "Receita Financeira",
          "anoValidade": 2024
        }
        """;

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("CONTADOR deve receber 403 ao tentar editar conta referencial")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn403WhenContadorTriesToUpdate() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "descricao": "Nova Descrição",
          "anoValidade": 2025
        }
        """;

    // Act & Assert
    mockMvc
        .perform(
            put("/api/v1/conta-referencial/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("CONTADOR deve receber 403 ao tentar alternar status")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn403WhenContadorTriesToToggleStatus() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "status": "INACTIVE"
        }
        """;

    // Act & Assert
    mockMvc
        .perform(
            patch("/api/v1/conta-referencial/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("ADMIN deve conseguir editar conta referencial (sem alterar codigoRfb)")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldUpdateContaReferencialAsAdmin() throws Exception {
    // Arrange - criar conta
    String createRequest =
        """
        {
          "codigoRfb": "6.01.01",
          "descricao": "Descrição Original",
          "anoValidade": 2024
        }
        """;

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/conta-referencial")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequest))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = createResult.getResponse().getContentAsString();
    Long contaId = objectMapper.readTree(responseBody).get("id").asLong();

    // Act - editar conta
    String updateRequest =
        """
        {
          "descricao": "Descrição Atualizada",
          "anoValidade": 2025
        }
        """;

    mockMvc
        .perform(
            put("/api/v1/conta-referencial/" + contaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(contaId))
        .andExpect(jsonPath("$.codigoRfb").value("6.01.01")) // Código não muda
        .andExpect(jsonPath("$.descricao").value("Descrição Atualizada"))
        .andExpect(jsonPath("$.anoValidade").value(2025));
  }

  @Test
  @DisplayName("ADMIN deve conseguir alternar status da conta referencial")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldToggleStatusAsAdmin() throws Exception {
    // Arrange - criar conta
    String createRequest =
        """
        {
          "codigoRfb": "7.01.01",
          "descricao": "Conta para teste de status",
          "anoValidade": 2024
        }
        """;

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/conta-referencial")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequest))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = createResult.getResponse().getContentAsString();
    Long contaId = objectMapper.readTree(responseBody).get("id").asLong();

    // Act - alternar para INACTIVE
    String toggleRequest =
        """
        {
          "status": "INACTIVE"
        }
        """;

    mockMvc
        .perform(
            patch("/api/v1/conta-referencial/" + contaId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toggleRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.newStatus").value("INACTIVE"));

    // Assert - verificar que status foi alterado
    mockMvc
        .perform(get("/api/v1/conta-referencial/" + contaId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"));
  }

  @Test
  @DisplayName("Deve filtrar contas por ano de validade")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldFilterByAnoValidade() throws Exception {
    // Arrange - criar contas para anos diferentes
    String conta2024 =
        """
        {
          "codigoRfb": "8.01.01",
          "descricao": "Conta 2024",
          "anoValidade": 2024
        }
        """;

    String conta2025 =
        """
        {
          "codigoRfb": "8.01.02",
          "descricao": "Conta 2025",
          "anoValidade": 2025
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(conta2024))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(conta2025))
        .andExpect(status().isCreated());

    // Act & Assert - filtrar por 2024
    mockMvc
        .perform(get("/api/v1/conta-referencial?ano_validade=2024"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[*].anoValidade").value(hasSize(greaterThanOrEqualTo(1))));
  }

  @Test
  @DisplayName("Deve buscar contas por termo em codigoRfb ou descricao")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldSearchByCodeOrDescription() throws Exception {
    // Arrange - criar conta
    String createRequest =
        """
        {
          "codigoRfb": "9.01.ESPECIAL",
          "descricao": "Conta Especial de Teste",
          "anoValidade": 2024
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
        .andExpect(status().isCreated());

    // Act & Assert - buscar por "ESPECIAL" (presente no código)
    mockMvc
        .perform(get("/api/v1/conta-referencial?search=ESPECIAL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].codigoRfb").value("9.01.ESPECIAL"));

    // Act & Assert - buscar por "Especial" (presente na descrição, case insensitive)
    mockMvc
        .perform(get("/api/v1/conta-referencial?search=Especial"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].descricao").value("Conta Especial de Teste"));
  }

  @Test
  @DisplayName("Deve retornar apenas contas ACTIVE por padrão")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldReturnOnlyActiveByDefault() throws Exception {
    // Arrange - criar conta e inativar
    String createRequest =
        """
        {
          "codigoRfb": "10.01.01",
          "descricao": "Conta para teste de filtro de status",
          "anoValidade": 2024
        }
        """;

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/conta-referencial")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequest))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = createResult.getResponse().getContentAsString();
    Long contaId = objectMapper.readTree(responseBody).get("id").asLong();

    // Inativar conta
    String toggleRequest =
        """
        {
          "status": "INACTIVE"
        }
        """;

    mockMvc
        .perform(
            patch("/api/v1/conta-referencial/" + contaId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toggleRequest))
        .andExpect(status().isOk());

    // Act & Assert - listar sem include_inactive (não deve aparecer)
    mockMvc
        .perform(get("/api/v1/conta-referencial?search=10.01.01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());

    // Act & Assert - listar com include_inactive=true (deve aparecer)
    mockMvc
        .perform(get("/api/v1/conta-referencial?search=10.01.01&include_inactive=true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(contaId))
        .andExpect(jsonPath("$.content[0].status").value("INACTIVE"));
  }

  // ==================== TESTES DE IMPORTAÇÃO CSV ====================

  @Test
  @DisplayName("ADMIN deve conseguir importar contas referenciais via CSV")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldImportContasReferenciaisViaCsv() throws Exception {
    // Arrange - criar arquivo CSV
    String csvContent =
        """
        codigoRfb;descricao;anoValidade
        CSV.01.01;Receita de Vendas CSV;2024
        CSV.01.02;Custo de Mercadorias CSV;2024
        CSV.01.03;Despesas Operacionais CSV;
        """;

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "contas-referenciais.csv", "text/csv", csvContent.getBytes());

    // Act & Assert
    mockMvc
        .perform(
            multipart("/api/v1/conta-referencial/import")
                .file(file)
                .param("dryRun", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.totalLines").value(3))
        .andExpect(jsonPath("$.processedLines").value(3))
        .andExpect(jsonPath("$.skippedLines").value(0))
        .andExpect(jsonPath("$.errors").isEmpty())
        .andExpect(jsonPath("$.preview").doesNotExist());

    // Assert - verificar que contas foram criadas
    mockMvc
        .perform(get("/api/v1/conta-referencial?search=CSV.01.01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].codigoRfb").value("CSV.01.01"))
        .andExpect(jsonPath("$.content[0].descricao").value("Receita de Vendas CSV"));
  }

  @Test
  @DisplayName("ADMIN deve conseguir fazer dry-run de importação CSV")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldDryRunImportContasReferenciais() throws Exception {
    // Arrange - criar arquivo CSV
    String csvContent =
        """
        codigoRfb;descricao;anoValidade
        DRY.01.01;Conta Dry Run 1;2024
        DRY.01.02;Conta Dry Run 2;2025
        """;

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "contas-referenciais.csv", "text/csv", csvContent.getBytes());

    // Act & Assert
    mockMvc
        .perform(
            multipart("/api/v1/conta-referencial/import")
                .file(file)
                .param("dryRun", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.totalLines").value(2))
        .andExpect(jsonPath("$.processedLines").value(2))
        .andExpect(jsonPath("$.skippedLines").value(0))
        .andExpect(jsonPath("$.preview").isArray())
        .andExpect(jsonPath("$.preview[0].codigoRfb").value("DRY.01.01"))
        .andExpect(jsonPath("$.preview[0].descricao").value("Conta Dry Run 1"))
        .andExpect(jsonPath("$.preview[0].anoValidade").value(2024))
        .andExpect(jsonPath("$.preview[1].codigoRfb").value("DRY.01.02"));

    // Assert - verificar que contas NÃO foram criadas (dry-run)
    mockMvc
        .perform(get("/api/v1/conta-referencial?search=DRY.01.01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());
  }

  @Test
  @DisplayName("Importação deve detectar duplicatas no arquivo CSV")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldDetectDuplicatesInCsvFile() throws Exception {
    // Arrange - criar arquivo CSV com duplicatas
    String csvContent =
        """
        codigoRfb;descricao;anoValidade
        DUP.01.01;Conta Original;2024
        DUP.01.02;Conta Diferente;2024
        DUP.01.01;Conta Duplicada;2024
        """;

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "contas-referenciais.csv", "text/csv", csvContent.getBytes());

    // Act & Assert
    mockMvc
        .perform(
            multipart("/api/v1/conta-referencial/import")
                .file(file)
                .param("dryRun", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.totalLines").value(3))
        .andExpect(jsonPath("$.processedLines").value(2))
        .andExpect(jsonPath("$.skippedLines").value(1))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors[0].lineNumber").value(4))
        .andExpect(jsonPath("$.errors[0].error").value(org.hamcrest.Matchers.containsString("Duplicate entry")));
  }

  @Test
  @DisplayName("Importação deve detectar duplicatas no banco de dados")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldDetectDuplicatesInDatabase() throws Exception {
    // Arrange - criar conta existente
    String createRequest =
        """
        {
          "codigoRfb": "EXIST.01.01",
          "descricao": "Conta Existente",
          "anoValidade": 2024
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
        .andExpect(status().isCreated());

    // Arrange - criar arquivo CSV tentando criar a mesma conta
    String csvContent =
        """
        codigoRfb;descricao;anoValidade
        EXIST.01.01;Tentativa de Duplicata;2024
        EXIST.01.02;Conta Nova;2024
        """;

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "contas-referenciais.csv", "text/csv", csvContent.getBytes());

    // Act & Assert
    mockMvc
        .perform(
            multipart("/api/v1/conta-referencial/import")
                .file(file)
                .param("dryRun", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.totalLines").value(2))
        .andExpect(jsonPath("$.processedLines").value(1))
        .andExpect(jsonPath("$.skippedLines").value(1))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors[0].lineNumber").value(2))
        .andExpect(
            jsonPath("$.errors[0].error")
                .value(org.hamcrest.Matchers.containsString("already exists")));
  }

  @Test
  @DisplayName("Importação deve validar campos obrigatórios")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldValidateRequiredFieldsOnImport() throws Exception {
    // Arrange - criar arquivo CSV com campos faltando
    String csvContent =
        """
        codigoRfb;descricao;anoValidade
        VAL.01.01;;2024
        ;Descrição sem código;2024
        """;

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "contas-referenciais.csv", "text/csv", csvContent.getBytes());

    // Act & Assert
    mockMvc
        .perform(
            multipart("/api/v1/conta-referencial/import")
                .file(file)
                .param("dryRun", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.totalLines").value(2))
        .andExpect(jsonPath("$.processedLines").value(0))
        .andExpect(jsonPath("$.skippedLines").value(2))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors[0].lineNumber").value(2))
        .andExpect(jsonPath("$.errors[1].lineNumber").value(3));
  }

  @Test
  @DisplayName("Importação deve validar anoValidade")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldValidateAnoValidadeOnImport() throws Exception {
    // Arrange - criar arquivo CSV com ano inválido
    String csvContent =
        """
        codigoRfb;descricao;anoValidade
        ANO.01.01;Ano Inválido;1999
        ANO.01.02;Ano Válido;2024
        """;

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "contas-referenciais.csv", "text/csv", csvContent.getBytes());

    // Act & Assert
    mockMvc
        .perform(
            multipart("/api/v1/conta-referencial/import")
                .file(file)
                .param("dryRun", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalLines").value(2))
        .andExpect(jsonPath("$.processedLines").value(1))
        .andExpect(jsonPath("$.skippedLines").value(1))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors[0].lineNumber").value(2))
        .andExpect(
            jsonPath("$.errors[0].error").value(org.hamcrest.Matchers.containsString("anoValidade")));
  }

  @Test
  @DisplayName("Importação deve suportar separador vírgula")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldSupportCommaSeparator() throws Exception {
    // Arrange - criar arquivo CSV com vírgula como separador
    String csvContent =
        """
        codigoRfb,descricao,anoValidade
        COMMA.01.01,Receita com Vírgula,2024
        COMMA.01.02,Custo com Vírgula,2025
        """;

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "contas-referenciais.csv", "text/csv", csvContent.getBytes());

    // Act & Assert
    mockMvc
        .perform(
            multipart("/api/v1/conta-referencial/import")
                .file(file)
                .param("dryRun", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.totalLines").value(2))
        .andExpect(jsonPath("$.processedLines").value(2));

    // Assert - verificar que conta foi criada
    mockMvc
        .perform(get("/api/v1/conta-referencial?search=COMMA.01.01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].descricao").value("Receita com Vírgula"));
  }

  @Test
  @DisplayName("CONTADOR deve receber 403 ao tentar importar CSV")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn403WhenContadorTriesToImport() throws Exception {
    // Arrange - criar arquivo CSV
    String csvContent =
        """
        codigoRfb;descricao;anoValidade
        FORBIDDEN.01.01;Tentativa do Contador;2024
        """;

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "contas-referenciais.csv", "text/csv", csvContent.getBytes());

    // Act & Assert
    mockMvc
        .perform(
            multipart("/api/v1/conta-referencial/import")
                .file(file)
                .param("dryRun", "false"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Importação deve rejeitar arquivo vazio")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldRejectEmptyFile() throws Exception {
    // Arrange - criar arquivo vazio
    MockMultipartFile file =
        new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

    // Act & Assert
    mockMvc
        .perform(
            multipart("/api/v1/conta-referencial/import")
                .file(file)
                .param("dryRun", "false"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Importação deve rejeitar formato de arquivo inválido")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldRejectInvalidFileFormat() throws Exception {
    // Arrange - criar arquivo com extensão inválida
    String content = "conteúdo qualquer";
    MockMultipartFile file =
        new MockMultipartFile("file", "invalid.pdf", "application/pdf", content.getBytes());

    // Act & Assert
    mockMvc
        .perform(
            multipart("/api/v1/conta-referencial/import")
                .file(file)
                .param("dryRun", "false"))
        .andExpect(status().isBadRequest());
  }
}
