package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.TipoSaldo;
import br.com.lalurecf.domain.enums.TipoTributo;
import br.com.lalurecf.domain.model.ContaParteB;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.ContaParteBJpaRepository;
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
@DisplayName("ContaParteBRepositoryAdapter Integration Tests")
class ContaParteBRepositoryAdapterTest {

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

  @Autowired private ContaParteBRepositoryAdapter repositoryAdapter;

  @Autowired private ContaParteBJpaRepository jpaRepository;

  @Autowired private CompanyJpaRepository companyJpaRepository;

  private Long testCompanyId;

  @BeforeEach
  void setUp() {
    jpaRepository.deleteAll();
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
  }

  @Test
  @DisplayName("Should save conta Parte B and retrieve by ID")
  void shouldSaveContaParteBAndRetrieveById() {
    // Arrange
    ContaParteB conta =
        createTestContaParteB("4.01.01", "Prejuízo Fiscal IRPJ", 2024, TipoTributo.IRPJ);

    // Act
    ContaParteB saved = repositoryAdapter.save(conta);
    Optional<ContaParteB> found = repositoryAdapter.findById(saved.getId());

    // Assert
    assertNotNull(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("4.01.01", found.get().getCodigoConta());
    assertEquals("Prejuízo Fiscal IRPJ", found.get().getDescricao());
    assertEquals(2024, found.get().getAnoBase());
    assertEquals(TipoTributo.IRPJ, found.get().getTipoTributo());
    assertEquals(TipoSaldo.DEVEDOR, found.get().getTipoSaldo());
    assertEquals(0, found.get().getSaldoInicial().compareTo(new BigDecimal("10000.00")));
  }

  @Test
  @DisplayName("Should enforce unique constraint on company + codigo + anoBase")
  void shouldEnforceUniqueConstraintOnCompanyCodigoAnoBase() {
    // Arrange
    ContaParteB conta1 =
        createTestContaParteB("4.01.01", "Prejuízo Fiscal IRPJ", 2024, TipoTributo.IRPJ);
    repositoryAdapter.save(conta1);

    // Act & Assert - tentar salvar conta duplicada (mesmo company + codigo + ano)
    ContaParteB conta2 =
        createTestContaParteB("4.01.01", "Outro Prejuízo", 2024, TipoTributo.IRPJ);

    assertThrows(
        DataIntegrityViolationException.class,
        () -> repositoryAdapter.save(conta2),
        "Should throw exception when saving duplicate company + codigo + anoBase");
  }

  @Test
  @DisplayName("Should allow same codigo for different anos")
  void shouldAllowSameCodigoForDifferentAnos() {
    // Arrange
    ContaParteB conta2024 =
        createTestContaParteB("4.01.01", "Prejuízo 2024", 2024, TipoTributo.IRPJ);
    ContaParteB conta2025 =
        createTestContaParteB("4.01.01", "Prejuízo 2025", 2025, TipoTributo.IRPJ);

    // Act
    ContaParteB saved2024 = repositoryAdapter.save(conta2024);
    ContaParteB saved2025 = repositoryAdapter.save(conta2025);

    // Assert
    assertNotNull(saved2024.getId());
    assertNotNull(saved2025.getId());
    assertEquals(2024, saved2024.getAnoBase());
    assertEquals(2025, saved2025.getAnoBase());
  }

  @Test
  @DisplayName("Should find conta by company + codigo + anoBase")
  void shouldFindContaByCompanyCodigoAnoBase() {
    // Arrange
    ContaParteB conta =
        createTestContaParteB("4.01.01", "Prejuízo Fiscal IRPJ", 2024, TipoTributo.IRPJ);
    repositoryAdapter.save(conta);

    // Act
    Optional<ContaParteB> found =
        repositoryAdapter.findByCompanyIdAndCodigoContaAndAnoBase(testCompanyId, "4.01.01", 2024);

    // Assert
    assertTrue(found.isPresent());
    assertEquals("4.01.01", found.get().getCodigoConta());
    assertEquals(2024, found.get().getAnoBase());
  }

  @Test
  @DisplayName("Should find all contas by company and anoBase")
  void shouldFindAllContasByCompanyAndAnoBase() {
    // Arrange
    repositoryAdapter.save(
        createTestContaParteB("4.01.01", "Prejuízo IRPJ", 2024, TipoTributo.IRPJ));
    repositoryAdapter.save(
        createTestContaParteB("4.01.02", "Prejuízo CSLL", 2024, TipoTributo.CSLL));
    repositoryAdapter.save(
        createTestContaParteB("4.01.03", "Prejuízo 2023", 2023, TipoTributo.IRPJ));

    // Act
    List<ContaParteB> contas2024 =
        repositoryAdapter.findByCompanyIdAndAnoBase(testCompanyId, 2024);

    // Assert
    assertEquals(2, contas2024.size());
    assertTrue(contas2024.stream().allMatch(c -> c.getAnoBase().equals(2024)));
  }

  @Test
  @DisplayName("Should paginate contas by company")
  void shouldPaginateContasByCompany() {
    // Arrange - criar 5 contas
    for (int i = 1; i <= 5; i++) {
      repositoryAdapter.save(
          createTestContaParteB(
              "4.01.0" + i, "Conta " + i, 2024, i % 2 == 0 ? TipoTributo.IRPJ : TipoTributo.CSLL));
    }

    // Act
    Page<ContaParteB> page1 = repositoryAdapter.findByCompanyId(testCompanyId, PageRequest.of(0, 3));
    Page<ContaParteB> page2 = repositoryAdapter.findByCompanyId(testCompanyId, PageRequest.of(1, 3));

    // Assert
    assertEquals(5, page1.getTotalElements());
    assertEquals(2, page1.getTotalPages());
    assertEquals(3, page1.getContent().size());
    assertEquals(2, page2.getContent().size());
  }

  @Test
  @DisplayName("Should persist and retrieve TipoTributo enum correctly")
  void shouldPersistTipoTributoEnum() {
    // Arrange & Act - testar cada tipo
    ContaParteB contaIrpj =
        createTestContaParteB("4.01.01", "Conta IRPJ", 2024, TipoTributo.IRPJ);
    ContaParteB contaCsll =
        createTestContaParteB("4.01.02", "Conta CSLL", 2024, TipoTributo.CSLL);
    ContaParteB contaAmbos =
        createTestContaParteB("4.01.03", "Conta AMBOS", 2024, TipoTributo.AMBOS);

    ContaParteB savedIrpj = repositoryAdapter.save(contaIrpj);
    ContaParteB savedCsll = repositoryAdapter.save(contaCsll);
    ContaParteB savedAmbos = repositoryAdapter.save(contaAmbos);

    // Assert
    assertEquals(TipoTributo.IRPJ, repositoryAdapter.findById(savedIrpj.getId()).get().getTipoTributo());
    assertEquals(TipoTributo.CSLL, repositoryAdapter.findById(savedCsll.getId()).get().getTipoTributo());
    assertEquals(TipoTributo.AMBOS, repositoryAdapter.findById(savedAmbos.getId()).get().getTipoTributo());
  }

  @Test
  @DisplayName("Should persist and retrieve TipoSaldo enum correctly")
  void shouldPersistTipoSaldoEnum() {
    // Arrange
    ContaParteB contaDevedor =
        ContaParteB.builder()
            .companyId(testCompanyId)
            .codigoConta("4.01.01")
            .descricao("Conta Devedor")
            .anoBase(2024)
            .dataVigenciaInicio(LocalDate.of(2024, 1, 1))
            .tipoTributo(TipoTributo.IRPJ)
            .saldoInicial(new BigDecimal("5000.00"))
            .tipoSaldo(TipoSaldo.DEVEDOR)
            .status(Status.ACTIVE)
            .build();

    ContaParteB contaCredor =
        ContaParteB.builder()
            .companyId(testCompanyId)
            .codigoConta("4.01.02")
            .descricao("Conta Credor")
            .anoBase(2024)
            .dataVigenciaInicio(LocalDate.of(2024, 1, 1))
            .tipoTributo(TipoTributo.CSLL)
            .saldoInicial(new BigDecimal("3000.00"))
            .tipoSaldo(TipoSaldo.CREDOR)
            .status(Status.ACTIVE)
            .build();

    // Act
    ContaParteB savedDevedor = repositoryAdapter.save(contaDevedor);
    ContaParteB savedCredor = repositoryAdapter.save(contaCredor);

    // Assert
    assertEquals(TipoSaldo.DEVEDOR, repositoryAdapter.findById(savedDevedor.getId()).get().getTipoSaldo());
    assertEquals(TipoSaldo.CREDOR, repositoryAdapter.findById(savedCredor.getId()).get().getTipoSaldo());
  }

  @Test
  @DisplayName("Should handle soft delete (status INACTIVE)")
  void shouldHandleSoftDelete() {
    // Arrange
    ContaParteB conta =
        createTestContaParteB("4.01.01", "Prejuízo Fiscal", 2024, TipoTributo.IRPJ);
    ContaParteB saved = repositoryAdapter.save(conta);

    // Act - marcar como INACTIVE
    saved.setStatus(Status.INACTIVE);
    repositoryAdapter.save(saved);

    // Assert - conta ainda existe no banco
    Optional<ContaParteB> found = repositoryAdapter.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals(Status.INACTIVE, found.get().getStatus());
  }

  private ContaParteB createTestContaParteB(
      String codigo, String descricao, Integer anoBase, TipoTributo tipoTributo) {
    return ContaParteB.builder()
        .companyId(testCompanyId)
        .codigoConta(codigo)
        .descricao(descricao)
        .anoBase(anoBase)
        .dataVigenciaInicio(LocalDate.of(anoBase, 1, 1))
        .tipoTributo(tipoTributo)
        .saldoInicial(new BigDecimal("10000.00"))
        .tipoSaldo(TipoSaldo.DEVEDOR)
        .status(Status.ACTIVE)
        .build();
  }
}
