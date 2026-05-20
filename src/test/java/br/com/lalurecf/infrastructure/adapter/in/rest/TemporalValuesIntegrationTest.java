package br.com.lalurecf.infrastructure.adapter.in.rest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lalurecf.domain.enums.ParameterNature;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.UserRole;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyTaxParameterEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterTypeEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyTaxParameterJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.TaxParameterJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.TaxParameterTypeJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.ValorParametroTemporalJpaRepository;
import br.com.lalurecf.infrastructure.security.JwtTokenProvider;
import br.com.lalurecf.util.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de integração para valores temporais de parâmetros tributários.
 *
 * <p>Gestão de Valores Temporais de Parâmetros Tributários.
 */
@DisplayName("Temporal Values - Testes de Integração")
@AutoConfigureMockMvc
class TemporalValuesIntegrationTest extends IntegrationTestBase {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private CompanyJpaRepository companyRepository;

  @Autowired
  private TaxParameterJpaRepository taxParameterRepository;

  @Autowired
  private TaxParameterTypeJpaRepository taxParameterTypeRepository;

  @Autowired
  private CompanyTaxParameterJpaRepository companyTaxParameterRepository;

  @Autowired
  private ValorParametroTemporalJpaRepository valorParametroTemporalRepository;

  @Autowired
  private ObjectMapper objectMapper;

  private String adminToken;
  private String contadorToken;
  private Long companyId;
  private Long taxParameterId;

  @BeforeEach
  void setUp() {
    // Gerar tokens
    adminToken = jwtTokenProvider.generateAccessToken("admin@example.com", UserRole.ADMIN);
    contadorToken = jwtTokenProvider.generateAccessToken("contador@example.com",
        UserRole.CONTADOR);

    // Criar empresa de teste
    CompanyEntity company = new CompanyEntity();
    company.setCnpj("12345678000195");
    company.setRazaoSocial("Empresa Teste LTDA");
    company.setStatus(Status.ACTIVE);
    company.setPeriodoContabil(LocalDate.of(2024, 1, 31));
    company.setCreatedAt(LocalDateTime.now());
    company.setUpdatedAt(LocalDateTime.now());
    company = companyRepository.save(company);
    companyId = company.getId();

    // Criar tipo de parâmetro tributário
    TaxParameterTypeEntity taxParameterType =
        TaxParameterTypeEntity.builder()
            .descricao("REGIME")
            .natureza(ParameterNature.GLOBAL)
            .status(Status.ACTIVE)
            .build();
    taxParameterType = taxParameterTypeRepository.save(taxParameterType);

    // Criar parâmetro tributário de teste
    TaxParameterEntity taxParameter =
        TaxParameterEntity.builder()
            .codigo("REGIME_TRIBUTARIO")
            .descricao("Regime Tributário")
            .tipoParametro(taxParameterType)
            .status(Status.ACTIVE)
            .build();
    taxParameter = taxParameterRepository.save(taxParameter);
    taxParameterId = taxParameter.getId();

    // Criar associação empresa-parâmetro
    CompanyTaxParameterEntity association = CompanyTaxParameterEntity.builder()
        .companyId(companyId)
        .taxParameterId(taxParameterId)
        .createdAt(LocalDateTime.now())
        .createdBy(1L)
        .build();
    companyTaxParameterRepository.save(association);
  }

  @AfterEach
  void tearDown() {
    valorParametroTemporalRepository.deleteAll();
    companyTaxParameterRepository.deleteAll();
    taxParameterRepository.deleteAll();
    companyRepository.deleteAll();
  }

