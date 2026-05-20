package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.lalurecf.domain.enums.AccountType;
import br.com.lalurecf.domain.enums.ClasseContabil;
import br.com.lalurecf.domain.enums.NaturezaConta;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.PlanoDeContas;
import br.com.lalurecf.domain.model.Company;
import br.com.lalurecf.domain.model.ContaReferencial;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.PlanoDeContasJpaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("PlanoDeContasRepositoryAdapter Integration Tests")
class PlanoDeContasRepositoryAdapterTest {

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

  @Autowired private PlanoDeContasRepositoryAdapter repositoryAdapter;

  @Autowired private PlanoDeContasJpaRepository jpaRepository;

  @Autowired private CompanyRepositoryAdapter companyRepository;

  @Autowired private ContaReferencialRepositoryAdapter contaReferencialRepository;

  private Company testCompany;
  private ContaReferencial testContaReferencial;

  @BeforeEach
  void setUp() {
    jpaRepository.deleteAll();

    // Criar empresa de teste
    Company company = new Company();
    company.setCnpj(br.com.lalurecf.domain.model.valueobject.CNPJ.of("11222333000181"));
    company.setRazaoSocial("Test Company");
    company.setPeriodoContabil(java.time.LocalDate.of(2024, 12, 31));
    company.setStatus(Status.ACTIVE);
    testCompany = companyRepository.save(company);

    // Criar conta referencial de teste
    testContaReferencial =
        contaReferencialRepository.save(
            ContaReferencial.builder()
                .codigoRfb("1.01.01")
                .descricao("Caixa e Equivalentes de Caixa")
                .anoValidade(2024)
                .status(Status.ACTIVE)
                .build());
  }

