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
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.PlanoDeContasEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ContaParteBEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ContaReferencialEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterTypeEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.PlanoDeContasJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.ContaParteBJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.ContaReferencialJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.TaxParameterJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.TaxParameterTypeJpaRepository;
import br.com.lalurecf.util.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
 * Testes de integração para LancamentoParteBController.
 *
 * <p>Valida todos os endpoints do CRUD de lançamentos da Parte B (e-Lalur/e-Lacs), incluindo:
 *
 * <ul>
 *   <li>Criação com todos tipos de relacionamento (CONTA_CONTABIL, CONTA_PARTE_B, AMBOS)
 *   <li>Validações condicionais de FK baseadas em tipoRelacionamento
 *   <li>Validação de parâmetro tributário (existe e está ACTIVE)
 *   <li>Validação de ownership de contas (empresa no contexto)
 *   <li>Listagem com filtros (anoReferencia, mesReferencia, tipoApuracao, tipoAjuste, status)
 *   <li>Visualização individual (CONTADOR com header)
 *   <li>Atualização com revalidações
 *   <li>Toggle de status
 *   <li>Validações de mês (1-12), ano (2000-2027), valor > 0
 *   <li>Controle de contexto (X-Company-Id obrigatório)
 * </ul>
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("LancamentoParteBController - Testes de Integração")
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
@org.springframework.test.annotation.DirtiesContext(
    classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LancamentoParteBControllerTest extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CompanyJpaRepository companyRepository;

  @Autowired private TaxParameterJpaRepository taxParameterRepository;
  @Autowired private TaxParameterTypeJpaRepository taxParameterTypeRepository;

  @Autowired private PlanoDeContasJpaRepository planoDeContasRepository;

  @Autowired private ContaParteBJpaRepository contaParteBRepository;

  @Autowired private ContaReferencialJpaRepository contaReferencialRepository;

  @Autowired
  private br.com.lalurecf.infrastructure.adapter.out.persistence.repository
          .LancamentoParteBJpaRepository
      lancamentoParteBJpaRepository;

  private Long companyId;
  private Long taxParameterId;
  private Long contaContabilId;
  private Long contaParteBId;

  @BeforeEach
  void setUp() {
    // Criar empresa de teste
    CompanyEntity company = new CompanyEntity();
    company.setCnpj("12345678000195");
    company.setRazaoSocial("Empresa Teste Lançamento Parte B");
    company.setStatus(Status.ACTIVE);
    company.setPeriodoContabil(LocalDate.of(2024, 1, 31));
    company.setCreatedAt(LocalDateTime.now());
    company.setUpdatedAt(LocalDateTime.now());
    company = companyRepository.save(company);
    companyId = company.getId();

    // Criar tipo de parâmetro tributário
    TaxParameterTypeEntity taxParameterType =
        TaxParameterTypeEntity.builder()
            .descricao("DEDUTIBILIDADE")
            .natureza(ParameterNature.GLOBAL)
            .status(Status.ACTIVE)
            .build();
    taxParameterType = taxParameterTypeRepository.save(taxParameterType);

    // Criar parâmetro tributário ACTIVE
    TaxParameterEntity taxParameter =
        TaxParameterEntity.builder()
            .codigo("ART170-DEDUTIVEL")
            .tipoParametro(taxParameterType)
            .descricao("Despesas operacionais dedutíveis conforme Art. 170 do RIR/2018")
            .status(Status.ACTIVE)
            .build();
    taxParameter = taxParameterRepository.save(taxParameter);
    taxParameterId = taxParameter.getId();

    // Criar conta referencial para PlanoDeContas
    ContaReferencialEntity contaReferencial = new ContaReferencialEntity();
    contaReferencial.setCodigoRfb("3.01");
    contaReferencial.setDescricao("Receita Bruta");
    contaReferencial.setAnoValidade(2024);
    contaReferencial.setCreatedAt(LocalDateTime.now());
    contaReferencial.setUpdatedAt(LocalDateTime.now());
    contaReferencial = contaReferencialRepository.save(contaReferencial);

    // Criar conta contábil da empresa
    PlanoDeContasEntity planoDeContas = new PlanoDeContasEntity();
    planoDeContas.setCode("1.01.01.001");
    planoDeContas.setName("Despesas Administrativas");
    planoDeContas.setCompany(company);
    planoDeContas.setContaReferencial(contaReferencial);
    planoDeContas.setFiscalYear(2024);
    planoDeContas.setAccountType(br.com.lalurecf.domain.enums.AccountType.DESPESA);
    planoDeContas.setClasse(br.com.lalurecf.domain.enums.ClasseContabil.ANALITICO);
    planoDeContas.setNivel(4);
    planoDeContas.setNatureza(br.com.lalurecf.domain.enums.NaturezaConta.DEVEDORA);
    planoDeContas.setAfetaResultado(true);
    planoDeContas.setDedutivel(true);
    planoDeContas.setCreatedAt(LocalDateTime.now());
    planoDeContas.setUpdatedAt(LocalDateTime.now());
    planoDeContas = planoDeContasRepository.save(planoDeContas);
    contaContabilId = planoDeContas.getId();

    // Criar conta Parte B da empresa
    ContaParteBEntity contaParteB = new ContaParteBEntity();
    contaParteB.setCodigoConta("4.01.01.001");
    contaParteB.setDescricao("Adições IRPJ - Despesas não dedutíveis");
    contaParteB.setAnoBase(2024);
    contaParteB.setCompany(company);
    contaParteB.setDataVigenciaInicio(LocalDate.of(2024, 1, 1));
    contaParteB.setTipoTributo(br.com.lalurecf.domain.enums.TipoTributo.IRPJ);
    contaParteB.setSaldoInicial(BigDecimal.ZERO);
    contaParteB.setTipoSaldo(br.com.lalurecf.domain.enums.TipoSaldo.DEVEDOR);
    contaParteB.setStatus(Status.ACTIVE);
    contaParteB.setCreatedAt(LocalDateTime.now());
    contaParteB.setUpdatedAt(LocalDateTime.now());
    contaParteB = contaParteBRepository.save(contaParteB);
    contaParteBId = contaParteB.getId();
  }


  @Test
  @DisplayName("CONTADOR deve criar lançamento com tipoRelacionamento=CONTA_CONTABIL")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldCreateLancamentoWithContaContabil() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "mesReferencia": 1,
          "anoReferencia": 2024,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "CONTA_CONTABIL",
          "contaContabilId": %d,
          "contaParteBId": null,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Adição de despesas não dedutíveis",
          "valor": 5000.00
        }
        """
            .formatted(contaContabilId, taxParameterId);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/lancamento-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.mesReferencia").value(1))
        .andExpect(jsonPath("$.anoReferencia").value(2024))
        .andExpect(jsonPath("$.tipoApuracao").value("IRPJ"))
        .andExpect(jsonPath("$.tipoRelacionamento").value("CONTA_CONTABIL"))
        .andExpect(jsonPath("$.contaContabilId").value(contaContabilId))
        .andExpect(jsonPath("$.contaParteBId").isEmpty())
        .andExpect(jsonPath("$.parametroTributarioId").value(taxParameterId))
        .andExpect(jsonPath("$.tipoAjuste").value("ADICAO"))
        .andExpect(jsonPath("$.descricao").value("Adição de despesas não dedutíveis"))
        .andExpect(jsonPath("$.valor").value(5000.0))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  @DisplayName("CONTADOR deve criar lançamento com tipoRelacionamento=CONTA_PARTE_B")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldCreateLancamentoWithContaParteB() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "mesReferencia": 2,
          "anoReferencia": 2024,
          "tipoApuracao": "CSLL",
          "tipoRelacionamento": "CONTA_PARTE_B",
          "contaContabilId": null,
          "contaParteBId": %d,
          "parametroTributarioId": %d,
          "tipoAjuste": "EXCLUSAO",
          "descricao": "Exclusão de receitas não tributáveis",
          "valor": 10000.00
        }
        """
            .formatted(contaParteBId, taxParameterId);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/lancamento-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.tipoRelacionamento").value("CONTA_PARTE_B"))
        .andExpect(jsonPath("$.contaContabilId").isEmpty())
        .andExpect(jsonPath("$.contaParteBId").value(contaParteBId))
        .andExpect(jsonPath("$.tipoAjuste").value("EXCLUSAO"));
  }

  @Test
  @DisplayName("CONTADOR deve criar lançamento com tipoRelacionamento=AMBOS")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldCreateLancamentoWithAmbos() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "mesReferencia": 3,
          "anoReferencia": 2024,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "AMBOS",
          "contaContabilId": %d,
          "contaParteBId": %d,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Ajuste relacionado a ambas contas",
          "valor": 7500.50
        }
        """
            .formatted(contaContabilId, contaParteBId, taxParameterId);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/lancamento-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.tipoRelacionamento").value("AMBOS"))
        .andExpect(jsonPath("$.contaContabilId").value(contaContabilId))
        .andExpect(jsonPath("$.contaParteBId").value(contaParteBId));
  }

  @Test
  @DisplayName("Deve retornar 400 quando contaContabilId ausente com tipoRelacionamento=CONTA_CONTABIL")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn400WhenContaContabilIdMissingForContaContabil() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "mesReferencia": 4,
          "anoReferencia": 2024,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "CONTA_CONTABIL",
          "contaContabilId": null,
          "contaParteBId": null,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Teste sem contaContabilId",
          "valor": 1000.00
        }
        """
            .formatted(taxParameterId);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/lancamento-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("contaContabilId é obrigatória quando tipoRelacionamento = CONTA_CONTABIL"));
  }

  @Test
  @DisplayName("Deve retornar 400 quando contaParteBId presente com tipoRelacionamento=CONTA_CONTABIL")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn400WhenContaParteBIdPresentForContaContabil() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "mesReferencia": 5,
          "anoReferencia": 2024,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "CONTA_CONTABIL",
          "contaContabilId": %d,
          "contaParteBId": %d,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Teste com contaParteBId incorreta",
          "valor": 1000.00
        }
        """
            .formatted(contaContabilId, contaParteBId, taxParameterId);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/lancamento-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("contaParteBId deve ser nula quando tipoRelacionamento = CONTA_CONTABIL"));
  }

  @Test
  @DisplayName("Deve retornar 400 quando contaParteBId ausente com tipoRelacionamento=CONTA_PARTE_B")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn400WhenContaParteBIdMissingForContaParteB() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "mesReferencia": 6,
          "anoReferencia": 2024,
          "tipoApuracao": "CSLL",
          "tipoRelacionamento": "CONTA_PARTE_B",
          "contaContabilId": null,
          "contaParteBId": null,
          "parametroTributarioId": %d,
          "tipoAjuste": "EXCLUSAO",
          "descricao": "Teste sem contaParteBId",
          "valor": 2000.00
        }
        """
            .formatted(taxParameterId);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/lancamento-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("contaParteBId é obrigatória quando tipoRelacionamento = CONTA_PARTE_B"));
  }

  @Test
  @DisplayName("Deve retornar 400 quando ambas FKs ausentes com tipoRelacionamento=AMBOS")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn400WhenBothFksMissingForAmbos() throws Exception {
    // Arrange - missing contaContabilId
    String requestBody =
        """
        {
          "mesReferencia": 7,
          "anoReferencia": 2024,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "AMBOS",
          "contaContabilId": null,
          "contaParteBId": %d,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Teste sem contaContabilId",
          "valor": 3000.00
        }
        """
            .formatted(contaParteBId, taxParameterId);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/lancamento-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("contaContabilId é obrigatória quando tipoRelacionamento = AMBOS"));
  }

  @Test
  @DisplayName("Deve retornar 400 quando parâmetro tributário inexistente")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn400WhenParametroTributarioNotFound() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "mesReferencia": 8,
          "anoReferencia": 2024,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "CONTA_CONTABIL",
          "contaContabilId": %d,
          "contaParteBId": null,
          "parametroTributarioId": 99999,
          "tipoAjuste": "ADICAO",
          "descricao": "Teste com parametro inexistente",
          "valor": 1000.00
        }
        """
            .formatted(contaContabilId);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/lancamento-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Parâmetro tributário não encontrado com id: 99999"));
  }

  @Test
  @DisplayName("Deve retornar 400 quando parâmetro tributário INACTIVE")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn400WhenParametroTributarioInactive() throws Exception {
    // Arrange - criar tipo e parâmetro INACTIVE
    TaxParameterTypeEntity inactiveType =
        TaxParameterTypeEntity.builder()
            .descricao("TESTE")
            .natureza(ParameterNature.GLOBAL)
            .status(Status.ACTIVE)
            .build();
    inactiveType = taxParameterTypeRepository.save(inactiveType);

    TaxParameterEntity inactiveParam =
        TaxParameterEntity.builder()
            .codigo("PARAM-INATIVO")
            .tipoParametro(inactiveType)
            .descricao("Parâmetro inativo para teste - Art. X")
            .status(Status.INACTIVE)
            .build();
    inactiveParam = taxParameterRepository.save(inactiveParam);

    String requestBody =
        """
        {
          "mesReferencia": 9,
          "anoReferencia": 2024,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "CONTA_CONTABIL",
          "contaContabilId": %d,
          "contaParteBId": null,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Teste com parametro INACTIVE",
          "valor": 1000.00
        }
        """
            .formatted(contaContabilId, inactiveParam.getId());

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/lancamento-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Parâmetro tributário deve estar ACTIVE. Status atual: INACTIVE"));
  }

  @Test
  @DisplayName("Deve retornar 400 quando conta contábil de outra empresa")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn400WhenContaContabilFromDifferentCompany() throws Exception {
    // Arrange - criar outra empresa e sua conta
    CompanyEntity otherCompany = new CompanyEntity();
    otherCompany.setCnpj("11222333000181");
    otherCompany.setRazaoSocial("Outra Empresa");
    otherCompany.setStatus(Status.ACTIVE);
    otherCompany.setPeriodoContabil(LocalDate.of(2024, 1, 31));
    otherCompany.setCreatedAt(LocalDateTime.now());
    otherCompany.setUpdatedAt(LocalDateTime.now());
    otherCompany = companyRepository.save(otherCompany);

    ContaReferencialEntity contaRef = contaReferencialRepository.findAll().get(0);

    PlanoDeContasEntity otherConta = new PlanoDeContasEntity();
    otherConta.setCode("2.01.01.001");
    otherConta.setName("Conta de outra empresa");
    otherConta.setCompany(otherCompany);
    otherConta.setContaReferencial(contaRef);
    otherConta.setFiscalYear(2024);
    otherConta.setAccountType(br.com.lalurecf.domain.enums.AccountType.PASSIVO);
    otherConta.setClasse(br.com.lalurecf.domain.enums.ClasseContabil.SINTETICO);
    otherConta.setNivel(4);
    otherConta.setNatureza(br.com.lalurecf.domain.enums.NaturezaConta.CREDORA);
    otherConta.setAfetaResultado(false);
    otherConta.setDedutivel(false);
    otherConta.setCreatedAt(LocalDateTime.now());
    otherConta.setUpdatedAt(LocalDateTime.now());
    otherConta = planoDeContasRepository.save(otherConta);

    String requestBody =
        """
        {
          "mesReferencia": 10,
          "anoReferencia": 2024,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "CONTA_CONTABIL",
          "contaContabilId": %d,
          "contaParteBId": null,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Teste com conta de outra empresa",
          "valor": 1000.00
        }
        """
            .formatted(otherConta.getId(), taxParameterId);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/lancamento-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Conta contábil não pertence à empresa no contexto (X-Company-Id)"));
  }

  @Test
  @DisplayName("Deve retornar 400 quando mesReferencia < 1")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn400WhenMesReferenciaLessThan1() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "mesReferencia": 0,
          "anoReferencia": 2024,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "CONTA_CONTABIL",
          "contaContabilId": %d,
          "contaParteBId": null,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Teste com mês inválido",
          "valor": 1000.00
        }
        """
            .formatted(contaContabilId, taxParameterId);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/lancamento-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve retornar 400 quando mesReferencia > 12")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn400WhenMesReferenciaGreaterThan12() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "mesReferencia": 13,
          "anoReferencia": 2024,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "CONTA_CONTABIL",
          "contaContabilId": %d,
          "contaParteBId": null,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Teste com mês inválido",
          "valor": 1000.00
        }
        """
            .formatted(contaContabilId, taxParameterId);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/lancamento-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve retornar 400 quando anoReferencia < 2000")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn400WhenAnoReferenciaLessThan2000() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "mesReferencia": 1,
          "anoReferencia": 1999,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "CONTA_CONTABIL",
          "contaContabilId": %d,
          "contaParteBId": null,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Teste com ano inválido",
          "valor": 1000.00
        }
        """
            .formatted(contaContabilId, taxParameterId);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/lancamento-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve retornar 400 quando valor <= 0")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn400WhenValorZeroOrNegative() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "mesReferencia": 1,
          "anoReferencia": 2024,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "CONTA_CONTABIL",
          "contaContabilId": %d,
          "contaParteBId": null,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Teste com valor zero",
          "valor": 0.00
        }
        """
            .formatted(contaContabilId, taxParameterId);

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/lancamento-parte-b")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Deve retornar 400 quando header X-Company-Id ausente")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn400WhenMissingCompanyIdHeader() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "mesReferencia": 1,
          "anoReferencia": 2024,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "CONTA_CONTABIL",
          "contaContabilId": %d,
          "contaParteBId": null,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Teste sem header",
          "valor": 1000.00
        }
        """
            .formatted(contaContabilId, taxParameterId);

    // Act & Assert - sem header X-Company-Id
    mockMvc
        .perform(
            post("/api/v1/lancamento-parte-b")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Company context is required (header X-Company-Id missing)"));
  }

  @Test
  @DisplayName("Listagem com filtros deve funcionar")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldFilterListingCorrectly() throws Exception {
    // Arrange - criar múltiplos lançamentos
    String lancamento1 =
        """
        {
          "mesReferencia": 1,
          "anoReferencia": 2024,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "CONTA_CONTABIL",
          "contaContabilId": %d,
          "contaParteBId": null,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Lançamento IRPJ Janeiro 2024",
          "valor": 1000.00
        }
        """
            .formatted(contaContabilId, taxParameterId);

    String lancamento2 =
        """
        {
          "mesReferencia": 2,
          "anoReferencia": 2024,
          "tipoApuracao": "CSLL",
          "tipoRelacionamento": "CONTA_PARTE_B",
          "contaContabilId": null,
          "contaParteBId": %d,
          "parametroTributarioId": %d,
          "tipoAjuste": "EXCLUSAO",
          "descricao": "Lançamento CSLL Fevereiro 2024",
          "valor": 2000.00
        }
        """
            .formatted(contaParteBId, taxParameterId);

    String lancamento3 =
        """
        {
          "mesReferencia": 1,
          "anoReferencia": 2025,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "CONTA_CONTABIL",
          "contaContabilId": %d,
          "contaParteBId": null,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Lançamento IRPJ Janeiro 2025",
          "valor": 3000.00
        }
        """
            .formatted(contaContabilId, taxParameterId);

    mockMvc.perform(
        post("/api/v1/lancamento-parte-b")
            .header("X-Company-Id", companyId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(lancamento1));

    mockMvc.perform(
        post("/api/v1/lancamento-parte-b")
            .header("X-Company-Id", companyId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(lancamento2));

    mockMvc.perform(
        post("/api/v1/lancamento-parte-b")
            .header("X-Company-Id", companyId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(lancamento3));

    // Act & Assert - filtrar por ano
    mockMvc
        .perform(
            get("/api/v1/lancamento-parte-b?ano_referencia=2024").header("X-Company-Id", companyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));

    // Act & Assert - filtrar por tipoApuracao
    mockMvc
        .perform(
            get("/api/v1/lancamento-parte-b?tipo_apuracao=IRPJ").header("X-Company-Id", companyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));

    // Act & Assert - filtrar por tipoAjuste
    mockMvc
        .perform(
            get("/api/v1/lancamento-parte-b?tipo_ajuste=EXCLUSAO")
                .header("X-Company-Id", companyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));

    // Act & Assert - filtrar por mês
    mockMvc
        .perform(
            get("/api/v1/lancamento-parte-b?mes_referencia=1").header("X-Company-Id", companyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));
  }

  @Test
  @DisplayName("CONTADOR deve conseguir visualizar lançamento Parte B")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldAllowContadorToViewLancamento() throws Exception {
    // Arrange - criar lançamento
    String createRequest =
        """
        {
          "mesReferencia": 6,
          "anoReferencia": 2024,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "CONTA_CONTABIL",
          "contaContabilId": %d,
          "contaParteBId": null,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Lançamento para visualização",
          "valor": 5000.00
        }
        """
            .formatted(contaContabilId, taxParameterId);

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/lancamento-parte-b")
                    .header("X-Company-Id", companyId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequest))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = createResult.getResponse().getContentAsString();
    Long lancamentoId = objectMapper.readTree(responseBody).get("id").asLong();

    // Act & Assert - visualizar lançamento
    mockMvc
        .perform(
            get("/api/v1/lancamento-parte-b/" + lancamentoId).header("X-Company-Id", companyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(lancamentoId))
        .andExpect(jsonPath("$.descricao").value("Lançamento para visualização"));
  }

  @Test
  @DisplayName("Edição deve permitir mudança de campos com revalidação")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldAllowEditingWithRevalidation() throws Exception {
    // Arrange - criar lançamento
    String createRequest =
        """
        {
          "mesReferencia": 7,
          "anoReferencia": 2024,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "CONTA_CONTABIL",
          "contaContabilId": %d,
          "contaParteBId": null,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Descrição Original",
          "valor": 1000.00
        }
        """
            .formatted(contaContabilId, taxParameterId);

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/lancamento-parte-b")
                    .header("X-Company-Id", companyId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequest))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = createResult.getResponse().getContentAsString();
    Long lancamentoId = objectMapper.readTree(responseBody).get("id").asLong();

    // Act - editar lançamento
    String updateRequest =
        """
        {
          "mesReferencia": 8,
          "anoReferencia": 2024,
          "tipoApuracao": "CSLL",
          "tipoRelacionamento": "CONTA_PARTE_B",
          "contaContabilId": null,
          "contaParteBId": %d,
          "parametroTributarioId": %d,
          "tipoAjuste": "EXCLUSAO",
          "descricao": "Descrição Atualizada",
          "valor": 2000.00
        }
        """
            .formatted(contaParteBId, taxParameterId);

    mockMvc
        .perform(
            put("/api/v1/lancamento-parte-b/" + lancamentoId)
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(lancamentoId))
        .andExpect(jsonPath("$.mesReferencia").value(8))
        .andExpect(jsonPath("$.tipoApuracao").value("CSLL"))
        .andExpect(jsonPath("$.tipoRelacionamento").value("CONTA_PARTE_B"))
        .andExpect(jsonPath("$.tipoAjuste").value("EXCLUSAO"))
        .andExpect(jsonPath("$.descricao").value("Descrição Atualizada"))
        .andExpect(jsonPath("$.valor").value(2000.0));
  }

  @Test
  @DisplayName("Toggle status deve funcionar")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldToggleStatusSuccessfully() throws Exception {
    // Arrange - criar lançamento
    String createRequest =
        """
        {
          "mesReferencia": 9,
          "anoReferencia": 2024,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "CONTA_CONTABIL",
          "contaContabilId": %d,
          "contaParteBId": null,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Lançamento para teste de status",
          "valor": 1000.00
        }
        """
            .formatted(contaContabilId, taxParameterId);

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/lancamento-parte-b")
                    .header("X-Company-Id", companyId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequest))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = createResult.getResponse().getContentAsString();
    Long lancamentoId = objectMapper.readTree(responseBody).get("id").asLong();

    // Act - alternar para INACTIVE
    String toggleRequest =
        """
        {
          "status": "INACTIVE"
        }
        """;

    mockMvc
        .perform(
            patch("/api/v1/lancamento-parte-b/" + lancamentoId + "/status")
                .header("X-Company-Id", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toggleRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.newStatus").value("INACTIVE"));

    // Assert - verificar que status foi alterado
    mockMvc
        .perform(
            get("/api/v1/lancamento-parte-b/" + lancamentoId).header("X-Company-Id", companyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"));
  }

  @Test
  @DisplayName("Deve retornar apenas lançamentos ACTIVE por padrão")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturnOnlyActiveByDefault() throws Exception {
    // Arrange - criar lançamento e inativar
    String createRequest =
        """
        {
          "mesReferencia": 10,
          "anoReferencia": 2024,
          "tipoApuracao": "IRPJ",
          "tipoRelacionamento": "CONTA_CONTABIL",
          "contaContabilId": %d,
          "contaParteBId": null,
          "parametroTributarioId": %d,
          "tipoAjuste": "ADICAO",
          "descricao": "Lançamento para teste de filtro de status",
          "valor": 1000.00
        }
        """
            .formatted(contaContabilId, taxParameterId);

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/lancamento-parte-b")
                    .header("X-Company-Id", companyId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequest))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = createResult.getResponse().getContentAsString();
    Long lancamentoId = objectMapper.readTree(responseBody).get("id").asLong();

    // Inativar lançamento
    String toggleRequest =
        """
        {
          "status": "INACTIVE"
        }
        """;

    mockMvc.perform(
        patch("/api/v1/lancamento-parte-b/" + lancamentoId + "/status")
            .header("X-Company-Id", companyId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(toggleRequest));

    // Act & Assert - listar sem include_inactive (não deve aparecer)
    mockMvc
        .perform(
            get("/api/v1/lancamento-parte-b?mes_referencia=10&ano_referencia=2024")
                .header("X-Company-Id", companyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());

    // Act & Assert - listar com include_inactive=true (deve aparecer)
    mockMvc
        .perform(
            get("/api/v1/lancamento-parte-b?mes_referencia=10&ano_referencia=2024&include_inactive=true")
                .header("X-Company-Id", companyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(lancamentoId))
        .andExpect(jsonPath("$.content[0].status").value("INACTIVE"));
  }
}
