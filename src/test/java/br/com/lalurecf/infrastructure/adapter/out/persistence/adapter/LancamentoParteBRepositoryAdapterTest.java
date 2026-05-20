package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.lalurecf.domain.enums.ParameterNature;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.TipoAjuste;
import br.com.lalurecf.domain.enums.TipoApuracao;
import br.com.lalurecf.domain.enums.TipoRelacionamento;
import br.com.lalurecf.domain.enums.TipoSaldo;
import br.com.lalurecf.domain.enums.TipoTributo;
import br.com.lalurecf.domain.model.LancamentoParteB;
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
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.LancamentoParteBJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.TaxParameterJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.TaxParameterTypeJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testes de integração para LancamentoParteBRepositoryAdapter.
 *
 * <p>Valida todas operações de persistência e validações condicionais de FK baseadas em
 * tipoRelacionamento.
 */
@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("LancamentoParteBRepositoryAdapter Integration Tests")
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
class LancamentoParteBRepositoryAdapterTest {

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

  @Autowired private LancamentoParteBRepositoryAdapter repositoryAdapter;

  @Autowired private LancamentoParteBJpaRepository jpaRepository;

  @Autowired private CompanyJpaRepository companyJpaRepository;

  @Autowired private PlanoDeContasJpaRepository planoDeContasJpaRepository;

  @Autowired private ContaParteBJpaRepository contaParteBJpaRepository;

  @Autowired private TaxParameterJpaRepository taxParameterJpaRepository;

  @Autowired private TaxParameterTypeJpaRepository taxParameterTypeJpaRepository;

  @Autowired private ContaReferencialJpaRepository contaReferencialJpaRepository;

  private Long testCompanyId;
  private Long testContaContabilId;
  private Long testContaParteBId;
  private Long testParametroTributarioId;

  @BeforeEach
  void setUp() {
    jpaRepository.deleteAll();
    planoDeContasJpaRepository.deleteAll();
    contaParteBJpaRepository.deleteAll();
    taxParameterJpaRepository.deleteAll();
    contaReferencialJpaRepository.deleteAll();
    companyJpaRepository.deleteAll();

    // Criar empresa de teste
    CompanyEntity testCompany = new CompanyEntity();
    testCompany.setCnpj("12345678000199");
    testCompany.setRazaoSocial("Empresa Teste Ltda");
    testCompany.setPeriodoContabil(LocalDate.of(2024, 12, 31));
    testCompany.setStatus(Status.ACTIVE);
    testCompany.setCreatedAt(LocalDateTime.now());
    testCompany.setUpdatedAt(LocalDateTime.now());
    CompanyEntity savedCompany = companyJpaRepository.save(testCompany);
    testCompanyId = savedCompany.getId();

    // Criar conta referencial RFB de teste
    ContaReferencialEntity contaReferencial = ContaReferencialEntity.builder()
        .codigoRfb("3.01")
        .descricao("Receita Bruta")
        .anoValidade(2024)
        .status(Status.ACTIVE)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    ContaReferencialEntity savedContaReferencial =
        contaReferencialJpaRepository.save(contaReferencial);

    // Criar conta contábil de teste
    PlanoDeContasEntity contaContabil = PlanoDeContasEntity.builder()
        .company(savedCompany)
        .contaReferencial(savedContaReferencial)
        .code("3.01.01")
        .name("Receita de Vendas")
        .fiscalYear(2024)
        .accountType(br.com.lalurecf.domain.enums.AccountType.RECEITA)
        .classe(br.com.lalurecf.domain.enums.ClasseContabil.ANALITICO)
        .nivel(3)
        .natureza(br.com.lalurecf.domain.enums.NaturezaConta.CREDORA)
        .status(Status.ACTIVE)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    PlanoDeContasEntity savedContaContabil = planoDeContasJpaRepository.save(contaContabil);
    testContaContabilId = savedContaContabil.getId();

    // Criar conta Parte B de teste
    ContaParteBEntity contaParteB = new ContaParteBEntity();
    contaParteB.setCompany(savedCompany);
    contaParteB.setCodigoConta("4.01.01");
    contaParteB.setDescricao("Adições - Despesas não dedutíveis");
    contaParteB.setAnoBase(2024);
    contaParteB.setDataVigenciaInicio(LocalDate.of(2024, 1, 1));
    contaParteB.setTipoTributo(TipoTributo.IRPJ);
    contaParteB.setSaldoInicial(BigDecimal.ZERO);
    contaParteB.setTipoSaldo(TipoSaldo.DEVEDOR);
    contaParteB.setStatus(Status.ACTIVE);
    contaParteB.setCreatedAt(LocalDateTime.now());
    contaParteB.setUpdatedAt(LocalDateTime.now());
    ContaParteBEntity savedContaParteB = contaParteBJpaRepository.save(contaParteB);
    testContaParteBId = savedContaParteB.getId();

    // Criar tipo de parâmetro tributário
    TaxParameterTypeEntity taxParameterType =
        TaxParameterTypeEntity.builder()
            .descricao("CNAE")
            .natureza(ParameterNature.GLOBAL)
            .status(Status.ACTIVE)
            .build();
    taxParameterType = taxParameterTypeJpaRepository.save(taxParameterType);

    // Criar parâmetro tributário de teste
    TaxParameterEntity taxParameter =
        TaxParameterEntity.builder()
            .codigo("CNAE-6201-5")
            .tipoParametro(taxParameterType)
            .descricao("Desenvolvimento de programas de computador sob encomenda")
            .status(Status.ACTIVE)
            .build();
    TaxParameterEntity savedTaxParameter = taxParameterJpaRepository.save(taxParameter);
    testParametroTributarioId = savedTaxParameter.getId();
  }