  @Test
  @DisplayName("Should save plano de contas with FK to ContaReferencial and retrieve by ID")
  void shouldSavePlanoDeContasAndRetrieveById() {
    // Arrange
    PlanoDeContas account =
        createTestPlanoDeContas("1.1.01.001", "Caixa", 2024, AccountType.ATIVO);

    // Act
    PlanoDeContas saved = repositoryAdapter.save(account);
    Optional<PlanoDeContas> found = repositoryAdapter.findById(saved.getId());

    // Assert
    assertNotNull(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("1.1.01.001", found.get().getCode());
    assertEquals("Caixa", found.get().getName());
    assertEquals(2024, found.get().getFiscalYear());
    assertEquals(AccountType.ATIVO, found.get().getAccountType());
    assertEquals(testContaReferencial.getId(), found.get().getContaReferencialId());
    assertEquals(ClasseContabil.ANALITICO, found.get().getClasse());
    assertEquals(4, found.get().getNivel());
    assertEquals(NaturezaConta.DEVEDORA, found.get().getNatureza());
    assertEquals(false, found.get().getAfetaResultado());
    assertEquals(false, found.get().getDedutivel());
  }

  @Test
  @DisplayName("Should enforce unique constraint on company+code+fiscalYear")
  void shouldEnforceUniqueConstraint() {
    // Arrange
    PlanoDeContas account1 =
        createTestPlanoDeContas("1.1.01.001", "Caixa", 2024, AccountType.ATIVO);
    repositoryAdapter.save(account1);

    PlanoDeContas account2 =
        createTestPlanoDeContas("1.1.01.001", "Caixa Duplicada", 2024, AccountType.ATIVO);

    // Act & Assert
    assertThrows(
        DataIntegrityViolationException.class,
        () -> repositoryAdapter.save(account2),
        "Should throw exception for duplicate company+code+fiscalYear");
  }

  @Test
  @DisplayName("Should allow same code for different fiscal years")
  void shouldAllowSameCodeForDifferentYears() {
    // Arrange
    PlanoDeContas account2024 =
        createTestPlanoDeContas("1.1.01.001", "Caixa 2024", 2024, AccountType.ATIVO);
    PlanoDeContas account2025 =
        createTestPlanoDeContas("1.1.01.001", "Caixa 2025", 2025, AccountType.ATIVO);

    // Act
    PlanoDeContas saved2024 = repositoryAdapter.save(account2024);
    PlanoDeContas saved2025 = repositoryAdapter.save(account2025);

    // Assert
    assertNotNull(saved2024.getId());
    assertNotNull(saved2025.getId());
    assertEquals("1.1.01.001", saved2024.getCode());
    assertEquals("1.1.01.001", saved2025.getCode());
    assertEquals(2024, saved2024.getFiscalYear());
    assertEquals(2025, saved2025.getFiscalYear());
  }

  @Test
  @DisplayName("Should find accounts by company and fiscal year")
  void shouldFindByCompanyAndFiscalYear() {
    // Arrange
    repositoryAdapter.save(createTestPlanoDeContas("1.1.01.001", "Caixa", 2024, AccountType.ATIVO));
    repositoryAdapter.save(createTestPlanoDeContas("1.1.02.001", "Bancos", 2024, AccountType.ATIVO));
    repositoryAdapter.save(createTestPlanoDeContas("2.1.01.001", "Fornecedores", 2023, AccountType.PASSIVO));

    // Act
    List<PlanoDeContas> accounts2024 =
        repositoryAdapter.findByCompanyIdAndFiscalYear(testCompany.getId(), 2024);

    // Assert
    assertEquals(2, accounts2024.size());
    assertTrue(accounts2024.stream().allMatch(a -> a.getFiscalYear().equals(2024)));
  }

  @Test
  @DisplayName("Should find account by company, code and fiscal year")
  void shouldFindByCompanyCodeAndFiscalYear() {
    // Arrange
    PlanoDeContas account =
        createTestPlanoDeContas("1.1.01.001", "Caixa", 2024, AccountType.ATIVO);
    repositoryAdapter.save(account);

    // Act
    Optional<PlanoDeContas> found =
        repositoryAdapter.findByCompanyIdAndCodeAndFiscalYear(
            testCompany.getId(), "1.1.01.001", 2024);

    // Assert
    assertTrue(found.isPresent());
    assertEquals("1.1.01.001", found.get().getCode());
    assertEquals("Caixa", found.get().getName());
    assertEquals(2024, found.get().getFiscalYear());
  }

  @Test
  @DisplayName("Should delete account by ID")
  void shouldDeleteById() {
    // Arrange
    PlanoDeContas account =
        createTestPlanoDeContas("1.1.01.001", "Caixa", 2024, AccountType.ATIVO);
    PlanoDeContas saved = repositoryAdapter.save(account);

    // Act
    repositoryAdapter.deleteById(saved.getId());
    Optional<PlanoDeContas> found = repositoryAdapter.findById(saved.getId());

    // Assert
    assertTrue(found.isEmpty());
  }

  @Test
  @DisplayName("Should find accounts by company with pagination")
  void shouldFindByCompanyIdWithPagination() {
    // Arrange
    for (int i = 1; i <= 5; i++) {
      repositoryAdapter.save(
          createTestPlanoDeContas("1.1.0" + i + ".001", "Conta " + i, 2024, AccountType.ATIVO));
    }

    // Act
    Page<PlanoDeContas> page =
        repositoryAdapter.findByCompanyId(testCompany.getId(), PageRequest.of(0, 3));

    // Assert
    assertEquals(5, page.getTotalElements());
    assertEquals(2, page.getTotalPages());
    assertEquals(3, page.getContent().size());
  }

  @Test
  @DisplayName("Should persist and retrieve all ECF-specific fields correctly")
  void shouldPersistEcfFields() {
    // Arrange
    PlanoDeContas account =
        PlanoDeContas.builder()
            .companyId(testCompany.getId())
            .contaReferencialId(testContaReferencial.getId())
            .code("3.1.01.001")
            .name("Receita de Vendas")
            .fiscalYear(2024)
            .accountType(AccountType.RECEITA)
            .classe(ClasseContabil.ANALITICO)
            .nivel(4)
            .natureza(NaturezaConta.CREDORA)
            .afetaResultado(true)
            .dedutivel(false)
            .status(Status.ACTIVE)
            .build();

    // Act
    PlanoDeContas saved = repositoryAdapter.save(account);
    Optional<PlanoDeContas> found = repositoryAdapter.findById(saved.getId());

    // Assert
    assertTrue(found.isPresent());
    PlanoDeContas retrieved = found.get();
    assertEquals(ClasseContabil.ANALITICO, retrieved.getClasse());
    assertEquals(4, retrieved.getNivel());
    assertEquals(NaturezaConta.CREDORA, retrieved.getNatureza());
    assertEquals(true, retrieved.getAfetaResultado());
    assertEquals(false, retrieved.getDedutivel());
  }

  @Test
  @DisplayName("Should update existing account")
  void shouldUpdateExistingAccount() {
    // Arrange
    PlanoDeContas account =
        createTestPlanoDeContas("1.1.01.001", "Caixa Original", 2024, AccountType.ATIVO);
    PlanoDeContas saved = repositoryAdapter.save(account);

    // Act
    saved.setName("Caixa Atualizada");
    saved.setNivel(5);
    PlanoDeContas updated = repositoryAdapter.save(saved);

    // Assert
    Optional<PlanoDeContas> found = repositoryAdapter.findById(updated.getId());
    assertTrue(found.isPresent());
    assertEquals("Caixa Atualizada", found.get().getName());
    assertEquals(5, found.get().getNivel());
  }

  private PlanoDeContas createTestPlanoDeContas(
      String code, String name, Integer fiscalYear, AccountType accountType) {
    return PlanoDeContas.builder()
        .companyId(testCompany.getId())
        .contaReferencialId(testContaReferencial.getId())
        .code(code)
        .name(name)
        .fiscalYear(fiscalYear)
        .accountType(accountType)
        .classe(ClasseContabil.ANALITICO)
        .nivel(4)
        .natureza(NaturezaConta.DEVEDORA)
        .afetaResultado(false)
        .dedutivel(false)
        .status(Status.ACTIVE)
        .build();
  }
}
