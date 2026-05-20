package br.com.lalurecf.infrastructure.adapter.in.rest;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.util.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes de integração para ContaParteBController.
 *
 * <p>Valida todos os endpoints do CRUD de contas da Parte B (e-Lalur/e-Lacs), incluindo:
 *
 * <ul>
 *   <li>Criação de contas (CONTADOR only, com header X-Company-Id)
 *   <li>Listagem com filtros e paginação (CONTADOR com header)
 *   <li>Visualização individual (CONTADOR com header)
 *   <li>Atualização de contas (CONTADOR com header, campos imutáveis)
 *   <li>Toggle de status (CONTADOR com header)
 *   <li>Validações de negócio (unicidade, vigência temporal, saldo)
 *   <li>Controle de contexto (X-Company-Id obrigatório)
 * </ul>
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("ContaParteBController - Testes de Integração")
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
class ContaParteBControllerTest extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CompanyJpaRepository companyRepository;

  private Long companyId;

  @BeforeEach
  void setUp() {
    // Criar empresa de teste
    CompanyEntity company = new CompanyEntity();
    company.setCnpj("12345678000195");
    company.setRazaoSocial("Empresa Teste Conta Parte B");
    company.setStatus(Status.ACTIVE);
    company.setPeriodoContabil(LocalDate.of(2024, 1, 31));
    company.setCreatedAt(LocalDateTime.now());
    company.setUpdatedAt(LocalDateTime.now());
    company = companyRepository.save(company);
    companyId = company.getId();
  }

  @AfterEach
  void tearDown() {
    companyRepository.deleteAll();
  }

  @Test
  @DisplayName("CONTADOR deve conseguir criar conta Parte B com todos campos")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldCreateContaParteBAsContador() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "codigoConta": "4.01.01",
          "descricao": "Adições - Despesas não dedutíveis",
          "anoBase": 2024,
          "dataVigenciaInicio": "2024-01-01",
          "dataVigenciaFim": null,
          "tipoTributo": "IRPJ",
          "saldoInicial": 0.00,
          "tipoSaldo": "DEVEDOR"
        }
        """;

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/conta-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.codigoConta").value("4.01.01"))
        .andExpect(jsonPath("$.descricao").value("Adições - Despesas não dedutíveis"))
        .andExpect(jsonPath("$.anoBase").value(2024))
        .andExpect(jsonPath("$.dataVigenciaInicio").value("2024-01-01"))
        .andExpect(jsonPath("$.dataVigenciaFim").isEmpty())
        .andExpect(jsonPath("$.tipoTributo").value("IRPJ"))
        .andExpect(jsonPath("$.saldoInicial").value(0.0))
        .andExpect(jsonPath("$.tipoSaldo").value("DEVEDOR"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  @DisplayName("Deve retornar 400 quando código duplicado para mesma empresa/ano")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn400WhenCodeAndYearAreDuplicateForSameCompany() throws Exception {
    // Arrange - criar primeira conta
    String requestBody =
        """
        {
          "codigoConta": "4.02.01",
          "descricao": "Exclusões - Receitas não tributáveis",
          "anoBase": 2024,
          "dataVigenciaInicio": "2024-01-01",
          "tipoTributo": "CSLL",
          "saldoInicial": 100.50,
          "tipoSaldo": "CREDOR"
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/conta-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated());

    // Act & Assert - tentar criar conta com mesmo código e ano para mesma empresa
    mockMvc
        .perform(
            post("/api/v1/conta-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Já existe conta Parte B com código '4.02.01' para o ano base 2024"));
  }

  @Test
  @DisplayName("Deve retornar 400 quando dataVigenciaFim < dataVigenciaInicio")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn400WhenDataVigenciaFimBeforeDataVigenciaInicio() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "codigoConta": "4.03.01",
          "descricao": "Conta com vigência inválida",
          "anoBase": 2024,
          "dataVigenciaInicio": "2024-12-31",
          "dataVigenciaFim": "2024-01-01",
          "tipoTributo": "AMBOS",
          "saldoInicial": 0.00,
          "tipoSaldo": "DEVEDOR"
        }
        """;

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/conta-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("dataVigenciaFim must be >= dataVigenciaInicio"));
  }

  @Test
  @DisplayName("Deve retornar 400 quando saldoInicial negativo")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn400WhenSaldoInicialIsNegative() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "codigoConta": "4.04.01",
          "descricao": "Conta com saldo negativo",
          "anoBase": 2024,
          "dataVigenciaInicio": "2024-01-01",
          "tipoTributo": "IRPJ",
          "saldoInicial": -100.00,
          "tipoSaldo": "DEVEDOR"
        }
        """;

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/conta-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("CONTADOR sem header X-Company-Id recebe 400")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn400WhenMissingCompanyIdHeader() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "codigoConta": "4.05.01",
          "descricao": "Conta sem header",
          "anoBase": 2024,
          "dataVigenciaInicio": "2024-01-01",
          "tipoTributo": "IRPJ",
          "saldoInicial": 0.00,
          "tipoSaldo": "DEVEDOR"
        }
        """;

    // Act & Assert - sem header X-Company-Id
    mockMvc
        .perform(
            post("/api/v1/conta-parte-b")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Company context is required (header X-Company-Id missing)"));
  }

  @Test
  @DisplayName("Edição não deve permitir mudar codigoConta ou anoBase")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldNotAllowEditingImmutableFields() throws Exception {
    // Arrange - criar conta
    String createRequest =
        """
        {
          "codigoConta": "4.06.01",
          "descricao": "Descrição Original",
          "anoBase": 2024,
          "dataVigenciaInicio": "2024-01-01",
          "tipoTributo": "IRPJ",
          "saldoInicial": 1000.00,
          "tipoSaldo": "DEVEDOR"
        }
        """;

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/conta-parte-b")
                    .header("X-Company-Id", companyId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequest))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = createResult.getResponse().getContentAsString();
    Long contaId = objectMapper.readTree(responseBody).get("id").asLong();

    // Act - editar conta (sem codigoConta e anoBase no UpdateRequest)
    String updateRequest =
        """
        {
          "descricao": "Descrição Atualizada",
          "dataVigenciaInicio": "2024-02-01",
          "dataVigenciaFim": "2024-12-31",
          "tipoTributo": "CSLL",
          "saldoInicial": 2000.00,
          "tipoSaldo": "CREDOR"
        }
        """;

    mockMvc
        .perform(
            put("/api/v1/conta-parte-b/" + contaId)
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(contaId))
        .andExpect(jsonPath("$.codigoConta").value("4.06.01")) // Imutável - não mudou
        .andExpect(jsonPath("$.anoBase").value(2024)) // Imutável - não mudou
        .andExpect(jsonPath("$.descricao").value("Descrição Atualizada"))
        .andExpect(jsonPath("$.dataVigenciaInicio").value("2024-02-01"))
        .andExpect(jsonPath("$.dataVigenciaFim").value("2024-12-31"))
        .andExpect(jsonPath("$.tipoTributo").value("CSLL"))
        .andExpect(jsonPath("$.saldoInicial").value(2000.0))
        .andExpect(jsonPath("$.tipoSaldo").value("CREDOR"));
  }

  @Test
  @DisplayName("Toggle status deve funcionar")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldToggleStatusSuccessfully() throws Exception {
    // Arrange - criar conta
    String createRequest =
        """
        {
          "codigoConta": "4.07.01",
          "descricao": "Conta para teste de status",
          "anoBase": 2024,
          "dataVigenciaInicio": "2024-01-01",
          "tipoTributo": "AMBOS",
          "saldoInicial": 0.00,
          "tipoSaldo": "DEVEDOR"
        }
        """;

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/conta-parte-b")
                    .header("X-Company-Id", companyId)
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
            patch("/api/v1/conta-parte-b/" + contaId + "/status")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toggleRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.newStatus").value("INACTIVE"));

    // Assert - verificar que status foi alterado
    mockMvc
        .perform(
            get("/api/v1/conta-parte-b/" + contaId)
                .header("X-Company-Id", companyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"));
  }

  @Test
  @DisplayName("Listagem com filtros deve funcionar")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldFilterListingCorrectly() throws Exception {
    // Arrange - criar múltiplas contas
    String conta1 =
        """
        {
          "codigoConta": "5.01.01",
          "descricao": "Conta IRPJ 2024",
          "anoBase": 2024,
          "dataVigenciaInicio": "2024-01-01",
          "tipoTributo": "IRPJ",
          "saldoInicial": 100.00,
          "tipoSaldo": "DEVEDOR"
        }
        """;

    String conta2 =
        """
        {
          "codigoConta": "5.02.01",
          "descricao": "Conta CSLL 2024",
          "anoBase": 2024,
          "dataVigenciaInicio": "2024-01-01",
          "tipoTributo": "CSLL",
          "saldoInicial": 200.00,
          "tipoSaldo": "CREDOR"
        }
        """;

    String conta3 =
        """
        {
          "codigoConta": "5.03.01",
          "descricao": "Conta IRPJ 2025",
          "anoBase": 2025,
          "dataVigenciaInicio": "2025-01-01",
          "tipoTributo": "IRPJ",
          "saldoInicial": 300.00,
          "tipoSaldo": "DEVEDOR"
        }
        """;

    mockMvc.perform(
        post("/api/v1/conta-parte-b")
            .header("X-Company-Id", companyId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(conta1));

    mockMvc.perform(
        post("/api/v1/conta-parte-b")
            .header("X-Company-Id", companyId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(conta2));

    mockMvc.perform(
        post("/api/v1/conta-parte-b")
            .header("X-Company-Id", companyId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(conta3));

    // Act & Assert - filtrar por ano
    mockMvc
        .perform(
            get("/api/v1/conta-parte-b?ano_base=2024")
                .header("X-Company-Id", companyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));

    // Act & Assert - filtrar por tipoTributo
    mockMvc
        .perform(
            get("/api/v1/conta-parte-b?tipo_tributo=IRPJ")
                .header("X-Company-Id", companyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));

    // Act & Assert - busca por search
    mockMvc
        .perform(
            get("/api/v1/conta-parte-b?search=CSLL")
                .header("X-Company-Id", companyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
  }

  @Test
  @DisplayName("Deve retornar apenas contas ACTIVE por padrão")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturnOnlyActiveByDefault() throws Exception {
    // Arrange - criar conta e inativar
    String createRequest =
        """
        {
          "codigoConta": "6.01.01",
          "descricao": "Conta para teste de filtro de status",
          "anoBase": 2024,
          "dataVigenciaInicio": "2024-01-01",
          "tipoTributo": "IRPJ",
          "saldoInicial": 0.00,
          "tipoSaldo": "DEVEDOR"
        }
        """;

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/conta-parte-b")
                    .header("X-Company-Id", companyId)
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

    mockMvc.perform(
        patch("/api/v1/conta-parte-b/" + contaId + "/status")
            .header("X-Company-Id", companyId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(toggleRequest));

    // Act & Assert - listar sem include_inactive (não deve aparecer)
    mockMvc
        .perform(
            get("/api/v1/conta-parte-b?search=6.01.01")
                .header("X-Company-Id", companyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());

    // Act & Assert - listar com include_inactive=true (deve aparecer)
    mockMvc
        .perform(
            get("/api/v1/conta-parte-b?search=6.01.01&include_inactive=true")
                .header("X-Company-Id", companyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(contaId))
        .andExpect(jsonPath("$.content[0].status").value("INACTIVE"));
  }

  @Test
  @DisplayName("CONTADOR deve conseguir visualizar conta Parte B")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldAllowContadorToViewContaParteB() throws Exception {
    // Arrange - criar conta
    String createRequest =
        """
        {
          "codigoConta": "7.01.01",
          "descricao": "Conta para visualização",
          "anoBase": 2024,
          "dataVigenciaInicio": "2024-01-01",
          "tipoTributo": "AMBOS",
          "saldoInicial": 500.00,
          "tipoSaldo": "DEVEDOR"
        }
        """;

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/conta-parte-b")
                    .header("X-Company-Id", companyId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequest))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = createResult.getResponse().getContentAsString();
    Long contaId = objectMapper.readTree(responseBody).get("id").asLong();

    // Act & Assert - visualizar conta
    mockMvc
        .perform(
            get("/api/v1/conta-parte-b/" + contaId)
                .header("X-Company-Id", companyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(contaId))
        .andExpect(jsonPath("$.codigoConta").value("7.01.01"))
        .andExpect(jsonPath("$.descricao").value("Conta para visualização"));
  }

  @Test
  @DisplayName("Deve permitir mesmo código para anos diferentes")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldAllowSameCodeForDifferentYears() throws Exception {
    // Arrange - criar conta para 2024
    String conta2024 =
        """
        {
          "codigoConta": "8.01.01",
          "descricao": "Conta 2024",
          "anoBase": 2024,
          "dataVigenciaInicio": "2024-01-01",
          "tipoTributo": "IRPJ",
          "saldoInicial": 0.00,
          "tipoSaldo": "DEVEDOR"
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/conta-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(conta2024))
        .andExpect(status().isCreated());

    // Act & Assert - criar mesmo código para 2025 (deve permitir)
    String conta2025 =
        """
        {
          "codigoConta": "8.01.01",
          "descricao": "Conta 2025",
          "anoBase": 2025,
          "dataVigenciaInicio": "2025-01-01",
          "tipoTributo": "IRPJ",
          "saldoInicial": 0.00,
          "tipoSaldo": "DEVEDOR"
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/conta-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(conta2025))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.codigoConta").value("8.01.01"))
        .andExpect(jsonPath("$.anoBase").value(2025));
  }
}