  @Test
  @DisplayName("Should save lancamento with tipoRelacionamento = CONTA_CONTABIL")
  void shouldSaveLancamentoWithTipoRelacionamentoContaContabil() {
    // Arrange
    LancamentoParteB lancamento =
        createTestLancamento(
            TipoRelacionamento.CONTA_CONTABIL, testContaContabilId, null, TipoApuracao.IRPJ);

    // Act
    LancamentoParteB saved = repositoryAdapter.save(lancamento);

    // Assert
    assertNotNull(saved.getId());
    assertEquals(testContaContabilId, saved.getContaContabilId());
    assertEquals(null, saved.getContaParteBId());
    assertEquals(TipoRelacionamento.CONTA_CONTABIL, saved.getTipoRelacionamento());
  }

  @Test
  @DisplayName("Should save lancamento with tipoRelacionamento = CONTA_PARTE_B")
  void shouldSaveLancamentoWithTipoRelacionamentoContaParteB() {
    // Arrange
    LancamentoParteB lancamento =
        createTestLancamento(
            TipoRelacionamento.CONTA_PARTE_B, null, testContaParteBId, TipoApuracao.CSLL);

    // Act
    LancamentoParteB saved = repositoryAdapter.save(lancamento);

    // Assert
    assertNotNull(saved.getId());
    assertEquals(null, saved.getContaContabilId());
    assertEquals(testContaParteBId, saved.getContaParteBId());
    assertEquals(TipoRelacionamento.CONTA_PARTE_B, saved.getTipoRelacionamento());
  }

  @Test
  @DisplayName("Should save lancamento with tipoRelacionamento = AMBOS")
  void shouldSaveLancamentoWithTipoRelacionamentoAmbos() {
    // Arrange
    LancamentoParteB lancamento =
        createTestLancamento(
            TipoRelacionamento.AMBOS,
            testContaContabilId,
            testContaParteBId,
            TipoApuracao.IRPJ);

    // Act
    LancamentoParteB saved = repositoryAdapter.save(lancamento);

    // Assert
    assertNotNull(saved.getId());
    assertEquals(testContaContabilId, saved.getContaContabilId());
    assertEquals(testContaParteBId, saved.getContaParteBId());
    assertEquals(TipoRelacionamento.AMBOS, saved.getTipoRelacionamento());
  }

