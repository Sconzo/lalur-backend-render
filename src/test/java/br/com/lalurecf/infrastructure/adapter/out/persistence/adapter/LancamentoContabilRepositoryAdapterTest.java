package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.dao.InvalidDataAccessApiUsageException;

import br.com.lalurecf.domain.enums.AccountType;
import br.com.lalurecf.domain.enums.ClasseContabil;
import br.com.lalurecf.domain.enums.NaturezaConta;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.LancamentoContabil;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.PlanoDeContasEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ContaReferencialEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.PlanoDeContasJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.ContaReferencialJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.LancamentoContabilJpaRepository;
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
 * Testes de integração para LancamentoContabilRepositoryAdapter.
 *
 * <p>Valida todas operações de persistência incluindo:
 *
 * <ul>
 *   <li>Salvar lançamento com partidas dobradas (débito/crédito diferentes)
 *   <li>Validação: contas de débito e crédito devem ser diferentes
 *   <li>Validação: valor deve ser > 0
 *   <li>Soft delete
 *   <li>Listagem paginada por empresa
 *   <li>Busca por empresa e ano fiscal
 * </ul>
 */
@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("LancamentoContabilRepositoryAdapter Integration Tests")
@org.springframework.test.annotation.DirtiesContext(
    classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LancamentoContabilRepositoryAdapterTest {

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

  @Autowired private LancamentoContabilRepositoryAdapter repositoryAdapter;

  @Autowired private LancamentoContabilJpaRepository jpaRepository;

  @Autowired private CompanyJpaRepository companyJpaRepository;

  @Autowired private PlanoDeContasJpaRepository planoDeContasJpaRepository;

  @Autowired private ContaReferencialJpaRepository contaReferencialJpaRepository;

  private Long testCompanyId;
  private Long testContaDebitoId;
  private Long testContaCreditoId;

  @BeforeEach
  void setUp() {
    // Criar empresa de teste
    CompanyEntity company = new CompanyEntity();
    company.setCnpj("12345678000195");
    company.setRazaoSocial("Empresa Teste Lançamento Contábil");
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

    // Criar conta de débito (Caixa - Ativo)
    PlanoDeContasEntity contaDebito = PlanoDeContasEntity.builder()
        .company(company)
        .contaReferencial(contaReferencial)
        .code("1.01.01.001")
        .name("Caixa")
        .fiscalYear(2024)
        .accountType(AccountType.ATIVO)
        .classe(ClasseContabil.ANALITICO)
        .nivel(4)
        .natureza(NaturezaConta.DEVEDORA)
        .afetaResultado(false)
        .dedutivel(false)
        .status(Status.ACTIVE)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    contaDebito = planoDeContasJpaRepository.save(contaDebito);
    testContaDebitoId = contaDebito.getId();

    // Criar conta de crédito (Receitas - Resultado)
    PlanoDeContasEntity contaCredito = PlanoDeContasEntity.builder()
        .company(company)
        .contaReferencial(contaReferencial)
        .code("3.01.01.001")
        .name("Receita de Vendas")
        .fiscalYear(2024)
        .accountType(AccountType.RECEITA)
        .classe(ClasseContabil.ANALITICO)
        .nivel(4)
        .natureza(NaturezaConta.CREDORA)
        .afetaResultado(true)
        .dedutivel(false)
        .status(Status.ACTIVE)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    contaCredito = planoDeContasJpaRepository.save(contaCredito);
    testContaCreditoId = contaCredito.getId();
  }

  @Test
  @DisplayName("Deve salvar lançamento contábil com partidas dobradas válidas")
  void shouldSaveLancamentoContabilWithValidPartidasDobradas() {
    // Arrange
    LancamentoContabil lancamento = LancamentoContabil.builder()
        .companyId(testCompanyId)
        .contaDebitoId(testContaDebitoId)
        .contaCreditoId(testContaCreditoId)
        .data(LocalDate.of(2024, 6, 15))
        .valor(new BigDecimal("1000.00"))
        .historico("Recebimento de vendas em dinheiro")
        .numeroDocumento("NF-001")
        .fiscalYear(2024)
        .status(Status.ACTIVE)
        .build();

    // Act
    LancamentoContabil saved = repositoryAdapter.save(lancamento);

    // Assert
    assertNotNull(saved.getId());
    assertEquals(testCompanyId, saved.getCompanyId());
    assertEquals(testContaDebitoId, saved.getContaDebitoId());
    assertEquals(testContaCreditoId, saved.getContaCreditoId());
    assertEquals(new BigDecimal("1000.00"), saved.getValor());
    assertEquals("Recebimento de vendas em dinheiro", saved.getHistorico());
    assertEquals("NF-001", saved.getNumeroDocumento());
    assertEquals(2024, saved.getFiscalYear());
    assertEquals(Status.ACTIVE, saved.getStatus());
  }

  @Test
  @DisplayName("Deve lançar exception quando conta de débito = conta de crédito")
  void shouldThrowExceptionWhenDebitoEqualsCredito() {
    // Arrange
    LancamentoContabil lancamento = LancamentoContabil.builder()
        .companyId(testCompanyId)
        .contaDebitoId(testContaDebitoId)
        .contaCreditoId(testContaDebitoId) // Mesma conta!
        .data(LocalDate.of(2024, 6, 15))
        .valor(new BigDecimal("1000.00"))
        .historico("Lançamento inválido")
        .fiscalYear(2024)
        .build();

    // Act & Assert
    InvalidDataAccessApiUsageException exception =
        assertThrows(
            InvalidDataAccessApiUsageException.class, () -> repositoryAdapter.save(lancamento));

    assertTrue(
        exception.getCause().getMessage().contains("Conta de débito e conta de crédito devem ser diferentes"));
  }

  @Test
  @DisplayName("Deve lançar exception quando valor <= 0")
  void shouldThrowExceptionWhenValorIsZeroOrNegative() {
    // Arrange - valor zero
    LancamentoContabil lancamentoZero = LancamentoContabil.builder()
        .companyId(testCompanyId)
        .contaDebitoId(testContaDebitoId)
        .contaCreditoId(testContaCreditoId)
        .data(LocalDate.of(2024, 6, 15))
        .valor(BigDecimal.ZERO)
        .historico("Valor zero")
        .fiscalYear(2024)
        .build();

    // Act & Assert - valor zero
    InvalidDataAccessApiUsageException exceptionZero =
        assertThrows(
            InvalidDataAccessApiUsageException.class, () -> repositoryAdapter.save(lancamentoZero));
    assertTrue(
        exceptionZero.getCause().getMessage().contains("Valor do lançamento contábil deve ser maior que zero"));

    // Arrange - valor negativo
    LancamentoContabil lancamentoNegativo = LancamentoContabil.builder()
        .companyId(testCompanyId)
        .contaDebitoId(testContaDebitoId)
        .contaCreditoId(testContaCreditoId)
        .data(LocalDate.of(2024, 6, 15))
        .valor(new BigDecimal("-100.00"))
        .historico("Valor negativo")
        .fiscalYear(2024)
        .build();

    // Act & Assert - valor negativo
    InvalidDataAccessApiUsageException exceptionNegativo =
        assertThrows(
            InvalidDataAccessApiUsageException.class, () -> repositoryAdapter.save(lancamentoNegativo));
    assertTrue(
        exceptionNegativo.getCause().getMessage().contains("Valor do lançamento contábil deve ser maior que zero"));
  }

  @Test
  @DisplayName("Deve buscar lançamento por ID")
  void shouldFindLancamentoById() {
    // Arrange
    LancamentoContabil lancamento = LancamentoContabil.builder()
        .companyId(testCompanyId)
        .contaDebitoId(testContaDebitoId)
        .contaCreditoId(testContaCreditoId)
        .data(LocalDate.of(2024, 6, 15))
        .valor(new BigDecimal("500.00"))
        .historico("Lançamento de teste")
        .fiscalYear(2024)
        .status(Status.ACTIVE)
        .build();

    LancamentoContabil saved = repositoryAdapter.save(lancamento);

    // Act
    Optional<LancamentoContabil> found = repositoryAdapter.findById(saved.getId());

    // Assert
    assertTrue(found.isPresent());
    assertEquals(saved.getId(), found.get().getId());
    assertEquals(new BigDecimal("500.00"), found.get().getValor());
  }

  @Test
  @DisplayName("Deve retornar empty quando buscar ID inexistente")
  void shouldReturnEmptyWhenIdNotFound() {
    // Act
    Optional<LancamentoContabil> found = repositoryAdapter.findById(999999L);

    // Assert
    assertFalse(found.isPresent());
  }

  @Test
  @DisplayName("Deve soft delete lançamento contábil")
  void shouldSoftDeleteLancamento() {
    // Arrange
    LancamentoContabil lancamento = LancamentoContabil.builder()
        .companyId(testCompanyId)
        .contaDebitoId(testContaDebitoId)
        .contaCreditoId(testContaCreditoId)
        .data(LocalDate.of(2024, 6, 15))
        .valor(new BigDecimal("750.00"))
        .historico("Lançamento para deletar")
        .fiscalYear(2024)
        .status(Status.ACTIVE)
        .build();

    LancamentoContabil saved = repositoryAdapter.save(lancamento);

    // Act
    repositoryAdapter.deleteById(saved.getId());

    // Assert
    Optional<LancamentoContabil> deleted = repositoryAdapter.findById(saved.getId());
    assertTrue(deleted.isPresent());
    assertEquals(Status.INACTIVE, deleted.get().getStatus());
  }

  @Test
  @DisplayName("Deve buscar lançamentos por empresa e ano fiscal")
  void shouldFindByCompanyIdAndFiscalYear() {
    // Arrange - criar 2 lançamentos em 2024 e 1 em 2025
    repositoryAdapter.save(createLancamento(testCompanyId, 2024, "100.00"));
    repositoryAdapter.save(createLancamento(testCompanyId, 2024, "200.00"));
    repositoryAdapter.save(createLancamento(testCompanyId, 2025, "300.00"));

    // Act
    List<LancamentoContabil> lancamentos2024 =
        repositoryAdapter.findByCompanyIdAndFiscalYear(testCompanyId, 2024);

    // Assert
    assertEquals(2, lancamentos2024.size());
    assertTrue(lancamentos2024.stream().allMatch(l -> l.getFiscalYear().equals(2024)));
  }

  @Test
  @DisplayName("Deve buscar lançamentos por empresa com paginação")
  void shouldFindByCompanyIdWithPagination() {
    // Arrange - criar 5 lançamentos
    for (int i = 1; i <= 5; i++) {
      repositoryAdapter.save(
          createLancamento(testCompanyId, 2024, String.format("%d00.00", i)));
    }

    // Act
    Page<LancamentoContabil> page1 = repositoryAdapter.findByCompanyId(testCompanyId, PageRequest.of(0, 3));
    Page<LancamentoContabil> page2 = repositoryAdapter.findByCompanyId(testCompanyId, PageRequest.of(1, 3));

    // Assert
    assertEquals(3, page1.getContent().size());
    assertEquals(2, page2.getContent().size());
    assertEquals(5, page1.getTotalElements());
  }

  /**
   * Helper method para criar lançamento de teste.
   */
  private LancamentoContabil createLancamento(Long companyId, Integer fiscalYear, String valor) {
    return LancamentoContabil.builder()
        .companyId(companyId)
        .contaDebitoId(testContaDebitoId)
        .contaCreditoId(testContaCreditoId)
        .data(LocalDate.of(fiscalYear, 6, 15))
        .valor(new BigDecimal(valor))
        .historico("Lançamento teste " + valor)
        .fiscalYear(fiscalYear)
        .status(Status.ACTIVE)
        .build();
  }
}
