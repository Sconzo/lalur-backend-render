package br.com.lalurecf.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.UserRole;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
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
import org.springframework.test.web.servlet.MvcResult;

/**
 * Teste E2E do fluxo completo do CONTADOR.
 *
 * <p>Testa o workflow:
 * <ol>
 *   <li>Login como CONTADOR
 *   <li>Listar empresas disponíveis
 *   <li>Selecionar empresa
 *   <li>Acessar recursos com header X-Company-Id
 *   <li>Validar bloqueios de segurança (403 para CRUD, 400 sem header)
 * </ol>
 */
@DisplayName("Contador Workflow - Teste E2E")
@AutoConfigureMockMvc
class ContadorWorkflowIntegrationTest extends IntegrationTestBase {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private CompanyJpaRepository companyRepository;

  @Autowired
  private ObjectMapper objectMapper;

  private String contadorToken;
  private Long companyId;

  @BeforeEach
  void setUp() {
    // Gerar token CONTADOR
    contadorToken = jwtTokenProvider.generateAccessToken(
        "contador@example.com",
        UserRole.CONTADOR);

    // Criar empresa de teste
    CompanyEntity company = new CompanyEntity();
    company.setCnpj("12345678000195");
    company.setRazaoSocial("Empresa Teste CONTADOR");
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
  @DisplayName("Fluxo completo CONTADOR: listar → selecionar → acessar com header → bloqueios")
  void shouldCompleteContadorWorkflow() throws Exception {
    // ============================================================
    // STEP 1: Listar empresas disponíveis
    // ============================================================
    mockMvc.perform(get("/companies/my-companies")
            .header("Authorization", "Bearer " + contadorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(greaterThan(0))))
        .andExpect(jsonPath("$[0].id", equalTo(companyId.intValue())))
        .andExpect(jsonPath("$[0].razaoSocial", equalTo("Empresa Teste CONTADOR")));

    // ============================================================
    // STEP 2: Selecionar empresa
    // ============================================================
    Map<String, Object> selectRequest = new HashMap<>();
    selectRequest.put("companyId", companyId);

    MvcResult selectResult = mockMvc.perform(post("/companies/select-company")
            .header("Authorization", "Bearer " + contadorToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(selectRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success", equalTo(true)))
        .andExpect(jsonPath("$.companyId", equalTo(companyId.intValue())))
        .andExpect(jsonPath("$.companyName", equalTo("Empresa Teste CONTADOR")))
        .andReturn();

    // ============================================================
    // STEP 3: Acessar empresa atual com header X-Company-Id
    // ============================================================
    mockMvc.perform(get("/companies/current-company")
            .header("Authorization", "Bearer " + contadorToken)
            .header("X-Company-Id", companyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", equalTo(companyId.intValue())))
        .andExpect(jsonPath("$.razaoSocial", equalTo("Empresa Teste CONTADOR")));

    // ============================================================
    // STEP 4: CONTADOR tenta acessar CRUD de empresas → 403 FORBIDDEN
    // ============================================================
    // Tentar listar todas empresas (apenas ADMIN) → 403
    mockMvc.perform(get("/companies")
            .header("Authorization", "Bearer " + contadorToken)
            .param("page", "0")
            .param("size", "20"))
        .andExpect(status().isForbidden());

    // Tentar buscar detalhes de empresa por ID (apenas ADMIN) → 403
    mockMvc.perform(get("/companies/{id}", companyId)
            .header("Authorization", "Bearer " + contadorToken))
        .andExpect(status().isForbidden());

    // ============================================================
    // STEP 5: Tentar acessar recurso sem header X-Company-Id → 400 BAD REQUEST
    // ============================================================
    // Nota: /current-company requer X-Company-Id no contexto
    mockMvc.perform(get("/companies/current-company")
            .header("Authorization", "Bearer " + contadorToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("CONTADOR pode listar empresas disponíveis mas não acessar CRUD")
  void contadorCanListButNotAccessCrud() throws Exception {
    // Listar empresas disponíveis - OK
    mockMvc.perform(get("/companies/my-companies")
            .header("Authorization", "Bearer " + contadorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));

    // Tentar listar TODAS empresas (apenas ADMIN) - FORBIDDEN
    mockMvc.perform(get("/companies")
            .header("Authorization", "Bearer " + contadorToken)
            .param("page", "0")
            .param("size", "20"))
        .andExpect(status().isForbidden());

    // Tentar buscar detalhes (apenas ADMIN) - FORBIDDEN
    mockMvc.perform(get("/companies/{id}", companyId)
            .header("Authorization", "Bearer " + contadorToken))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("CONTADOR precisa de header X-Company-Id para acessar current-company")
  void contadorNeedsCompanyHeaderToAccessCurrentCompany() throws Exception {
    // Sem header → 400 BAD REQUEST
    mockMvc.perform(get("/companies/current-company")
            .header("Authorization", "Bearer " + contadorToken))
        .andExpect(status().isBadRequest());

    // Com header → 200 OK
    mockMvc.perform(get("/companies/current-company")
            .header("Authorization", "Bearer " + contadorToken)
            .header("X-Company-Id", companyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", equalTo(companyId.intValue())));
  }
}
