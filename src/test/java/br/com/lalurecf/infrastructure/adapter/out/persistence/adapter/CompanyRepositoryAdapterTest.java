package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.Company;
import br.com.lalurecf.domain.model.valueobject.CNPJ;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import java.time.LocalDate;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("CompanyRepositoryAdapter Integration Tests")
class CompanyRepositoryAdapterTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.5-alpine")
      .withDatabaseName("ecf_test_db")
      .withUsername("test_user")
      .withPassword("test_pass");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  private CompanyRepositoryAdapter repositoryAdapter;

  @Autowired
  private CompanyJpaRepository jpaRepository;

  @BeforeEach
  void setUp() {
    jpaRepository.deleteAll();
  }

  @Test
  @DisplayName("Should save company and retrieve by CNPJ")
  void shouldSaveCompanyAndRetrieveByCnpj() {
    // Arrange
    Company company = createTestCompany("11222333000181", "Test Company LTDA");

    // Act
    Company saved = repositoryAdapter.save(company);
    Optional<Company> found = repositoryAdapter.findByCnpj("11222333000181");

    // Assert
    assertNotNull(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("11222333000181", found.get().getCnpj().getValue());
    assertEquals("Test Company LTDA", found.get().getRazaoSocial());
  }

  @Test
  @DisplayName("Should throw exception when saving company with duplicate CNPJ")
  void shouldThrowExceptionWhenSavingCompanyWithDuplicateCnpj() {
    // Arrange
    Company company1 = createTestCompany("11222333000181", "Company 1");
    Company company2 = createTestCompany("11222333000181", "Company 2");

    // Act
    repositoryAdapter.save(company1);

    // Assert
    assertThrows(DataIntegrityViolationException.class, () -> {
      repositoryAdapter.save(company2);
    });
  }

  @Test
  @DisplayName("Should throw exception when saving company with invalid CNPJ")
  void shouldThrowExceptionWhenSavingCompanyWithInvalidCnpj() {
    // Arrange & Act & Assert
    assertThrows(IllegalArgumentException.class, () -> {
      createTestCompany("12345678901234", "Invalid CNPJ Company");
    });
  }

  @Test
  @DisplayName("Should save company and retrieve by ID")
  void shouldSaveCompanyAndRetrieveById() {
    // Arrange
    Company company = createTestCompany("11222333000181", "Test Company LTDA");

    // Act
    Company saved = repositoryAdapter.save(company);
    Optional<Company> found = repositoryAdapter.findById(saved.getId());

    // Assert
    assertTrue(found.isPresent());
    assertEquals(saved.getId(), found.get().getId());
    assertEquals("11222333000181", found.get().getCnpj().getValue());
  }

  @Test
  @DisplayName("Should persist periodoContabil correctly")
  void shouldPersistPeriodoContabilCorrectly() {
    // Arrange
    LocalDate periodoContabil = LocalDate.of(2024, 1, 1);
    Company company = createTestCompany("11222333000181", "Test Company LTDA");
    company.setPeriodoContabil(periodoContabil);

    // Act
    Company saved = repositoryAdapter.save(company);
    Optional<Company> found = repositoryAdapter.findById(saved.getId());

    // Assert
    assertTrue(found.isPresent());
    assertEquals(periodoContabil, found.get().getPeriodoContabil());
  }

  @Test
  @DisplayName("Should list all companies with pagination")
  void shouldListAllCompaniesWithPagination() {
    // Arrange
    repositoryAdapter.save(createTestCompany("11222333000181", "Company 1"));
    repositoryAdapter.save(createTestCompany("11444777000161", "Company 2"));
    repositoryAdapter.save(createTestCompany("12345678000195", "Company 3"));

    // Act
    Page<Company> page = repositoryAdapter.findAll(PageRequest.of(0, 2));

    // Assert
    assertEquals(2, page.getContent().size());
    assertEquals(3, page.getTotalElements());
    assertEquals(2, page.getTotalPages());
  }

  @Test
  @DisplayName("Should save company with status ACTIVE by default")
  void shouldSaveCompanyWithStatusActiveByDefault() {
    // Arrange
    Company company = createTestCompany("11222333000181", "Test Company LTDA");

    // Act
    Company saved = repositoryAdapter.save(company);
    Optional<Company> found = repositoryAdapter.findById(saved.getId());

    // Assert
    assertTrue(found.isPresent());
    assertEquals(Status.ACTIVE, found.get().getStatus());
  }

  @Test
  @DisplayName("Should update company status to INACTIVE (soft delete)")
  void shouldUpdateCompanyStatusToInactive() {
    // Arrange
    Company company = createTestCompany("60701190000104", "Test Company LTDA");
    Company saved = repositoryAdapter.save(company);

    // Act
    saved.setStatus(Status.INACTIVE);
    Company updated = repositoryAdapter.save(saved);
    Optional<Company> found = repositoryAdapter.findById(updated.getId());

    // Assert
    assertTrue(found.isPresent());
    assertEquals(Status.INACTIVE, found.get().getStatus());
  }

  @Test
  @DisplayName("Should format CNPJ correctly when retrieved")
  void shouldFormatCnpjCorrectlyWhenRetrieved() {
    // Arrange
    Company company = createTestCompany("00000000000191", "Test Company LTDA");

    // Act
    Company saved = repositoryAdapter.save(company);
    Optional<Company> found = repositoryAdapter.findById(saved.getId());

    // Assert
    assertTrue(found.isPresent());
    assertEquals("00.000.000/0001-91", found.get().getCnpj().format());
  }

  private Company createTestCompany(String cnpj, String razaoSocial) {
    Company company = new Company();
    company.setCnpj(CNPJ.of(cnpj));
    company.setRazaoSocial(razaoSocial);
    company.setPeriodoContabil(LocalDate.of(2024, 1, 1));
    company.setStatus(Status.ACTIVE);
    return company;
  }
}
