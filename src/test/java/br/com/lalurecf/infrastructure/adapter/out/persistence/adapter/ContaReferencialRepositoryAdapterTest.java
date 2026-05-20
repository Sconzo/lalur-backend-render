package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.ContaReferencial;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.ContaReferencialJpaRepository;
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
@DisplayName("ContaReferencialRepositoryAdapter Integration Tests")
class ContaReferencialRepositoryAdapterTest {

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

    @Autowired private ContaReferencialRepositoryAdapter repositoryAdapter;

    @Autowired private ContaReferencialJpaRepository jpaRepository;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save conta referencial and retrieve by ID")
    void shouldSaveContaReferencialAndRetrieveById() {
        // Arrange
        ContaReferencial conta = createTestContaReferencial("1.01.01", "Caixa", 2024);

        // Act
        ContaReferencial saved = repositoryAdapter.save(conta);
        Optional<ContaReferencial> found = repositoryAdapter.findById(saved.getId());

        // Assert
        assertNotNull(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("1.01.01", found.get().getCodigoRfb());
        assertEquals("Caixa", found.get().getDescricao());
        assertEquals(2024, found.get().getAnoValidade());
    }

    @Test
    @DisplayName("Should save conta referencial and retrieve by codigo RFB")
    void shouldSaveContaReferencialAndRetrieveByCodigoRfb() {
        // Arrange
        ContaReferencial conta = createTestContaReferencial("1.01.01", "Caixa", 2024);

        // Act
        repositoryAdapter.save(conta);
        Optional<ContaReferencial> found = repositoryAdapter.findByCodigoRfb("1.01.01");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("1.01.01", found.get().getCodigoRfb());
        assertEquals("Caixa", found.get().getDescricao());
    }

    @Test
    @DisplayName("Should find all contas by ano validade")
    void shouldFindAllContasByAnoValidade() {
        // Arrange
        repositoryAdapter.save(createTestContaReferencial("1.01.01", "Caixa", 2024));
        repositoryAdapter.save(createTestContaReferencial("1.01.02", "Bancos", 2024));
        repositoryAdapter.save(createTestContaReferencial("2.01.01", "Fornecedores", 2023));

        // Act
        List<ContaReferencial> contas2024 = repositoryAdapter.findByAnoValidade(2024);
        List<ContaReferencial> contas2023 = repositoryAdapter.findByAnoValidade(2023);

        // Assert
        assertEquals(2, contas2024.size());
        assertEquals(1, contas2023.size());
    }

    @Test
    @DisplayName("Should enforce unique constraint on (codigo_rfb, ano_validade)")
    void shouldEnforceUniqueConstraintOnCodigoRfbAndAnoValidade() {
        // Arrange
        ContaReferencial conta1 = createTestContaReferencial("1.01.01", "Caixa", 2024);
        ContaReferencial conta2 = createTestContaReferencial("1.01.01", "Caixa Duplicado", 2024);

        // Act
        repositoryAdapter.save(conta1);

        // Assert
        assertThrows(
                DataIntegrityViolationException.class,
                () -> {
                    repositoryAdapter.save(conta2);
                });
    }

    @Test
    @DisplayName("Should allow same codigo RFB for different years (versioning)")
    void shouldAllowSameCodigoRfbForDifferentYears() {
        // Arrange
        ContaReferencial conta2023 = createTestContaReferencial("1.01.01", "Caixa 2023", 2023);
        ContaReferencial conta2024 = createTestContaReferencial("1.01.01", "Caixa 2024", 2024);

        // Act
        ContaReferencial saved2023 = repositoryAdapter.save(conta2023);
        ContaReferencial saved2024 = repositoryAdapter.save(conta2024);

        // Assert
        assertNotNull(saved2023.getId());
        assertNotNull(saved2024.getId());
        assertEquals("1.01.01", saved2023.getCodigoRfb());
        assertEquals("1.01.01", saved2024.getCodigoRfb());
        assertEquals(2023, saved2023.getAnoValidade());
        assertEquals(2024, saved2024.getAnoValidade());
    }

    @Test
    @DisplayName("Should allow null ano validade (valid for all years)")
    void shouldAllowNullAnoValidade() {
        // Arrange
        ContaReferencial conta = createTestContaReferencial("1.01.01", "Caixa Geral", null);

        // Act
        ContaReferencial saved = repositoryAdapter.save(conta);
        Optional<ContaReferencial> found = repositoryAdapter.findById(saved.getId());

        // Assert
        assertTrue(found.isPresent());
        assertEquals("1.01.01", found.get().getCodigoRfb());
        assertEquals(null, found.get().getAnoValidade());
    }

    @Test
    @DisplayName("Should list all contas referenciais with pagination")
    void shouldListAllContasReferenciaisWithPagination() {
        // Arrange
        repositoryAdapter.save(createTestContaReferencial("1.01.01", "Caixa", 2024));
        repositoryAdapter.save(createTestContaReferencial("1.01.02", "Bancos", 2024));
        repositoryAdapter.save(createTestContaReferencial("2.01.01", "Fornecedores", 2024));

        // Act
        Page<ContaReferencial> page = repositoryAdapter.findAll(PageRequest.of(0, 2));

        // Assert
        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    @DisplayName("Should save conta referencial with status ACTIVE by default")
    void shouldSaveContaReferencialWithStatusActiveByDefault() {
        // Arrange
        ContaReferencial conta = createTestContaReferencial("1.01.01", "Caixa", 2024);

        // Act
        ContaReferencial saved = repositoryAdapter.save(conta);
        Optional<ContaReferencial> found = repositoryAdapter.findById(saved.getId());

        // Assert
        assertTrue(found.isPresent());
        assertEquals(Status.ACTIVE, found.get().getStatus());
    }

    @Test
    @DisplayName("Should update conta referencial status to INACTIVE (soft delete)")
    void shouldUpdateContaReferencialStatusToInactive() {
        // Arrange
        ContaReferencial conta = createTestContaReferencial("1.01.01", "Caixa", 2024);
        ContaReferencial saved = repositoryAdapter.save(conta);

        // Act
        saved.setStatus(Status.INACTIVE);
        ContaReferencial updated = repositoryAdapter.save(saved);
        Optional<ContaReferencial> found = repositoryAdapter.findById(updated.getId());

        // Assert
        assertTrue(found.isPresent());
        assertEquals(Status.INACTIVE, found.get().getStatus());
    }

    @Test
    @DisplayName("Should update existing conta referencial preserving ID")
    void shouldUpdateExistingContaReferencialPreservingId() {
        // Arrange
        ContaReferencial conta = createTestContaReferencial("1.01.01", "Caixa Original", 2024);
        ContaReferencial saved = repositoryAdapter.save(conta);
        Long originalId = saved.getId();

        // Act
        saved.setDescricao("Caixa Atualizado");
        ContaReferencial updated = repositoryAdapter.save(saved);

        // Assert
        assertEquals(originalId, updated.getId());
        assertEquals("Caixa Atualizado", updated.getDescricao());
    }

    @Test
    @DisplayName("Should find all contas referenciais without pagination")
    void shouldFindAllContasReferenciaisWithoutPagination() {
        // Arrange
        repositoryAdapter.save(createTestContaReferencial("1.01.01", "Caixa", 2024));
        repositoryAdapter.save(createTestContaReferencial("1.01.02", "Bancos", 2024));
        repositoryAdapter.save(createTestContaReferencial("2.01.01", "Fornecedores", 2024));

        // Act
        List<ContaReferencial> allContas = repositoryAdapter.findAll();

        // Assert
        assertEquals(3, allContas.size());
    }

    private ContaReferencial createTestContaReferencial(
            String codigoRfb, String descricao, Integer anoValidade) {
        ContaReferencial conta = new ContaReferencial();
        conta.setCodigoRfb(codigoRfb);
        conta.setDescricao(descricao);
        conta.setAnoValidade(anoValidade);
        conta.setStatus(Status.ACTIVE);
        return conta;
    }
}
