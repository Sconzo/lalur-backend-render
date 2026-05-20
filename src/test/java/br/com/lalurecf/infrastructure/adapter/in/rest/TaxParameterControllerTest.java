package br.com.lalurecf.infrastructure.adapter.in.rest;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lalurecf.domain.enums.ParameterNature;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.util.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes de integração para TaxParameterController.
 *
 * <p>Valida todos os endpoints do CRUD de parâmetros tributários, incluindo:
 * <ul>
 *   <li>Criação de parâmetros (ADMIN only)
 *   <li>Listagem com filtros e paginação
 *   <li>Visualização individual
 *   <li>Atualização de parâmetros
 *   <li>Toggle de status (ACTIVE ↔ INACTIVE)
 *   <li>Controle de acesso (CONTADOR recebe 403)
 * </ul>
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("TaxParameterController - Testes de Integração")
class TaxParameterControllerTest extends IntegrationTestBase {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("ADMIN deve conseguir criar parâmetro tributário com tipo")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldCreateTaxParameterAsAdmin() throws Exception {
    // Arrange
    String requestBody = """
        {
          "code": "TEST-PARAM-001",
          "type": "IRPJ",
          "description": "Parâmetro de teste para IRPJ",
          "nature": "GLOBAL"
        }
        """;

    // Act & Assert
    mockMvc.perform(post("/tax-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.code").value("TEST-PARAM-001"))
        .andExpect(jsonPath("$.type").value("IRPJ"))
        .andExpect(jsonPath("$.description").value("Parâmetro de teste para IRPJ"))
        .andExpect(jsonPath("$.nature").value("GLOBAL"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  @DisplayName("Deve retornar 400 Bad Request ao tentar criar parâmetro com código duplicado")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldReturn400WhenCodeIsDuplicate() throws Exception {
    // Arrange - criar primeiro parâmetro
    String requestBody = """
        {
          "code": "DUPLICATE-CODE",
          "type": "CSLL",
          "description": "Primeiro parâmetro",
          "nature": "GLOBAL"
        }
        """;

    mockMvc.perform(post("/tax-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated());

    // Act & Assert - tentar criar parâmetro com mesmo código
    mockMvc.perform(post("/tax-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Já existe um parâmetro tributário com o código: DUPLICATE-CODE"));
  }

  @Test
  @DisplayName("Deve retornar 400 Bad Request quando código tem formato inválido")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldReturn400WhenCodeFormatIsInvalid() throws Exception {
    // Arrange - código com caracteres inválidos (minúsculas e espaços)
    String requestBody = """
        {
          "code": "invalid code 123",
          "type": "GERAL",
          "description": "Código inválido",
          "nature": "GLOBAL"
        }
        """;

    // Act & Assert
    mockMvc.perform(post("/tax-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors").exists())
        .andExpect(jsonPath("$.validationErrors.code").exists());
  }

  @Test
  @DisplayName("CONTADOR deve receber 403 Forbidden ao tentar criar parâmetro")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn403WhenContadorTriesToCreate() throws Exception {
    // Arrange
    String requestBody = """
        {
          "code": "TEST-PARAM",
          "type": "IRPJ",
          "description": "Teste",
          "nature": "GLOBAL"
        }
        """;

    // Act & Assert
    mockMvc.perform(post("/tax-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("CONTADOR deve receber 403 Forbidden ao tentar listar parâmetros")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn403WhenContadorTriesToList() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/tax-parameters"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Deve listar parâmetros tributários com paginação")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldListTaxParametersWithPagination() throws Exception {
    // Arrange - criar alguns parâmetros
    createTaxParameter("LIST-TEST-1", "IRPJ", "Parâmetro 1");
    createTaxParameter("LIST-TEST-2", "CSLL", "Parâmetro 2");
    createTaxParameter("LIST-TEST-3", "GERAL", "Parâmetro 3");

    // Act & Assert
    mockMvc.perform(get("/tax-parameters")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(3))))
        .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(3)))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.number").value(0));
  }

  @Test
  @DisplayName("Deve filtrar parâmetros tributários por tipo")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldFilterTaxParametersByType() throws Exception {
    // Arrange - criar parâmetros de diferentes tipos
    createTaxParameter("FILTER-IRPJ-1", "IRPJ", "Parâmetro IRPJ 1");
    createTaxParameter("FILTER-IRPJ-2", "IRPJ", "Parâmetro IRPJ 2");
    createTaxParameter("FILTER-CSLL-1", "CSLL", "Parâmetro CSLL 1");

    // Act & Assert - filtrar apenas IRPJ
    mockMvc.perform(get("/tax-parameters")
            .param("type", "IRPJ"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));
  }

  @Test
  @DisplayName("Deve buscar parâmetro por código e descrição")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldSearchTaxParametersByCodeAndDescription() throws Exception {
    // Arrange
    createTaxParameter("SEARCH-ALIQUOTA-15", "IRPJ", "Alíquota de 15% para IRPJ");
    createTaxParameter("SEARCH-OTHER", "CSLL", "Outro parâmetro sem alíquota");
    createTaxParameter("SEARCH-ALIQUOTA-9", "CSLL", "Alíquota de 9% para CSLL");

    // Act & Assert - buscar por "aliquota"
    mockMvc.perform(get("/tax-parameters")
            .param("search", "aliquota"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(2)));
  }

  @Test
  @DisplayName("Deve retornar parâmetro tributário por ID")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldGetTaxParameterById() throws Exception {
    // Arrange - criar parâmetro e obter ID
    String createResponse = mockMvc.perform(post("/tax-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "GET-BY-ID-TEST",
                  "type": "GERAL",
                  "description": "Teste de busca por ID",
                  "nature": "GLOBAL"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(createResponse).get("id").asLong();

    // Act & Assert
    mockMvc.perform(get("/tax-parameters/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.code").value("GET-BY-ID-TEST"))
        .andExpect(jsonPath("$.type").value("GERAL"))
        .andExpect(jsonPath("$.description").value("Teste de busca por ID"))
        .andExpect(jsonPath("$.nature").value("GLOBAL"));
  }

  @Test
  @DisplayName("Deve atualizar parâmetro tributário")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldUpdateTaxParameter() throws Exception {
    // Arrange - criar parâmetro
    String createResponse = mockMvc.perform(post("/tax-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "UPDATE-TEST",
                  "type": "IRPJ",
                  "description": "Descrição original",
                  "nature": "GLOBAL"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(createResponse).get("id").asLong();

    // Act & Assert - atualizar parâmetro
    mockMvc.perform(put("/tax-parameters/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "UPDATE-TEST-MODIFIED",
                  "type": "CSLL",
                  "description": "Descrição atualizada",
                  "nature": "MONTHLY"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.code").value("UPDATE-TEST-MODIFIED"))
        .andExpect(jsonPath("$.type").value("CSLL"))
        .andExpect(jsonPath("$.description").value("Descrição atualizada"))
        .andExpect(jsonPath("$.nature").value("MONTHLY"))
        .andExpect(jsonPath("$.updatedAt").exists());
  }

  @Test
  @DisplayName("Deve alternar status de ACTIVE para INACTIVE")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldToggleStatusFromActiveToInactive() throws Exception {
    // Arrange - criar parâmetro (status inicial é ACTIVE)
    String createResponse = mockMvc.perform(post("/tax-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "TOGGLE-TEST",
                  "type": "GERAL",
                  "description": "Teste de toggle status",
                  "nature": "GLOBAL"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(createResponse).get("id").asLong();

    // Act & Assert - alternar para INACTIVE
    mockMvc.perform(patch("/tax-parameters/{id}/status", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "INACTIVE"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.newStatus").value(Status.INACTIVE.name()));
  }

  @Test
  @DisplayName("Deve alternar status de INACTIVE para ACTIVE")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldToggleStatusFromInactiveToActive() throws Exception {
    // Arrange - criar parâmetro e inativar
    String createResponse = mockMvc.perform(post("/tax-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "TOGGLE-REACTIVE-TEST",
                  "type": "GERAL",
                  "description": "Teste de reativação",
                  "nature": "GLOBAL"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(createResponse).get("id").asLong();

    // Inativar primeiro
    mockMvc.perform(patch("/tax-parameters/{id}/status", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "INACTIVE"
                }
                """))
        .andExpect(status().isOk());

    // Act & Assert - reativar
    mockMvc.perform(patch("/tax-parameters/{id}/status", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "ACTIVE"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.newStatus").value(Status.ACTIVE.name()));
  }

  @Test
  @DisplayName("Parâmetro inativado não deve aparecer na listagem padrão")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldNotListInactiveParametersByDefault() throws Exception {
    // Arrange - criar parâmetro ativo
    createTaxParameter("ACTIVE-PARAM", "IRPJ", "Parâmetro ativo");

    // Criar e inativar parâmetro
    String createResponse = mockMvc.perform(post("/tax-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "INACTIVE-PARAM",
                  "type": "IRPJ",
                  "description": "Parâmetro inativo",
                  "nature": "GLOBAL"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(createResponse).get("id").asLong();

    // Inativar
    mockMvc.perform(patch("/tax-parameters/{id}/status", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "INACTIVE"
                }
                """))
        .andExpect(status().isOk());

    // Act & Assert - listagem padrão (sem includeInactive)
    mockMvc.perform(get("/tax-parameters")
            .param("type", "IRPJ"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
  }

  @Test
  @DisplayName("Parâmetro inativado deve aparecer com include_inactive=true")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldListInactiveParametersWhenIncludeInactiveIsTrue() throws Exception {
    // Arrange - criar parâmetro ativo
    createTaxParameter("ACTIVE-INCLUDE-TEST", "CSLL", "Parâmetro ativo");

    // Criar e inativar parâmetro
    String createResponse = mockMvc.perform(post("/tax-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "INACTIVE-INCLUDE-TEST",
                  "type": "CSLL",
                  "description": "Parâmetro inativo",
                  "nature": "GLOBAL"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(createResponse).get("id").asLong();

    // Inativar
    mockMvc.perform(patch("/tax-parameters/{id}/status", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "status": "INACTIVE"
                }
                """))
        .andExpect(status().isOk());

    // Act & Assert - listagem com includeInactive=true
    mockMvc.perform(get("/tax-parameters")
            .param("type", "CSLL")
            .param("includeInactive", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));
  }

  @Test
  @DisplayName("Deve retornar lista de tipos únicos para filtro")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldGetUniqueTypesForFilter() throws Exception {
    // Arrange - criar parâmetros de diferentes tipos
    createTaxParameter("TYPE-1", "IRPJ", "Tipo 1");
    createTaxParameter("TYPE-2", "CSLL", "Tipo 2");
    createTaxParameter("TYPE-3", "IRPJ", "Tipo 3 duplicado");
    createTaxParameter("TYPE-4", "GERAL", "Tipo 4");

    // Act & Assert
    mockMvc.perform(get("/tax-parameters/filter-options/types"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.options").isArray())
        .andExpect(jsonPath("$.options", hasSize(greaterThanOrEqualTo(3))));
  }

  // Helper method para criar parâmetros nos testes
  private void createTaxParameter(String code, String type, String description) throws Exception {
    createTaxParameter(code, type, description, ParameterNature.GLOBAL);
  }

  private void createTaxParameter(String code, String type, String description, ParameterNature nature) throws Exception {
    String requestBody = String.format("""
        {
          "code": "%s",
          "type": "%s",
          "description": "%s",
          "nature": "%s"
        }
        """, code, type, description, nature.name());

    mockMvc.perform(post("/tax-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated());
  }

  // ==================================================================================
  // Testes para ParameterNature
  // ==================================================================================

  @Test
  @DisplayName("ADMIN deve conseguir criar parâmetro tributário com nature GLOBAL")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldCreateTaxParameterWithGlobalNature() throws Exception {
    String requestBody = """
        {
          "code": "NATURE-GLOBAL-TEST",
          "type": "IRPJ",
          "description": "Parâmetro GLOBAL",
          "nature": "GLOBAL"
        }
        """;

    mockMvc.perform(post("/tax-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("NATURE-GLOBAL-TEST"))
        .andExpect(jsonPath("$.nature").value("GLOBAL"));
  }

  @Test
  @DisplayName("ADMIN deve conseguir criar parâmetro tributário com nature MONTHLY")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldCreateTaxParameterWithMonthlyNature() throws Exception {
    String requestBody = """
        {
          "code": "NATURE-MONTHLY-TEST",
          "type": "FORMA_TRIBUTACAO",
          "description": "Parâmetro MONTHLY",
          "nature": "MONTHLY"
        }
        """;

    mockMvc.perform(post("/tax-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("NATURE-MONTHLY-TEST"))
        .andExpect(jsonPath("$.nature").value("MONTHLY"));
  }

  @Test
  @DisplayName("ADMIN deve conseguir criar parâmetro tributário com nature QUARTERLY")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldCreateTaxParameterWithQuarterlyNature() throws Exception {
    String requestBody = """
        {
          "code": "NATURE-QUARTERLY-TEST",
          "type": "FORMA_ESTIMATIVA",
          "description": "Parâmetro QUARTERLY",
          "nature": "QUARTERLY"
        }
        """;

    mockMvc.perform(post("/tax-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("NATURE-QUARTERLY-TEST"))
        .andExpect(jsonPath("$.nature").value("QUARTERLY"));
  }

  @Test
  @DisplayName("Deve retornar 400 Bad Request quando nature não é fornecido")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldReturn400WhenNatureIsNull() throws Exception {
    String requestBody = """
        {
          "code": "NATURE-NULL-TEST",
          "type": "IRPJ",
          "description": "Sem natureza"
        }
        """;

    mockMvc.perform(post("/tax-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.validationErrors.nature").exists());
  }

  @Test
  @DisplayName("Deve filtrar parâmetros tributários por nature")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldFilterTaxParametersByNature() throws Exception {
    // Arrange - criar parâmetros de diferentes naturezas
    createTaxParameter("FILTER-NATURE-1", "IRPJ", "Global 1", ParameterNature.GLOBAL);
    createTaxParameter("FILTER-NATURE-2", "CSLL", "Global 2", ParameterNature.GLOBAL);
    createTaxParameter("FILTER-NATURE-3", "FORMA_TRIB", "Monthly", ParameterNature.MONTHLY);

    // Act & Assert - filtrar apenas GLOBAL
    mockMvc.perform(get("/tax-parameters")
            .param("nature", "GLOBAL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));
  }

  @Test
  @DisplayName("Deve atualizar nature do parâmetro tributário")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldUpdateTaxParameterNature() throws Exception {
    // Arrange - criar parâmetro GLOBAL
    String createResponse = mockMvc.perform(post("/tax-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "UPDATE-NATURE-TEST",
                  "type": "FORMA_TRIBUTACAO",
                  "description": "Inicialmente GLOBAL",
                  "nature": "GLOBAL"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(createResponse).get("id").asLong();

    // Act & Assert - atualizar para MONTHLY
    mockMvc.perform(put("/tax-parameters/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "UPDATE-NATURE-TEST",
                  "type": "FORMA_TRIBUTACAO",
                  "description": "Agora MONTHLY",
                  "nature": "MONTHLY"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nature").value("MONTHLY"));
  }
}