  @Test
  @DisplayName("ADMIN pode criar valor temporal mensal")
  void adminCanCreateMonthlyTemporalValue() throws Exception {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("ano", 2024);
    request.put("mes", 3);

    // Act & Assert
    mockMvc.perform(post("/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values",
            companyId, taxParameterId)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.ano", equalTo(2024)))
        .andExpect(jsonPath("$.mes", equalTo(3)))
        .andExpect(jsonPath("$.trimestre").doesNotExist())
        .andExpect(jsonPath("$.periodo", equalTo("Mar/2024")));
  }

  @Test
  @DisplayName("ADMIN pode criar valor temporal trimestral")
  void adminCanCreateQuarterlyTemporalValue() throws Exception {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("ano", 2024);
    request.put("trimestre", 2);

    // Act & Assert
    mockMvc.perform(post("/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values",
            companyId, taxParameterId)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.ano", equalTo(2024)))
        .andExpect(jsonPath("$.mes").doesNotExist())
        .andExpect(jsonPath("$.trimestre", equalTo(2)))
        .andExpect(jsonPath("$.periodo", equalTo("2º Tri/2024")));
  }

  @Test
  @DisplayName("CONTADOR recebe 403 ao tentar criar valor temporal")
  void contadorReceives403WhenCreatingTemporalValue() throws Exception {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("ano", 2024);
    request.put("mes", 1);

    // Act & Assert
    mockMvc.perform(post("/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values",
            companyId, taxParameterId)
            .header("Authorization", "Bearer " + contadorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Deve falhar ao criar valor temporal com ambos mes e trimestre preenchidos")
  void shouldFailWhenBothMonthAndQuarterAreFilled() throws Exception {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("ano", 2024);
    request.put("mes", 3);
    request.put("trimestre", 1);

    // Act & Assert
    mockMvc.perform(post("/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values",
            companyId, taxParameterId)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve falhar ao criar valor temporal com ambos mes e trimestre nulos")
  void shouldFailWhenBothMonthAndQuarterAreNull() throws Exception {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("ano", 2024);

    // Act & Assert
    mockMvc.perform(post("/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values",
            companyId, taxParameterId)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve falhar ao criar valor temporal com mes fora do range 1-12")
  void shouldFailWhenMonthIsOutOfRange() throws Exception {
    // Arrange - mes < 1
    Map<String, Object> request1 = new HashMap<>();
    request1.put("ano", 2024);
    request1.put("mes", 0);

    mockMvc.perform(post("/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values",
            companyId, taxParameterId)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isBadRequest());

    // Arrange - mes > 12
    Map<String, Object> request2 = new HashMap<>();
    request2.put("ano", 2024);
    request2.put("mes", 13);

    mockMvc.perform(post("/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values",
            companyId, taxParameterId)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve falhar ao criar valor temporal com trimestre fora do range 1-4")
  void shouldFailWhenQuarterIsOutOfRange() throws Exception {
    // Arrange - trimestre < 1
    Map<String, Object> request1 = new HashMap<>();
    request1.put("ano", 2024);
    request1.put("trimestre", 0);

    mockMvc.perform(post("/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values",
            companyId, taxParameterId)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isBadRequest());

    // Arrange - trimestre > 4
    Map<String, Object> request2 = new HashMap<>();
    request2.put("ano", 2024);
    request2.put("trimestre", 5);

    mockMvc.perform(post("/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values",
            companyId, taxParameterId)
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("ADMIN pode listar valores temporais")
  void adminCanListTemporalValues() throws Exception {
    // Arrange - criar alguns valores temporais
    createTemporalValue(2024, 1, null);
    createTemporalValue(2024, 2, null);
    createTemporalValue(2024, null, 1);

    // Act & Assert
    mockMvc.perform(get("/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values",
            companyId, taxParameterId)
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)));
  }

  @Test
  @DisplayName("ADMIN pode listar valores temporais filtrados por ano")
  void adminCanListTemporalValuesFilteredByYear() throws Exception {
    // Arrange
    createTemporalValue(2024, 1, null);
    createTemporalValue(2024, 2, null);
    createTemporalValue(2025, 1, null);

    // Act & Assert
    mockMvc.perform(get("/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values",
            companyId, taxParameterId)
            .param("ano", "2024")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));
  }

  @Test
  @DisplayName("ADMIN pode deletar valor temporal")
  void adminCanDeleteTemporalValue() throws Exception {
    // Arrange
    Long valorId = createTemporalValue(2024, 1, null);

    // Act & Assert
    mockMvc.perform(
            delete("/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values/{valorId}",
                companyId, taxParameterId, valorId)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    // Verificar que foi deletado
    mockMvc.perform(get("/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values",
            companyId, taxParameterId)
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  @DisplayName("CONTADOR recebe 403 ao tentar deletar valor temporal")
  void contadorReceives403WhenDeletingTemporalValue() throws Exception {
    // Arrange
    Long valorId = createTemporalValue(2024, 1, null);

    // Act & Assert
    mockMvc.perform(
            delete("/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values/{valorId}",
                companyId, taxParameterId, valorId)
                .header("Authorization", "Bearer " + contadorToken))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("ADMIN pode obter timeline agregada")
  void adminCanGetAggregatedTimeline() throws Exception {
    // Arrange
    createTemporalValue(2024, 1, null);
    createTemporalValue(2024, 2, null);
    createTemporalValue(2024, null, 1);

    // Act & Assert
    mockMvc.perform(get("/companies/{companyId}/tax-parameters-timeline", companyId)
            .param("ano", "2024")
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ano", equalTo(2024)))
        .andExpect(jsonPath("$.timeline").isMap());
  }

  @Test
  @DisplayName("CONTADOR recebe 403 ao tentar obter timeline")
  void contadorReceives403WhenGettingTimeline() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/companies/{companyId}/tax-parameters-timeline", companyId)
            .param("ano", "2024")
            .header("Authorization", "Bearer " + contadorToken))
        .andExpect(status().isForbidden());
  }

  /**
   * Helper method para criar valor temporal diretamente via endpoint.
   */
  private Long createTemporalValue(Integer ano, Integer mes, Integer trimestre) throws Exception {
    Map<String, Object> request = new HashMap<>();
    request.put("ano", ano);
    if (mes != null) {
      request.put("mes", mes);
    }
    if (trimestre != null) {
      request.put("trimestre", trimestre);
    }

    String response = mockMvc.perform(
            post("/companies/{companyId}/tax-parameters/{taxParameterId}/temporal-values",
                companyId, taxParameterId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
    return ((Number) responseMap.get("id")).longValue();
  }
}
