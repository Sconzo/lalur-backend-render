package br.com.lalurecf.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lalurecf.domain.enums.ParameterNature;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.UserRole;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterTypeEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.TaxParameterJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.TaxParameterTypeJpaRepository;
import br.com.lalurecf.infrastructure.security.JwtTokenProvider;
import br.com.lalurecf.util.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * Testes End-to-End de integração.
 *
 * <p>Cobre os principais fluxos de integração:
 * <ul>
 *   <li>Fluxo ADMIN com parâmetros tributários
 *   <li>Associação empresa + parâmetros
 *   <li>Row-level security via X-Company-Id
 * </ul>
 */
@DisplayName("Parâmetros Tributários e Empresas - Testes E2E")
@AutoConfigureMockMvc
class TaxParameterCompanyE2ETest extends IntegrationTestBase {

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
  private ObjectMapper objectMapper;

  private String adminToken;
  private String contadorToken;

  @BeforeEach
  void setUp() {
    adminToken = jwtTokenProvider.generateAccessToken("admin@example.com", UserRole.ADMIN);
    contadorToken = jwtTokenProvider.generateAccessToken("contador@example.com",
        UserRole.CONTADOR);
  }

  @AfterEach
  void tearDown() {
    taxParameterRepository.deleteAll();
    companyRepository.deleteAll();
  }

  @Test
  @DisplayName("E2E: ADMIN cria parâmetros e filtra por tipo")
  void adminCanCreateAndFilterTaxParametersByType() throws Exception {
    // Criar parâmetros usando helper
    TaxParameterEntity regime = createTaxParameter("REGIME-001", "REGIME", "Lucro Real");
    TaxParameterEntity cnae = createTaxParameter("CNAE-001", "CNAE", "Comércio Varejista");
    TaxParameterEntity geral = createTaxParameter("GERAL-001", "GERAL", "Parâmetro Geral");

    // Listar todos parâmetros REGIME
    mockMvc.perform(get("/tax-parameters")
            .header("Authorization", "Bearer " + adminToken)
            .param("tipo", "REGIME"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.codigo == 'REGIME-001')]").exists());

    // Listar todos parâmetros CNAE
    mockMvc.perform(get("/tax-parameters")
            .header("Authorization", "Bearer " + adminToken)
            .param("tipo", "CNAE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.codigo == 'CNAE-001')]").exists());

    // Listar todos parâmetros GERAL
    mockMvc.perform(get("/tax-parameters")
            .header("Authorization", "Bearer " + adminToken)
            .param("tipo", "GERAL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.codigo == 'GERAL-001')]").exists());
  }

  @Test
  @DisplayName("E2E: ADMIN associa parâmetros a empresa e atualiza lista")
  void adminCanManageCompanyTaxParameters() throws Exception {
    // Criar empresa e parâmetros usando helpers
    CompanyEntity company = createCompany("12345678000195", "Empresa E2E Test");
    TaxParameterEntity param1 = createTaxParameter("PARAM-001", "REGIME", "Lucro Real");
    TaxParameterEntity param2 = createTaxParameter("PARAM-002", "REGIME", "Lucro Presumido");

    // ============================================================
    // STEP 1: Associar parâmetros à empresa
    // ============================================================
    Map<String, Object> updateParams = new HashMap<>();
    List<Long> paramIds = new ArrayList<>();
    paramIds.add(param1.getId());
    paramIds.add(param2.getId());
    updateParams.put("taxParameterIds", paramIds);

    mockMvc.perform(put("/companies/{id}/tax-parameters", company.getId())
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateParams)))
        .andExpect(status().isOk());

    // ============================================================
    // STEP 2: Listar parâmetros associados
    // ============================================================
    mockMvc.perform(get("/companies/{id}/tax-parameters", company.getId())
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));

    // ============================================================
    // STEP 3: Substituir lista (manter apenas param1)
    // ============================================================
    List<Long> newParamIds = new ArrayList<>();
    newParamIds.add(param1.getId());
    updateParams.put("taxParameterIds", newParamIds);

    mockMvc.perform(put("/companies/{id}/tax-parameters", company.getId())
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateParams)))
        .andExpect(status().isOk());

    // Validar que lista foi substituída
    mockMvc.perform(get("/companies/{id}/tax-parameters", company.getId())
            .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].codigo", equalTo("PARAM-001")));
  }

  @Test
  @DisplayName("E2E: Row-level security - CONTADOR acessa recursos com X-Company-Id")
  void rowLevelSecurityViaCompanyHeader() throws Exception {
    // ============================================================
    // STEP 1: ADMIN cria duas empresas
    // ============================================================
    CompanyEntity company1 = createCompany("11111111000191", "Empresa 1");
    CompanyEntity company2 = createCompany("22222222000192", "Empresa 2");

    // ============================================================
    // STEP 2: CONTADOR lista empresas - deve ver ambas
    // ============================================================
    mockMvc.perform(get("/companies/my-companies")
            .header("Authorization", "Bearer " + contadorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));

    // ============================================================
    // STEP 3: CONTADOR seleciona empresa1 e usa header X-Company-Id
    // ============================================================
    Map<String, Object> selectRequest = new HashMap<>();
    selectRequest.put("companyId", company1.getId());

    mockMvc.perform(post("/companies/select-company")
            .header("Authorization", "Bearer " + contadorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(selectRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.companyId", equalTo(company1.getId().intValue())));

    // Acessar current-company com header - sucesso
    mockMvc.perform(get("/companies/current-company")
            .header("Authorization", "Bearer " + contadorToken)
            .header("X-Company-Id", company1.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", equalTo(company1.getId().intValue())))
        .andExpect(jsonPath("$.razaoSocial", equalTo("Empresa 1")));

    // ============================================================
    // STEP 4: Tentar acessar sem header - erro 400
    // ============================================================
    mockMvc.perform(get("/companies/current-company")
            .header("Authorization", "Bearer " + contadorToken))
        .andExpect(status().isBadRequest());
  }

  /**
   * Helper: criar parâmetro tributário.
   */
  private TaxParameterEntity createTaxParameter(String codigo, String tipo, String descricao) {
    // Create or find type
    TaxParameterTypeEntity paramType =
        taxParameterTypeRepository
            .findByDescricao(tipo)
            .orElseGet(
                () ->
                    taxParameterTypeRepository.save(
                        TaxParameterTypeEntity.builder()
                            .descricao(tipo)
                            .natureza(ParameterNature.GLOBAL)
                            .status(Status.ACTIVE)
                            .build()));

    TaxParameterEntity param =
        TaxParameterEntity.builder()
            .codigo(codigo)
            .tipoParametro(paramType)
            .descricao(descricao)
            .status(Status.ACTIVE)
            .build();
    return taxParameterRepository.save(param);
  }

  /**
   * Helper: criar empresa.
   */
  private CompanyEntity createCompany(String cnpj, String razaoSocial) {
    CompanyEntity company = new CompanyEntity();
    company.setCnpj(cnpj);
    company.setRazaoSocial(razaoSocial);
    company.setStatus(Status.ACTIVE);
    company.setPeriodoContabil(LocalDate.of(2024, 1, 31));
    company.setCreatedAt(LocalDateTime.now());
    company.setUpdatedAt(LocalDateTime.now());
    return companyRepository.save(company);
  }
}
