package br.com.lalurecf.infrastructure.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.PlanoDeContasEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ContaReferencialEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.PlanoDeContasJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.ContaReferencialJpaRepository;
import br.com.lalurecf.infrastructure.dto.planodecontas.ImportPlanoDeContasResponse;
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
 * Testes de integração para PlanoDeContasController.
 *
 * <p>Valida importação de plano de contas via CSV com vinculação a Contas Referenciais RFB.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DisplayName("PlanoDeContasController Integration Tests")
@org.springframework.test.annotation.DirtiesContext(
    classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PlanoDeContasControllerTest {

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

  private Long testCompanyId;
  private Long testContaReferencialId;

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

    // Criar contas referenciais RFB
    testContaReferencialId = createContaReferencial("1.01.01", "Caixa e Equivalentes de Caixa");
    createContaReferencial("1.01.02", "Aplicações Financeiras");
    createContaReferencial("3.01", "Receitas de Vendas");
  }

  private Long createContaReferencial(String codigoRfb, String descricao) {
    ContaReferencialEntity contaReferencial = new ContaReferencialEntity();
    contaReferencial.setCodigoRfb(codigoRfb);
    contaReferencial.setDescricao(descricao);
    contaReferencial.setAnoValidade(2024);
    contaReferencial.setStatus(Status.ACTIVE);
    contaReferencial.setCreatedAt(LocalDateTime.now());
    contaReferencial.setUpdatedAt(LocalDateTime.now());
    return contaReferencialJpaRepository.save(contaReferencial).getId();
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve importar plano de contas com sucesso")
  void shouldImportPlanoDeContasWithSuccess() throws Exception {
    // Arrange
    String csvContent =
        """
        code;name;accountType;contaReferencialCodigo;classe;nivel;natureza;afetaResultado;dedutivel
        1.1.01.001;Caixa;ATIVO;1.01.01;ATIVO_CIRCULANTE;4;DEVEDORA;false;false
        1.1.02.001;Bancos Conta Movimento;ATIVO;1.01.02;ATIVO_CIRCULANTE;4;DEVEDORA;false;false
        3.1.01.001;Receita de Vendas;RECEITA;3.01;RECEITA_BRUTA;4;CREDORA;true;false
        """;

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "plano_contas.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/plano-de-contas/import")
                    .file(file)
                    .param("fiscalYear", "2024")
                    .param("dryRun", "false")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String responseBody = result.getResponse().getContentAsString();
    ImportPlanoDeContasResponse response =
        objectMapper.readValue(responseBody, ImportPlanoDeContasResponse.class);

    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals(3, response.getTotalLines());
    assertEquals(3, response.getProcessedLines());
    assertEquals(0, response.getSkippedLines());
    assertTrue(response.getErrors().isEmpty());

    // Verificar no banco
    assertEquals(3, planoDeContasJpaRepository.count());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve funcionar dry-run sem persistir")
  void shouldWorkDryRunWithoutPersisting() throws Exception {
    // Arrange
    String csvContent =
        """
        code;name;accountType;contaReferencialCodigo;classe;nivel;natureza;afetaResultado;dedutivel
        1.1.01.001;Caixa;ATIVO;1.01.01;ATIVO_CIRCULANTE;4;DEVEDORA;false;false
        1.1.02.001;Bancos Conta Movimento;ATIVO;1.01.02;ATIVO_CIRCULANTE;4;DEVEDORA;false;false
        """;

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "plano_contas.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/plano-de-contas/import")
                    .file(file)
                    .param("fiscalYear", "2024")
                    .param("dryRun", "true")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String responseBody = result.getResponse().getContentAsString();
    ImportPlanoDeContasResponse response =
        objectMapper.readValue(responseBody, ImportPlanoDeContasResponse.class);

    assertNotNull(response);
    assertEquals(2, response.getProcessedLines());
    assertNotNull(response.getPreview());
    assertEquals(2, response.getPreview().size());

    // Verificar que não persistiu no banco
    assertEquals(0, planoDeContasJpaRepository.count());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve registrar erro quando campo obrigatório está faltando")
  void shouldRegisterErrorWhenRequiredFieldIsMissing() throws Exception {
    // Arrange - linha sem "name"
    String csvContent =
        """
        code;name;accountType;contaReferencialCodigo;classe;nivel;natureza;afetaResultado;dedutivel
        1.1.01.001;;ATIVO;1.01.01;ATIVO_CIRCULANTE;4;DEVEDORA;false;false
        """;

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "plano_contas.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/plano-de-contas/import")
                    .file(file)
                    .param("fiscalYear", "2024")
                    .param("dryRun", "false")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String responseBody = result.getResponse().getContentAsString();
    ImportPlanoDeContasResponse response =
        objectMapper.readValue(responseBody, ImportPlanoDeContasResponse.class);

    assertFalse(response.isSuccess());
    assertEquals(0, response.getProcessedLines());
    assertEquals(1, response.getSkippedLines());
    assertEquals(1, response.getErrors().size());
    assertTrue(response.getErrors().get(0).getError().contains("name"));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve registrar erro quando accountType é inválido")
  void shouldRegisterErrorWhenAccountTypeIsInvalid() throws Exception {
    // Arrange
    String csvContent =
        """
        code;name;accountType;contaReferencialCodigo;classe;nivel;natureza;afetaResultado;dedutivel
        1.1.01.001;Caixa;INVALIDO;1.01.01;ATIVO_CIRCULANTE;4;DEVEDORA;false;false
        """;

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "plano_contas.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/plano-de-contas/import")
                    .file(file)
                    .param("fiscalYear", "2024")
                    .param("dryRun", "false")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String responseBody = result.getResponse().getContentAsString();
    ImportPlanoDeContasResponse response =
        objectMapper.readValue(responseBody, ImportPlanoDeContasResponse.class);

    assertFalse(response.isSuccess());
    assertEquals(0, response.getProcessedLines());
    assertEquals(1, response.getErrors().size());
    assertTrue(response.getErrors().get(0).getError().contains("Invalid accountType"));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve registrar erro quando contaReferencialCodigo não existe")
  void shouldRegisterErrorWhenContaReferencialNotFound() throws Exception {
    // Arrange
    String csvContent =
        """
        code;name;accountType;contaReferencialCodigo;classe;nivel;natureza;afetaResultado;dedutivel
        1.1.01.001;Caixa;ATIVO;99.99.99;ATIVO_CIRCULANTE;4;DEVEDORA;false;false
        """;

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "plano_contas.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/plano-de-contas/import")
                    .file(file)
                    .param("fiscalYear", "2024")
                    .param("dryRun", "false")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String responseBody = result.getResponse().getContentAsString();
    ImportPlanoDeContasResponse response =
        objectMapper.readValue(responseBody, ImportPlanoDeContasResponse.class);

    assertFalse(response.isSuccess());
    assertEquals(0, response.getProcessedLines());
    assertEquals(1, response.getErrors().size());
    assertTrue(response.getErrors().get(0).getError().contains("not found"));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve registrar erro quando nivel está fora do range 1-5")
  void shouldRegisterErrorWhenNivelIsOutOfRange() throws Exception {
    // Arrange
    String csvContent =
        """
        code;name;accountType;contaReferencialCodigo;classe;nivel;natureza;afetaResultado;dedutivel
        1.1.01.001;Caixa;ATIVO;1.01.01;ATIVO_CIRCULANTE;10;DEVEDORA;false;false
        """;

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "plano_contas.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/plano-de-contas/import")
                    .file(file)
                    .param("fiscalYear", "2024")
                    .param("dryRun", "false")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String responseBody = result.getResponse().getContentAsString();
    ImportPlanoDeContasResponse response =
        objectMapper.readValue(responseBody, ImportPlanoDeContasResponse.class);

    assertFalse(response.isSuccess());
    assertEquals(0, response.getProcessedLines());
    assertEquals(1, response.getErrors().size());
    assertTrue(response.getErrors().get(0).getError().contains("between 1 and 5"));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve ignorar duplicatas dentro do arquivo")
  void shouldIgnoreDuplicatesInFile() throws Exception {
    // Arrange - código duplicado
    String csvContent =
        """
        code;name;accountType;contaReferencialCodigo;classe;nivel;natureza;afetaResultado;dedutivel
        1.1.01.001;Caixa;ATIVO;1.01.01;ATIVO_CIRCULANTE;4;DEVEDORA;false;false
        1.1.01.001;Caixa Duplicado;ATIVO;1.01.01;ATIVO_CIRCULANTE;4;DEVEDORA;false;false
        """;

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "plano_contas.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/plano-de-contas/import")
                    .file(file)
                    .param("fiscalYear", "2024")
                    .param("dryRun", "false")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String responseBody = result.getResponse().getContentAsString();
    ImportPlanoDeContasResponse response =
        objectMapper.readValue(responseBody, ImportPlanoDeContasResponse.class);

    assertEquals(2, response.getTotalLines());
    assertEquals(1, response.getProcessedLines());
    assertEquals(1, response.getSkippedLines());
    assertEquals(1, response.getErrors().size());
    assertTrue(response.getErrors().get(0).getError().contains("Duplicate code"));

    // Verificar que apenas 1 foi persistido
    assertEquals(1, planoDeContasJpaRepository.count());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve detectar separador vírgula automaticamente")
  void shouldDetectCommaSeparatorAutomatically() throws Exception {
    // Arrange - usando vírgula como separador
    String csvContent =
        """
        code,name,accountType,contaReferencialCodigo,classe,nivel,natureza,afetaResultado,dedutivel
        1.1.01.001,Caixa,ATIVO,1.01.01,ATIVO_CIRCULANTE,4,DEVEDORA,false,false
        """;

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "plano_contas.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/plano-de-contas/import")
                    .file(file)
                    .param("fiscalYear", "2024")
                    .param("dryRun", "false")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String responseBody = result.getResponse().getContentAsString();
    ImportPlanoDeContasResponse response =
        objectMapper.readValue(responseBody, ImportPlanoDeContasResponse.class);

    assertTrue(response.isSuccess());
    assertEquals(1, response.getProcessedLines());
    assertEquals(1, planoDeContasJpaRepository.count());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Deve respeitar encoding UTF-8")
  void shouldRespectUtf8Encoding() throws Exception {
    // Arrange - nome com caracteres acentuados
    String csvContent =
        """
        code;name;accountType;contaReferencialCodigo;classe;nivel;natureza;afetaResultado;dedutivel
        1.1.01.001;Caixa e Equivalentes de Caixa;ATIVO;1.01.01;ATIVO_CIRCULANTE;4;DEVEDORA;false;false
        """;

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "plano_contas.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/plano-de-contas/import")
                    .file(file)
                    .param("fiscalYear", "2024")
                    .param("dryRun", "false")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String responseBody = result.getResponse().getContentAsString();
    ImportPlanoDeContasResponse response =
        objectMapper.readValue(responseBody, ImportPlanoDeContasResponse.class);

    assertTrue(response.isSuccess());
    assertEquals(1, response.getProcessedLines());

    // Verificar que o nome foi salvo corretamente
    PlanoDeContasEntity account = planoDeContasJpaRepository.findAll().get(0);
    assertEquals("Caixa e Equivalentes de Caixa", account.getName());
  }

  // ============================== CRUD MANUAL TESTS ==============================

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Cenário 1: CONTADOR consegue criar conta com todos campos ECF")
  void shouldCreatePlanoDeContasWithAllEcfFields() throws Exception {
    // Arrange
    String requestBody =
        "{"
            + "\"code\": \"1.1.01.999\","
            + "\"name\": \"Conta Teste Manual\","
            + "\"fiscalYear\": 2024,"
            + "\"accountType\": \"ATIVO\","
            + "\"contaReferencialId\": "
            + testContaReferencialId
            + ","
            + "\"classe\": \"ATIVO_CIRCULANTE\","
            + "\"nivel\": 4,"
            + "\"natureza\": \"DEVEDORA\","
            + "\"afetaResultado\": false,"
            + "\"dedutivel\": false"
            + "}";

    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/plano-de-contas")
                .contentType("application/json")
                .content(requestBody)
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isCreated())
        .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("1.1.01.999"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Conta Teste Manual"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.fiscalYear").value(2024))
        .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Cenário 2: Código duplicado para mesma empresa/ano retorna 400")
  void shouldReturn400WhenDuplicateCodeForSameCompanyAndYear() throws Exception {
    // Arrange - Criar primeira conta
    String requestBody =
        "{"
            + "\"code\": \"1.1.01.888\","
            + "\"name\": \"Conta Original\","
            + "\"fiscalYear\": 2024,"
            + "\"accountType\": \"ATIVO\","
            + "\"contaReferencialId\": "
            + testContaReferencialId
            + ","
            + "\"classe\": \"ATIVO_CIRCULANTE\","
            + "\"nivel\": 4,"
            + "\"natureza\": \"DEVEDORA\","
            + "\"afetaResultado\": false,"
            + "\"dedutivel\": false"
            + "}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/plano-de-contas")
                .contentType("application/json")
                .content(requestBody)
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isCreated());

    // Act & Assert - Tentar criar duplicata
    String duplicateRequestBody =
        "{"
            + "\"code\": \"1.1.01.888\","
            + "\"name\": \"Conta Duplicada\","
            + "\"fiscalYear\": 2024,"
            + "\"accountType\": \"ATIVO\","
            + "\"contaReferencialId\": "
            + testContaReferencialId
            + ","
            + "\"classe\": \"ATIVO_CIRCULANTE\","
            + "\"nivel\": 4,"
            + "\"natureza\": \"DEVEDORA\","
            + "\"afetaResultado\": false,"
            + "\"dedutivel\": false"
            + "}";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/plano-de-contas")
                .contentType("application/json")
                .content(duplicateRequestBody)
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Cenário 3: ContaReferencialId inexistente retorna 400")
  void shouldReturn400WhenContaReferencialIdNotFound() throws Exception {
    // Arrange
    String requestBody =
        "{"
            + "\"code\": \"1.1.01.777\","
            + "\"name\": \"Conta Inválida\","
            + "\"fiscalYear\": 2024,"
            + "\"accountType\": \"ATIVO\","
            + "\"contaReferencialId\": 99999,"
            + "\"classe\": \"ATIVO_CIRCULANTE\","
            + "\"nivel\": 4,"
            + "\"natureza\": \"DEVEDORA\","
            + "\"afetaResultado\": false,"
            + "\"dedutivel\": false"
            + "}";

    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/plano-de-contas")
                .contentType("application/json")
                .content(requestBody)
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Cenário 4: ContaReferencialId INACTIVE retorna 400")
  void shouldReturn400WhenContaReferencialIdIsInactive() throws Exception {
    // Arrange - Inativar conta referencial
    ContaReferencialEntity contaReferencial =
        contaReferencialJpaRepository.findById(testContaReferencialId).get();
    contaReferencial.setStatus(Status.INACTIVE);
    contaReferencialJpaRepository.save(contaReferencial);

    String requestBody =
        "{"
            + "\"code\": \"1.1.01.666\","
            + "\"name\": \"Conta com Referencial Inativo\","
            + "\"fiscalYear\": 2024,"
            + "\"accountType\": \"ATIVO\","
            + "\"contaReferencialId\": "
            + testContaReferencialId
            + ","
            + "\"classe\": \"ATIVO_CIRCULANTE\","
            + "\"nivel\": 4,"
            + "\"natureza\": \"DEVEDORA\","
            + "\"afetaResultado\": false,"
            + "\"dedutivel\": false"
            + "}";

    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/plano-de-contas")
                .contentType("application/json")
                .content(requestBody)
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Cenário 5: Nivel fora do range 1-5 retorna 400")
  void shouldReturn400WhenNivelOutOfRange() throws Exception {
    // Arrange
    String requestBody =
        "{"
            + "\"code\": \"1.1.01.555\","
            + "\"name\": \"Conta Nível Inválido\","
            + "\"fiscalYear\": 2024,"
            + "\"accountType\": \"ATIVO\","
            + "\"contaReferencialId\": "
            + testContaReferencialId
            + ","
            + "\"classe\": \"ATIVO_CIRCULANTE\","
            + "\"nivel\": 6,"
            + "\"natureza\": \"DEVEDORA\","
            + "\"afetaResultado\": false,"
            + "\"dedutivel\": false"
            + "}";

    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/plano-de-contas")
                .contentType("application/json")
                .content(requestBody)
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Cenário 6: CONTADOR sem header X-Company-Id recebe 400")
  void shouldReturn400WhenMissingCompanyIdHeader() throws Exception {
    // Arrange
    String requestBody =
        "{"
            + "\"code\": \"1.1.01.444\","
            + "\"name\": \"Conta sem Contexto\","
            + "\"fiscalYear\": 2024,"
            + "\"accountType\": \"ATIVO\","
            + "\"contaReferencialId\": "
            + testContaReferencialId
            + ","
            + "\"classe\": \"ATIVO_CIRCULANTE\","
            + "\"nivel\": 4,"
            + "\"natureza\": \"DEVEDORA\","
            + "\"afetaResultado\": false,"
            + "\"dedutivel\": false"
            + "}";

    // Act & Assert
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/plano-de-contas")
                .contentType("application/json")
                .content(requestBody))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Cenário 7: Listagem retorna apenas contas da empresa no contexto")
  void shouldListOnlyAccountsFromContextCompany() throws Exception {
    // Arrange - Criar conta para company 1
    String requestBody1 =
        "{"
            + "\"code\": \"1.1.01.333\","
            + "\"name\": \"Conta Company 1\","
            + "\"fiscalYear\": 2024,"
            + "\"accountType\": \"ATIVO\","
            + "\"contaReferencialId\": "
            + testContaReferencialId
            + ","
            + "\"classe\": \"ATIVO_CIRCULANTE\","
            + "\"nivel\": 4,"
            + "\"natureza\": \"DEVEDORA\","
            + "\"afetaResultado\": false,"
            + "\"dedutivel\": false"
            + "}";

    mockMvc.perform(
        MockMvcRequestBuilders.post("/api/v1/plano-de-contas")
            .contentType("application/json")
            .content(requestBody1)
            .header("X-Company-Id", testCompanyId.toString()));

    // Criar segunda company
    CompanyEntity company2 = new CompanyEntity();
    company2.setCnpj("11222333000181");
    company2.setRazaoSocial("Empresa 2");
    company2.setStatus(Status.ACTIVE);
    company2.setPeriodoContabil(LocalDate.of(2024, 1, 1));
    company2.setCreatedAt(LocalDateTime.now());
    company2.setUpdatedAt(LocalDateTime.now());
    company2 = companyJpaRepository.save(company2);

    // Criar conta para company 2
    String requestBody2 =
        "{"
            + "\"code\": \"1.1.01.222\","
            + "\"name\": \"Conta Company 2\","
            + "\"fiscalYear\": 2024,"
            + "\"accountType\": \"ATIVO\","
            + "\"contaReferencialId\": "
            + testContaReferencialId
            + ","
            + "\"classe\": \"ATIVO_CIRCULANTE\","
            + "\"nivel\": 4,"
            + "\"natureza\": \"DEVEDORA\","
            + "\"afetaResultado\": false,"
            + "\"dedutivel\": false"
            + "}";

    mockMvc.perform(
        MockMvcRequestBuilders.post("/api/v1/plano-de-contas")
            .contentType("application/json")
            .content(requestBody2)
            .header("X-Company-Id", company2.getId().toString()));

    // Act - Listar contas da company 1
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/plano-de-contas")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert - Deve retornar apenas contas da company 1
    String response = result.getResponse().getContentAsString();
    assertTrue(response.contains("Conta Company 1"));
    assertFalse(response.contains("Conta Company 2"));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Cenário 8: Edição não permite mudar code ou fiscalYear")
  void shouldNotAllowEditingCodeOrFiscalYear() throws Exception {
    // Arrange - Criar conta
    String createRequestBody =
        "{"
            + "\"code\": \"1.1.01.111\","
            + "\"name\": \"Conta Original\","
            + "\"fiscalYear\": 2024,"
            + "\"accountType\": \"ATIVO\","
            + "\"contaReferencialId\": "
            + testContaReferencialId
            + ","
            + "\"classe\": \"ATIVO_CIRCULANTE\","
            + "\"nivel\": 4,"
            + "\"natureza\": \"DEVEDORA\","
            + "\"afetaResultado\": false,"
            + "\"dedutivel\": false"
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/plano-de-contas")
                    .contentType("application/json")
                    .content(createRequestBody)
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andReturn();

    String createResponse = createResult.getResponse().getContentAsString();
    Long accountId = objectMapper.readTree(createResponse).get("id").asLong();

    // Act - Tentar editar
    String updateRequestBody =
        "{"
            + "\"name\": \"Conta Atualizada\","
            + "\"accountType\": \"ATIVO\","
            + "\"contaReferencialId\": "
            + testContaReferencialId
            + ","
            + "\"classe\": \"ATIVO_NAO_CIRCULANTE\","
            + "\"nivel\": 3,"
            + "\"natureza\": \"DEVEDORA\","
            + "\"afetaResultado\": true,"
            + "\"dedutivel\": true"
            + "}";

    MvcResult updateResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.put("/api/v1/plano-de-contas/" + accountId)
                    .contentType("application/json")
                    .content(updateRequestBody)
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert - Code e fiscalYear devem permanecer inalterados
    String updateResponse = updateResult.getResponse().getContentAsString();
    assertTrue(updateResponse.contains("1.1.01.111")); // code não mudou
    assertTrue(updateResponse.contains("2024")); // fiscalYear não mudou
    assertTrue(updateResponse.contains("Conta Atualizada")); // name mudou
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Cenário 9: Toggle status ACTIVE → INACTIVE funciona")
  void shouldToggleStatusFromActiveToInactive() throws Exception {
    // Arrange - Criar conta ACTIVE
    String createRequestBody =
        "{"
            + "\"code\": \"2.1.01.001\","
            + "\"name\": \"Conta para Toggle\","
            + "\"fiscalYear\": 2024,"
            + "\"accountType\": \"PASSIVO\","
            + "\"contaReferencialId\": "
            + testContaReferencialId
            + ","
            + "\"classe\": \"PASSIVO_CIRCULANTE\","
            + "\"nivel\": 4,"
            + "\"natureza\": \"CREDORA\","
            + "\"afetaResultado\": false,"
            + "\"dedutivel\": false"
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/plano-de-contas")
                    .contentType("application/json")
                    .content(createRequestBody)
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andReturn();

    String createResponse = createResult.getResponse().getContentAsString();
    Long accountId = objectMapper.readTree(createResponse).get("id").asLong();

    // Act - Toggle para INACTIVE
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/api/v1/plano-de-contas/" + accountId + "/status")
                .contentType("application/json")
                .content("{\"status\": \"INACTIVE\"}")
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.newStatus").value("INACTIVE"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Cenário 10: Toggle status INACTIVE → ACTIVE funciona")
  void shouldToggleStatusFromInactiveToActive() throws Exception {
    // Arrange - Criar conta e inativar
    String createRequestBody =
        "{"
            + "\"code\": \"2.1.01.002\","
            + "\"name\": \"Conta para Reativar\","
            + "\"fiscalYear\": 2024,"
            + "\"accountType\": \"PASSIVO\","
            + "\"contaReferencialId\": "
            + testContaReferencialId
            + ","
            + "\"classe\": \"PASSIVO_CIRCULANTE\","
            + "\"nivel\": 4,"
            + "\"natureza\": \"CREDORA\","
            + "\"afetaResultado\": false,"
            + "\"dedutivel\": false"
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/plano-de-contas")
                    .contentType("application/json")
                    .content(createRequestBody)
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andReturn();

    String createResponse = createResult.getResponse().getContentAsString();
    Long accountId = objectMapper.readTree(createResponse).get("id").asLong();

    // Inativar primeiro
    mockMvc.perform(
        MockMvcRequestBuilders.patch("/api/v1/plano-de-contas/" + accountId + "/status")
            .contentType("application/json")
            .content("{\"status\": \"INACTIVE\"}")
            .header("X-Company-Id", testCompanyId.toString()));

    // Act - Reativar para ACTIVE
    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/api/v1/plano-de-contas/" + accountId + "/status")
                .contentType("application/json")
                .content("{\"status\": \"ACTIVE\"}")
                .header("X-Company-Id", testCompanyId.toString()))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.newStatus").value("ACTIVE"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Cenário 11: Listagem com filtros (classe, natureza) funciona")
  void shouldListWithFilters() throws Exception {
    // Arrange - Criar conta ATIVO_CIRCULANTE DEVEDORA
    String requestBody1 =
        "{"
            + "\"code\": \"1.1.02.001\","
            + "\"name\": \"Conta Ativo Circulante\","
            + "\"fiscalYear\": 2024,"
            + "\"accountType\": \"ATIVO\","
            + "\"contaReferencialId\": "
            + testContaReferencialId
            + ","
            + "\"classe\": \"ATIVO_CIRCULANTE\","
            + "\"nivel\": 4,"
            + "\"natureza\": \"DEVEDORA\","
            + "\"afetaResultado\": false,"
            + "\"dedutivel\": false"
            + "}";

    mockMvc.perform(
        MockMvcRequestBuilders.post("/api/v1/plano-de-contas")
            .contentType("application/json")
            .content(requestBody1)
            .header("X-Company-Id", testCompanyId.toString()));

    // Criar conta PASSIVO_CIRCULANTE CREDORA
    String requestBody2 =
        "{"
            + "\"code\": \"2.1.01.003\","
            + "\"name\": \"Conta Passivo Circulante\","
            + "\"fiscalYear\": 2024,"
            + "\"accountType\": \"PASSIVO\","
            + "\"contaReferencialId\": "
            + testContaReferencialId
            + ","
            + "\"classe\": \"PASSIVO_CIRCULANTE\","
            + "\"nivel\": 4,"
            + "\"natureza\": \"CREDORA\","
            + "\"afetaResultado\": false,"
            + "\"dedutivel\": false"
            + "}";

    mockMvc.perform(
        MockMvcRequestBuilders.post("/api/v1/plano-de-contas")
            .contentType("application/json")
            .content(requestBody2)
            .header("X-Company-Id", testCompanyId.toString()));

    // Act - Listar filtrando por classe ATIVO_CIRCULANTE
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/plano-de-contas")
                    .param("classe", "ATIVO_CIRCULANTE")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    // Assert
    String response = result.getResponse().getContentAsString();
    assertTrue(response.contains("Conta Ativo Circulante"));
    assertFalse(response.contains("Conta Passivo Circulante"));
  }

  @Test
  @WithMockUser(roles = "CONTADOR")
  @DisplayName("Cenário 12: Listagem com include_inactive=true retorna contas inativas")
  void shouldListIncludingInactiveAccounts() throws Exception {
    // Arrange - Criar conta e inativar
    String createRequestBody =
        "{"
            + "\"code\": \"3.1.01.001\","
            + "\"name\": \"Conta Inativa\","
            + "\"fiscalYear\": 2024,"
            + "\"accountType\": \"RECEITA\","
            + "\"contaReferencialId\": "
            + testContaReferencialId
            + ","
            + "\"classe\": \"RECEITA_BRUTA\","
            + "\"nivel\": 4,"
            + "\"natureza\": \"CREDORA\","
            + "\"afetaResultado\": true,"
            + "\"dedutivel\": false"
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/plano-de-contas")
                    .contentType("application/json")
                    .content(createRequestBody)
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andReturn();

    String createResponse = createResult.getResponse().getContentAsString();
    Long accountId = objectMapper.readTree(createResponse).get("id").asLong();

    // Inativar conta
    mockMvc.perform(
        MockMvcRequestBuilders.patch("/api/v1/plano-de-contas/" + accountId + "/status")
            .contentType("application/json")
            .content("{\"status\": \"INACTIVE\"}")
            .header("X-Company-Id", testCompanyId.toString()));

    // Act - Listar sem includeInactive (não deve aparecer)
    MvcResult resultWithoutInactive =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/plano-de-contas")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    String responseWithoutInactive = resultWithoutInactive.getResponse().getContentAsString();
    assertFalse(responseWithoutInactive.contains("Conta Inativa"));

    // Act - Listar com includeInactive=true (deve aparecer)
    MvcResult resultWithInactive =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/plano-de-contas")
                    .param("includeInactive", "true")
                    .header("X-Company-Id", testCompanyId.toString()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    String responseWithInactive = resultWithInactive.getResponse().getContentAsString();
    assertTrue(responseWithInactive.contains("Conta Inativa"));
  }
}
