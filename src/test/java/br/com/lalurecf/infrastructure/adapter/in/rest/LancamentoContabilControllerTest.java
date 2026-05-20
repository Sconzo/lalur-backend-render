package br.com.lalurecf.infrastructure.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.lalurecf.domain.enums.AccountType;
import br.com.lalurecf.domain.enums.ClasseContabil;
import br.com.lalurecf.domain.enums.NaturezaConta;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.PlanoDeContasEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ContaReferencialEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.PlanoDeContasJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.ContaReferencialJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.LancamentoContabilJpaRepository;
import br.com.lalurecf.infrastructure.dto.lancamentocontabil.ImportLancamentoContabilResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testes de integração para LancamentoContabilController.
 *
 * <p>Valida importação de lançamentos contábeis via CSV com partidas dobradas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DisplayName("LancamentoContabilController Integration Tests")
@org.springframework.test.annotation.DirtiesContext(
    classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LancamentoContabilControllerTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15.5-alpine")
          .withDatabaseName("ecf_test_db")
          .withUsername("test_user")
          .withPassword("test_pass");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CompanyJpaRepository companyJpaRepository;

  @Autowired private PlanoDeContasJpaRepository planoDeContasJpaRepository;

  @Autowired private ContaReferencialJpaRepository contaReferencialJpaRepository;

  @Autowired private LancamentoContabilJpaRepository lancamentoContabilJpaRepository;

  private Long testCompanyId;
  private Long planoDeContasIdDebito;
  private Long planoDeContasIdCredito;

  @BeforeEach
  void setUp() {
    // Criar empresa de teste
    CompanyEntity company = new CompanyEntity();
    company.setCnpj("12345678000195");
    company.setRazaoSocial("Empresa Teste Importação");
    company.setStatus(Status.ACTIVE);
    company.setPeriodoContabil(LocalDate.of(2024, 1, 1));
    company.setCreatedAt(LocalDateTime.now());
    company.setUpdatedAt(LocalDateTime.now());
    company = companyJpaRepository.save(company);
    testCompanyId = company.getId();

    // Criar conta referencial
    ContaReferencialEntity contaReferencial = new ContaReferencialEntity();
    contaReferencial.setCodigoRfb("1.01");
    contaReferencial.setDescricao("Ativo Circulante");
    contaReferencial.setAnoValidade(2024);
    contaReferencial.setCreatedAt(LocalDateTime.now());
    contaReferencial.setUpdatedAt(LocalDateTime.now());
    contaReferencial = contaReferencialJpaRepository.save(contaReferencial);

    // Criar contas do plano de contas
    planoDeContasIdDebito =
        createPlanoDeContas(company, contaReferencial, "1.1.01.001", "Caixa", AccountType.ATIVO);
    planoDeContasIdCredito =
        createPlanoDeContas(
            company, contaReferencial, "3.1.01.001", "Receita de Vendas", AccountType.RECEITA);
    createPlanoDeContas(
        company, contaReferencial, "5.1.01.001", "Despesas Operacionais", AccountType.DESPESA);
  }

  private Long createPlanoDeContas(
      CompanyEntity company,
      ContaReferencialEntity contaReferencial,
      String code,
      String name,
      AccountType accountType) {
    PlanoDeContasEntity account =
        PlanoDeContasEntity.builder()
            .company(company)
            .contaReferencial(contaReferencial)
            .code(code)
            .name(name)
            .fiscalYear(2024)
            .accountType(accountType)
            .classe(ClasseContabil.ANALITICO)
            .nivel(4)
            .natureza(NaturezaConta.DEVEDORA)
            .afetaResultado(false)
            .dedutivel(false)
            .status(Status.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    return planoDeContasJpaRepository.save(account).getId();
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve importar lançamentos contábeis com sucesso")
  void shouldImportLancamentosWithSuccess() throws Exception {
    // Arrange
    String csvContent =
        "contaDebitoCode;contaCreditoCode;data;valor;historico;numeroDocumento\n"
            + "1.1.01.001;3.1.01.001;2024-01-15;1000.00;Venda mercadoria;NF-123\n"
            + "5.1.01.001;1.1.01.001;2024-01-20;500.00;Pagamento fornecedor;DOC-456\n";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "lancamentos.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/lancamento-contabil/import")
                    .file(file)
                    .param("fiscalYear", "2024")
                    .param("dryRun", "false")
                    .header("X-Company-Id", testCompanyId.toString())
                    .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String responseBody = result.getResponse().getContentAsString();
    ImportLancamentoContabilResponse response =
        objectMapper.readValue(responseBody, ImportLancamentoContabilResponse.class);

    assertTrue(response.isSuccess());
    assertEquals(2, response.getTotalLines());
    assertEquals(2, response.getProcessedLines());
    assertEquals(0, response.getSkippedLines());
    assertTrue(response.getErrors().isEmpty());

    // Verificar que lançamentos foram persistidos
    assertEquals(2, lancamentoContabilJpaRepository.count());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve detectar separador vírgula automaticamente")
  void shouldAutoDetectCommaSeparator() throws Exception {
    // Arrange - usando vírgula como separador
    String csvContent =
        "contaDebitoCode,contaCreditoCode,data,valor,historico,numeroDocumento\n"
            + "1.1.01.001,3.1.01.001,2024-01-15,1000.00,Venda mercadoria,NF-123\n";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "lancamentos.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/lancamento-contabil/import")
                    .file(file)
                    .param("fiscalYear", "2024")
                    .param("dryRun", "false")
                    .header("X-Company-Id", testCompanyId.toString())
                    .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String responseBody = result.getResponse().getContentAsString();
    ImportLancamentoContabilResponse response =
        objectMapper.readValue(responseBody, ImportLancamentoContabilResponse.class);

    assertTrue(response.isSuccess());
    assertEquals(1, response.getProcessedLines());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve registrar erro quando conta não existe")
  void shouldRegisterErrorWhenAccountNotFound() throws Exception {
    // Arrange
    String csvContent =
        "contaDebitoCode;contaCreditoCode;data;valor;historico;numeroDocumento\n"
            + "9.9.99.999;3.1.01.001;2024-01-15;1000.00;Venda mercadoria;NF-123\n";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "lancamentos.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/lancamento-contabil/import")
                    .file(file)
                    .param("fiscalYear", "2024")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String responseBody = result.getResponse().getContentAsString();
    ImportLancamentoContabilResponse response =
        objectMapper.readValue(responseBody, ImportLancamentoContabilResponse.class);

    assertFalse(response.isSuccess());
    assertEquals(1, response.getSkippedLines());
    assertEquals(1, response.getErrors().size());
    assertTrue(
        response.getErrors().get(0).getError().contains("Account code '9.9.99.999' not found"));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve registrar erro quando débito = crédito")
  void shouldRegisterErrorWhenDebitoEqualsCredito() throws Exception {
    // Arrange
    String csvContent =
        "contaDebitoCode;contaCreditoCode;data;valor;historico;numeroDocumento\n"
            + "1.1.01.001;1.1.01.001;2024-01-15;1000.00;Lançamento inválido;NF-123\n";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "lancamentos.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/lancamento-contabil/import")
                    .file(file)
                    .param("fiscalYear", "2024")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String responseBody = result.getResponse().getContentAsString();
    ImportLancamentoContabilResponse response =
        objectMapper.readValue(responseBody, ImportLancamentoContabilResponse.class);

    assertFalse(response.isSuccess());
    assertEquals(1, response.getSkippedLines());
    assertTrue(
        response
            .getErrors()
            .get(0)
            .getError()
            .contains("Debit and credit accounts must be different"));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve registrar erro quando data é anterior a Período Contábil")
  void shouldRegisterErrorWhenDateBeforePeriodoContabil() throws Exception {
    // Arrange
    String csvContent =
        "contaDebitoCode;contaCreditoCode;data;valor;historico;numeroDocumento\n"
            + "1.1.01.001;3.1.01.001;2023-12-31;1000.00;Lançamento antigo;NF-123\n";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "lancamentos.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/lancamento-contabil/import")
                    .file(file)
                    .param("fiscalYear", "2024")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String responseBody = result.getResponse().getContentAsString();
    ImportLancamentoContabilResponse response =
        objectMapper.readValue(responseBody, ImportLancamentoContabilResponse.class);

    assertFalse(response.isSuccess());
    assertEquals(1, response.getSkippedLines());
    assertTrue(response.getErrors().get(0).getError().contains("is before"));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve registrar erro quando valor é zero ou negativo")
  void shouldRegisterErrorWhenValueIsZeroOrNegative() throws Exception {
    // Arrange
    String csvContent =
        "contaDebitoCode;contaCreditoCode;data;valor;historico;numeroDocumento\n"
            + "1.1.01.001;3.1.01.001;2024-01-15;0;Valor zero;NF-123\n"
            + "1.1.01.001;3.1.01.001;2024-01-15;-100.00;Valor negativo;NF-124\n";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "lancamentos.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/lancamento-contabil/import")
                    .file(file)
                    .param("fiscalYear", "2024")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String responseBody = result.getResponse().getContentAsString();
    ImportLancamentoContabilResponse response =
        objectMapper.readValue(responseBody, ImportLancamentoContabilResponse.class);

    assertFalse(response.isSuccess());
    assertEquals(2, response.getSkippedLines());
    assertEquals(2, response.getErrors().size());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve retornar preview quando dryRun=true")
  void shouldReturnPreviewWhenDryRun() throws Exception {
    // Arrange
    String csvContent =
        "contaDebitoCode;contaCreditoCode;data;valor;historico;numeroDocumento\n"
            + "1.1.01.001;3.1.01.001;2024-01-15;1000.00;Venda mercadoria;NF-123\n";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "lancamentos.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/lancamento-contabil/import")
                    .file(file)
                    .param("fiscalYear", "2024")
                    .param("dryRun", "true")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String responseBody = result.getResponse().getContentAsString();
    ImportLancamentoContabilResponse response =
        objectMapper.readValue(responseBody, ImportLancamentoContabilResponse.class);

    assertTrue(response.isSuccess());
    assertNotNull(response.getPreview());
    assertEquals(1, response.getPreview().size());
    assertEquals("1.1.01.001", response.getPreview().get(0).getContaDebitoCode());
    assertEquals("3.1.01.001", response.getPreview().get(0).getContaCreditoCode());

    // Verificar que nenhum lançamento foi persistido
    assertEquals(0, lancamentoContabilJpaRepository.count());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve retornar 400 quando fiscalYear ausente")
  void shouldReturn400WhenFiscalYearMissing() throws Exception {
    // Arrange
    String csvContent =
        "contaDebitoCode;contaCreditoCode;data;valor;historico;numeroDocumento\n"
            + "1.1.01.001;3.1.01.001;2024-01-15;1000.00;Venda mercadoria;NF-123\n";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "lancamentos.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.multipart("/api/v1/lancamento-contabil/import")
                .file(file)
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve exportar lançamentos contábeis com sucesso")
  void shouldExportLancamentosWithSuccess() throws Exception {
    // Arrange - importar 2 lançamentos primeiro
    String csvContent =
        "contaDebitoCode;contaCreditoCode;data;valor;historico;numeroDocumento\n"
            + "1.1.01.001;3.1.01.001;2024-01-15;1000.00;Venda mercadoria;NF-123\n"
            + "5.1.01.001;1.1.01.001;2024-01-20;500.00;Pagamento fornecedor;DOC-456\n";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "lancamentos.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            MockMvcRequestBuilders.multipart("/api/v1/lancamento-contabil/import")
                .file(file)
                .param("fiscalYear", "2024")
                .param("dryRun", "false")
                .header("X-Company-Id", testCompanyId.toString()));

    // Act - exportar
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/lancamento-contabil/export")
                    .param("fiscalYear", "2024")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                MockMvcResultMatchers.header()
                    .string("Content-Type", "text/csv;charset=UTF-8"))
            .andReturn();

    // Assert
    String exported = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    assertNotNull(exported);
    assertTrue(exported.contains("contaDebitoCode"));
    assertTrue(exported.contains("1.1.01.001"));
    assertTrue(exported.contains("Caixa"));
    assertTrue(exported.contains("3.1.01.001"));
    assertTrue(exported.contains("Receita de Vendas"));
    assertTrue(exported.contains("1000.00"));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve exportar com filtro de range de data")
  void shouldExportWithDateRangeFilter() throws Exception {
    // Arrange - importar 3 lançamentos em datas diferentes
    String csvContent =
        "contaDebitoCode;contaCreditoCode;data;valor;historico;numeroDocumento\n"
            + "1.1.01.001;3.1.01.001;2024-01-10;100.00;Venda 1;NF-1\n"
            + "1.1.01.001;3.1.01.001;2024-02-15;200.00;Venda 2;NF-2\n"
            + "1.1.01.001;3.1.01.001;2024-03-20;300.00;Venda 3;NF-3\n";

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "lancamentos.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    mockMvc.perform(
        MockMvcRequestBuilders.multipart("/api/v1/lancamento-contabil/import")
            .file(file)
            .param("fiscalYear", "2024")
            .header("X-Company-Id", testCompanyId.toString()));

    // Act - exportar apenas fevereiro
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/lancamento-contabil/export")
                    .param("fiscalYear", "2024")
                    .param("dataInicio", "2024-02-01")
                    .param("dataFim", "2024-02-28")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String exported = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    assertTrue(exported.contains("2024-02-15"));
    assertTrue(exported.contains("200.00"));
    assertFalse(exported.contains("2024-01-10"));
    assertFalse(exported.contains("2024-03-20"));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve retornar 400 quando dataInicio fornecido sem dataFim")
  void shouldReturn400WhenDataInicioWithoutDataFim() throws Exception {
    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/v1/lancamento-contabil/export")
                .param("fiscalYear", "2024")
                .param("dataInicio", "2024-01-01")
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve retornar 400 quando dataFim < dataInicio")
  void shouldReturn400WhenDataFimBeforeDataInicio() throws Exception {
    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/v1/lancamento-contabil/export")
                .param("fiscalYear", "2024")
                .param("dataInicio", "2024-02-01")
                .param("dataFim", "2024-01-01")
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve retornar 404 quando nenhum lançamento encontrado")
  void shouldReturn404WhenNoLancamentosFound() throws Exception {
    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/v1/lancamento-contabil/export")
                .param("fiscalYear", "2024")
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isNotFound());
  }

  // ============================== CRUD MANUAL TESTS ==============================

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve criar lançamento contábil com partidas dobradas")
  void shouldCreateLancamentoContabil() throws Exception {
    // Arrange
    String requestBody =
        "{"
            + "\"contaDebitoId\": "
            + planoDeContasIdDebito
            + ","
            + "\"contaCreditoId\": "
            + planoDeContasIdCredito
            + ","
            + "\"data\": \"2024-03-15\","
            + "\"valor\": 500.00,"
            + "\"historico\": \"Lançamento de teste manual\","
            + "\"numeroDocumento\": \"DOC-001\","
            + "\"fiscalYear\": 2024"
            + "}";

    // Act
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/lancamento-contabil")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andReturn();

    // Assert
    String response = result.getResponse().getContentAsString();
    assertTrue(response.contains("\"valor\":500.0"));
    assertTrue(response.contains("\"historico\":\"Lançamento de teste manual\""));
    assertEquals(1, lancamentoContabilJpaRepository.count());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve retornar 400 quando débito = crédito")
  void shouldReturn400WhenDebitoEqualsCredito() throws Exception {
    // Arrange
    String requestBody =
        "{"
            + "\"contaDebitoId\": "
            + planoDeContasIdDebito
            + ","
            + "\"contaCreditoId\": "
            + planoDeContasIdDebito
            + ","
            + "\"data\": \"2024-03-15\","
            + "\"valor\": 500.00,"
            + "\"historico\": \"Lançamento inválido\","
            + "\"fiscalYear\": 2024"
            + "}";

    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/lancamento-contabil")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve retornar 400 quando data < Período Contábil")
  void shouldReturn400WhenDataBeforePeriodoContabil() throws Exception {
    // Arrange
    String requestBody =
        "{"
            + "\"contaDebitoId\": "
            + planoDeContasIdDebito
            + ","
            + "\"contaCreditoId\": "
            + planoDeContasIdCredito
            + ","
            + "\"data\": \"2023-12-31\","
            + "\"valor\": 500.00,"
            + "\"historico\": \"Lançamento em período fechado\","
            + "\"fiscalYear\": 2024"
            + "}";

    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/lancamento-contabil")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve retornar 400 quando valor <= 0")
  void shouldReturn400WhenValorZeroOrNegative() throws Exception {
    // Arrange
    String requestBody =
        "{"
            + "\"contaDebitoId\": "
            + planoDeContasIdDebito
            + ","
            + "\"contaCreditoId\": "
            + planoDeContasIdCredito
            + ","
            + "\"data\": \"2024-03-15\","
            + "\"valor\": 0.00,"
            + "\"historico\": \"Lançamento com valor zero\","
            + "\"fiscalYear\": 2024"
            + "}";

    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/lancamento-contabil")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve listar lançamentos com filtros")
  void shouldListLancamentosWithFilters() throws Exception {
    // Arrange - Importar lançamentos primeiro
    String csvContent =
        "contaDebitoCode;contaCreditoCode;data;valor;historico;numeroDocumento\n"
            + "1.1.01.001;3.1.01.001;2024-01-10;100.00;Histórico 1;DOC-001\n"
            + "1.1.01.001;3.1.01.001;2024-02-15;200.00;Histórico 2;DOC-002";

    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "lancamentos.csv",
            "text/csv",
            csvContent.getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            MockMvcRequestBuilders.multipart("/api/v1/lancamento-contabil/import")
                .file(file)
                .param("fiscalYear", "2024")
                .param("dryRun", "false")
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isOk());

    // Act - Listar com filtro por fiscalYear
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/lancamento-contabil")
                    .param("fiscalYear", "2024")
                    .param("page", "0")
                    .param("size", "10")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String response = result.getResponse().getContentAsString();
    assertTrue(response.contains("\"totalElements\":2"));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve buscar lançamento por ID")
  void shouldGetLancamentoById() throws Exception {
    // Arrange - Criar lançamento
    String requestBody =
        "{"
            + "\"contaDebitoId\": "
            + planoDeContasIdDebito
            + ","
            + "\"contaCreditoId\": "
            + planoDeContasIdCredito
            + ","
            + "\"data\": \"2024-03-15\","
            + "\"valor\": 500.00,"
            + "\"historico\": \"Lançamento de teste\","
            + "\"fiscalYear\": 2024"
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/lancamento-contabil")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andReturn();

    String createResponse = createResult.getResponse().getContentAsString();
    Long lancamentoId =
        objectMapper.readTree(createResponse).get("id").asLong();

    // Act - Buscar por ID
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/lancamento-contabil/" + lancamentoId)
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String response = result.getResponse().getContentAsString();
    assertTrue(response.contains("\"id\":" + lancamentoId));
    assertTrue(response.contains("\"valor\":500.0"));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve atualizar lançamento contábil")
  void shouldUpdateLancamentoContabil() throws Exception {
    // Arrange - Criar lançamento
    String createRequestBody =
        "{"
            + "\"contaDebitoId\": "
            + planoDeContasIdDebito
            + ","
            + "\"contaCreditoId\": "
            + planoDeContasIdCredito
            + ","
            + "\"data\": \"2024-03-15\","
            + "\"valor\": 500.00,"
            + "\"historico\": \"Lançamento original\","
            + "\"fiscalYear\": 2024"
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/lancamento-contabil")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequestBody)
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andReturn();

    String createResponse = createResult.getResponse().getContentAsString();
    Long lancamentoId =
        objectMapper.readTree(createResponse).get("id").asLong();

    // Arrange - Request de update
    String updateRequestBody =
        "{"
            + "\"contaDebitoId\": "
            + planoDeContasIdDebito
            + ","
            + "\"contaCreditoId\": "
            + planoDeContasIdCredito
            + ","
            + "\"data\": \"2024-03-20\","
            + "\"valor\": 750.00,"
            + "\"historico\": \"Lançamento atualizado\""
            + "}";

    // Act - Atualizar
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.put("/api/v1/lancamento-contabil/" + lancamentoId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequestBody)
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String response = result.getResponse().getContentAsString();
    assertTrue(response.contains("\"valor\":750.0"));
    assertTrue(response.contains("\"historico\":\"Lançamento atualizado\""));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve retornar 400 ao tentar atualizar lançamento com data < Período Contábil")
  void shouldReturn400WhenUpdatingLancamentoBeforePeriodoContabil() throws Exception {
    // Arrange - Criar lançamento
    String createRequestBody =
        "{"
            + "\"contaDebitoId\": "
            + planoDeContasIdDebito
            + ","
            + "\"contaCreditoId\": "
            + planoDeContasIdCredito
            + ","
            + "\"data\": \"2024-03-15\","
            + "\"valor\": 500.00,"
            + "\"historico\": \"Lançamento original\","
            + "\"fiscalYear\": 2024"
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/lancamento-contabil")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequestBody)
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andReturn();

    String createResponse = createResult.getResponse().getContentAsString();
    Long lancamentoId =
        objectMapper.readTree(createResponse).get("id").asLong();

    // Arrange - Request de update com data < Período Contábil
    String updateRequestBody =
        "{"
            + "\"contaDebitoId\": "
            + planoDeContasIdDebito
            + ","
            + "\"contaCreditoId\": "
            + planoDeContasIdCredito
            + ","
            + "\"data\": \"2023-12-31\","
            + "\"valor\": 750.00,"
            + "\"historico\": \"Lançamento em período fechado\""
            + "}";

    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/api/v1/lancamento-contabil/" + lancamentoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequestBody)
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve alternar status de lançamento (ACTIVE → INACTIVE)")
  void shouldToggleStatusFromActiveToInactive() throws Exception {
    // Arrange - Criar lançamento
    String requestBody =
        "{"
            + "\"contaDebitoId\": "
            + planoDeContasIdDebito
            + ","
            + "\"contaCreditoId\": "
            + planoDeContasIdCredito
            + ","
            + "\"data\": \"2024-03-15\","
            + "\"valor\": 500.00,"
            + "\"historico\": \"Lançamento de teste\","
            + "\"fiscalYear\": 2024"
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/lancamento-contabil")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andReturn();

    String createResponse = createResult.getResponse().getContentAsString();
    Long lancamentoId =
        objectMapper.readTree(createResponse).get("id").asLong();

    // Act - Toggle status
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.patch(
                        "/api/v1/lancamento-contabil/" + lancamentoId + "/status")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String response = result.getResponse().getContentAsString();
    assertTrue(response.contains("\"newStatus\":\"INACTIVE\""));
    assertTrue(response.contains("\"previousStatus\":\"ACTIVE\""));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve retornar 400 ao tentar inativar lançamento com data < Período Contábil")
  void shouldReturn400WhenTogglingStatusOfLancamentoBeforePeriodoContabil() throws Exception {
    // Arrange - Atualizar Período Contábil para data futura
    CompanyEntity company = companyJpaRepository.findById(testCompanyId).get();
    company.setPeriodoContabil(LocalDate.of(2024, 6, 1));
    companyJpaRepository.save(company);

    // Criar lançamento
    String requestBody =
        "{"
            + "\"contaDebitoId\": "
            + planoDeContasIdDebito
            + ","
            + "\"contaCreditoId\": "
            + planoDeContasIdCredito
            + ","
            + "\"data\": \"2024-03-15\","
            + "\"valor\": 500.00,"
            + "\"historico\": \"Lançamento antigo\","
            + "\"fiscalYear\": 2024"
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/lancamento-contabil")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andReturn();

    String createResponse = createResult.getResponse().getContentAsString();
    Long lancamentoId =
        objectMapper.readTree(createResponse).get("id").asLong();

    // Atualizar Período Contábil para bloquear toggle
    company = companyJpaRepository.findById(testCompanyId).get();
    company.setPeriodoContabil(LocalDate.of(2024, 4, 1));
    companyJpaRepository.save(company);

    // Act & Assert - Tentar toggle status
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(
                    "/api/v1/lancamento-contabil/" + lancamentoId + "/status")
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }
}