  @Test
  @DisplayName(
      "Should throw exception when tipoRelacionamento = CONTA_CONTABIL but contaContabil is null")
  void shouldThrowExceptionWhenContaContabilRequiredButNull() {
    // Arrange
    LancamentoParteB lancamento =
        createTestLancamento(TipoRelacionamento.CONTA_CONTABIL, null, null, TipoApuracao.IRPJ);

    // Act & Assert
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> repositoryAdapter.save(lancamento));
    assertTrue(exception.getMessage().contains("contaContabil é obrigatória"));
  }

  @Test
  @DisplayName(
      "Should throw exception when tipoRelacionamento = CONTA_CONTABIL but contaParteB is not"
          + " null")
  void shouldThrowExceptionWhenContaParteBShouldBeNullForContaContabil() {
    // Arrange
    LancamentoParteB lancamento =
        createTestLancamento(
            TipoRelacionamento.CONTA_CONTABIL,
            testContaContabilId,
            testContaParteBId,
            TipoApuracao.IRPJ);

    // Act & Assert
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> repositoryAdapter.save(lancamento));
    assertTrue(exception.getMessage().contains("contaParteB deve ser nula"));
  }

  @Test
  @DisplayName(
      "Should throw exception when tipoRelacionamento = CONTA_PARTE_B but contaParteB is null")
  void shouldThrowExceptionWhenContaParteBRequiredButNull() {
    // Arrange
    LancamentoParteB lancamento =
        createTestLancamento(TipoRelacionamento.CONTA_PARTE_B, null, null, TipoApuracao.CSLL);

    // Act & Assert
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> repositoryAdapter.save(lancamento));
    assertTrue(exception.getMessage().contains("contaParteB é obrigatória"));
  }

  @Test
  @DisplayName(
      "Should throw exception when tipoRelacionamento = CONTA_PARTE_B but contaContabil is not"
          + " null")
  void shouldThrowExceptionWhenContaContabilShouldBeNullForContaParteB() {
    // Arrange
    LancamentoParteB lancamento =
        createTestLancamento(
            TipoRelacionamento.CONTA_PARTE_B,
            testContaContabilId,
            testContaParteBId,
            TipoApuracao.CSLL);

    // Act & Assert
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> repositoryAdapter.save(lancamento));
    assertTrue(exception.getMessage().contains("contaContabil deve ser nula"));
  }

  @Test
  @DisplayName("Should throw exception when tipoRelacionamento = AMBOS but contaContabil is null")
  void shouldThrowExceptionWhenAmbosMissingContaContabil() {
    // Arrange
    LancamentoParteB lancamento =
        createTestLancamento(
            TipoRelacionamento.AMBOS, null, testContaParteBId, TipoApuracao.IRPJ);

    // Act & Assert
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> repositoryAdapter.save(lancamento));
    assertTrue(exception.getMessage().contains("contaContabil é obrigatória"));
  }

  @Test
  @DisplayName("Should throw exception when tipoRelacionamento = AMBOS but contaParteB is null")
  void shouldThrowExceptionWhenAmbosMissingContaParteB() {
    // Arrange
    LancamentoParteB lancamento =
        createTestLancamento(
            TipoRelacionamento.AMBOS, testContaContabilId, null, TipoApuracao.IRPJ);

    // Act & Assert
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> repositoryAdapter.save(lancamento));
    assertTrue(exception.getMessage().contains("contaParteB é obrigatória"));
  }

  @Test
  @DisplayName("Should throw exception when valor is zero")
  void shouldThrowExceptionWhenValorIsZero() {
    // Arrange
    LancamentoParteB lancamento =
        createTestLancamento(
            TipoRelacionamento.CONTA_CONTABIL, testContaContabilId, null, TipoApuracao.IRPJ);
    lancamento.setValor(BigDecimal.ZERO);

    // Act & Assert
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> repositoryAdapter.save(lancamento));
    assertTrue(exception.getMessage().contains("valor deve ser maior que zero"));
  }

  @Test
  @DisplayName("Should throw exception when valor is negative")
  void shouldThrowExceptionWhenValorIsNegative() {
    // Arrange
    LancamentoParteB lancamento =
        createTestLancamento(
            TipoRelacionamento.CONTA_CONTABIL, testContaContabilId, null, TipoApuracao.IRPJ);
    lancamento.setValor(new BigDecimal("-100.00"));

    // Act & Assert
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> repositoryAdapter.save(lancamento));
    assertTrue(exception.getMessage().contains("valor deve ser maior que zero"));
  }

  @Test
  @DisplayName("Should find lancamento by ID")
  void shouldFindLancamentoById() {
    // Arrange
    LancamentoParteB lancamento =
        createTestLancamento(
            TipoRelacionamento.CONTA_CONTABIL, testContaContabilId, null, TipoApuracao.IRPJ);
    LancamentoParteB saved = repositoryAdapter.save(lancamento);

    // Act
    Optional<LancamentoParteB> found = repositoryAdapter.findById(saved.getId());

    // Assert
    assertTrue(found.isPresent());
    assertEquals(saved.getId(), found.get().getId());
    assertEquals(saved.getDescricao(), found.get().getDescricao());
  }

  @Test
  @DisplayName("Should find lancamentos by company and ano referencia")
  void shouldFindLancamentosByCompanyAndAnoReferencia() {
    // Arrange
    LancamentoParteB lancamento1 =
        createTestLancamento(
            TipoRelacionamento.CONTA_CONTABIL, testContaContabilId, null, TipoApuracao.IRPJ);
    lancamento1.setAnoReferencia(2024);
    repositoryAdapter.save(lancamento1);

    LancamentoParteB lancamento2 =
        createTestLancamento(
            TipoRelacionamento.CONTA_PARTE_B, null, testContaParteBId, TipoApuracao.CSLL);
    lancamento2.setAnoReferencia(2024);
    repositoryAdapter.save(lancamento2);

    LancamentoParteB lancamento3 =
        createTestLancamento(
            TipoRelacionamento.CONTA_CONTABIL, testContaContabilId, null, TipoApuracao.IRPJ);
    lancamento3.setAnoReferencia(2025);
    repositoryAdapter.save(lancamento3);

    // Act
    List<LancamentoParteB> found =
        repositoryAdapter.findByCompanyIdAndAnoReferencia(testCompanyId, 2024);

    // Assert
    assertEquals(2, found.size());
  }

  @Test
  @DisplayName("Should find lancamentos by company, ano and mes referencia")
  void shouldFindLancamentosByCompanyAndAnoAndMesReferencia() {
    // Arrange
    LancamentoParteB lancamento1 =
        createTestLancamento(
            TipoRelacionamento.CONTA_CONTABIL, testContaContabilId, null, TipoApuracao.IRPJ);
    lancamento1.setAnoReferencia(2024);
    lancamento1.setMesReferencia(3);
    repositoryAdapter.save(lancamento1);

    LancamentoParteB lancamento2 =
        createTestLancamento(
            TipoRelacionamento.CONTA_PARTE_B, null, testContaParteBId, TipoApuracao.CSLL);
    lancamento2.setAnoReferencia(2024);
    lancamento2.setMesReferencia(3);
    repositoryAdapter.save(lancamento2);

    LancamentoParteB lancamento3 =
        createTestLancamento(
            TipoRelacionamento.CONTA_CONTABIL, testContaContabilId, null, TipoApuracao.IRPJ);
    lancamento3.setAnoReferencia(2024);
    lancamento3.setMesReferencia(4);
    repositoryAdapter.save(lancamento3);

    // Act
    List<LancamentoParteB> found =
        repositoryAdapter.findByCompanyIdAndAnoReferenciaAndMesReferencia(testCompanyId, 2024, 3);

    // Assert
    assertEquals(2, found.size());
  }

  @Test
  @DisplayName("Should find lancamentos by company with pagination")
  void shouldFindLancamentosByCompanyWithPagination() {
    // Arrange
    for (int i = 0; i < 5; i++) {
      LancamentoParteB lancamento =
          createTestLancamento(
              TipoRelacionamento.CONTA_CONTABIL, testContaContabilId, null, TipoApuracao.IRPJ);
      lancamento.setDescricao("Lançamento " + i);
      repositoryAdapter.save(lancamento);
    }

    // Act
    Page<LancamentoParteB> page =
        repositoryAdapter.findByCompanyId(testCompanyId, PageRequest.of(0, 3));

    // Assert
    assertEquals(3, page.getContent().size());
    assertEquals(5, page.getTotalElements());
  }

  @Test
  @DisplayName("Should delete lancamento by ID")
  void shouldDeleteLancamentoById() {
    // Arrange
    LancamentoParteB lancamento =
        createTestLancamento(
            TipoRelacionamento.CONTA_CONTABIL, testContaContabilId, null, TipoApuracao.IRPJ);
    LancamentoParteB saved = repositoryAdapter.save(lancamento);

    // Act
    repositoryAdapter.deleteById(saved.getId());

    // Assert
    Optional<LancamentoParteB> found = repositoryAdapter.findById(saved.getId());
    assertTrue(found.isEmpty());
  }

  /**
   * Helper method para criar lançamento de teste.
   */
  private LancamentoParteB createTestLancamento(
      TipoRelacionamento tipoRelacionamento,
      Long contaContabilId,
      Long contaParteBId,
      TipoApuracao tipoApuracao) {
    return LancamentoParteB.builder()
        .companyId(testCompanyId)
        .mesReferencia(3)
        .anoReferencia(2024)
        .tipoApuracao(tipoApuracao)
        .tipoRelacionamento(tipoRelacionamento)
        .contaContabilId(contaContabilId)
        .contaParteBId(contaParteBId)
        .parametroTributarioId(testParametroTributarioId)
        .tipoAjuste(TipoAjuste.ADICAO)
        .descricao("Ajuste fiscal de teste")
        .valor(new BigDecimal("1000.00"))
        .status(Status.ACTIVE)
        .build();
  }
}
