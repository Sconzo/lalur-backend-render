package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.lalurecf.domain.enums.EcfFileStatus;
import br.com.lalurecf.domain.enums.EcfFileType;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.EcfFile;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.EcfFileJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("EcfFileRepositoryAdapter Integration Tests")
class EcfFileRepositoryAdapterTest {

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

    @Autowired
    private EcfFileRepositoryAdapter repositoryAdapter;

    @Autowired
    private EcfFileJpaRepository jpaRepository;

    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    private Long testCompanyId;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
        companyJpaRepository.deleteAll();

        CompanyEntity company = new CompanyEntity();
        company.setCnpj("12345678000199");
        company.setRazaoSocial("Empresa Teste Ltda");
        company.setPeriodoContabil(LocalDate.of(2024, 12, 31));
        company.setStatus(Status.ACTIVE);
        company.setCreatedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());

        testCompanyId = companyJpaRepository.save(company).getId();
    }

    @Test
    @DisplayName("Should save EcfFile and retrieve by ID")
    void shouldSaveEcfFileAndRetrieveById() {
        EcfFile file = buildEcfFile(EcfFileType.ARQUIVO_PARCIAL, "|M001|0|1|\n|M990|2|");

        EcfFile saved = repositoryAdapter.saveOrReplace(file);
        Optional<EcfFile> found = repositoryAdapter.findById(saved.getId());

        assertNotNull(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(EcfFileType.ARQUIVO_PARCIAL, found.get().getFileType());
        assertEquals(2024, found.get().getFiscalYear());
        assertEquals("|M001|0|1|\n|M990|2|", found.get().getContent());
        assertEquals(EcfFileStatus.DRAFT, found.get().getFileStatus());
    }

    @Test
    @DisplayName("Should upsert: second save replaces first (same type+company+year)")
    void shouldUpsertSecondSaveReplacesFirst() {
        EcfFile v1 = buildEcfFile(EcfFileType.ARQUIVO_PARCIAL, "conteudo versao 1");
        repositoryAdapter.saveOrReplace(v1);

        EcfFile v2 = buildEcfFile(EcfFileType.ARQUIVO_PARCIAL, "conteudo versao 2");
        EcfFile saved = repositoryAdapter.saveOrReplace(v2);

        // Verify only one record in DB
        List<EcfFile> all = repositoryAdapter.findByCompanyAndFiscalYear(testCompanyId, 2024);
        assertEquals(1, all.size());
        assertEquals("conteudo versao 2", saved.getContent());
        assertEquals("conteudo versao 2", all.get(0).getContent());
    }

    @Test
    @DisplayName("Should retrieve content intact for large text")
    void shouldRetrieveContentIntactForLargeText() {
        String largeContent = "|M001|0|1|\n".repeat(10000) + "|M990|10001|";
        EcfFile file = buildEcfFile(EcfFileType.IMPORTED_ECF, largeContent);

        EcfFile saved = repositoryAdapter.saveOrReplace(file);
        Optional<EcfFile> found = repositoryAdapter.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(largeContent, found.get().getContent());
    }

    @Test
    @DisplayName("Should allow distinct types to coexist for same company and year")
    void shouldAllowDistinctTypesToCoexistForSameCompanyAndYear() {
        repositoryAdapter.saveOrReplace(buildEcfFile(EcfFileType.ARQUIVO_PARCIAL, "parcial"));
        repositoryAdapter.saveOrReplace(buildEcfFile(EcfFileType.IMPORTED_ECF, "importado"));
        repositoryAdapter.saveOrReplace(buildEcfFile(EcfFileType.COMPLETE_ECF, "completo"));

        List<EcfFile> all = repositoryAdapter.findByCompanyAndFiscalYear(testCompanyId, 2024);
        assertEquals(3, all.size());

        Optional<EcfFile> parcial = repositoryAdapter.findByCompanyAndFiscalYearAndType(
            testCompanyId, 2024, EcfFileType.ARQUIVO_PARCIAL);
        Optional<EcfFile> importado = repositoryAdapter.findByCompanyAndFiscalYearAndType(
            testCompanyId, 2024, EcfFileType.IMPORTED_ECF);
        Optional<EcfFile> completo = repositoryAdapter.findByCompanyAndFiscalYearAndType(
            testCompanyId, 2024, EcfFileType.COMPLETE_ECF);

        assertTrue(parcial.isPresent());
        assertTrue(importado.isPresent());
        assertTrue(completo.isPresent());
        assertEquals("parcial", parcial.get().getContent());
        assertEquals("importado", importado.get().getContent());
        assertEquals("completo", completo.get().getContent());
    }

    @Test
    @DisplayName("Should find by company and fiscal year returning empty for wrong year")
    void shouldReturnEmptyForWrongYear() {
        repositoryAdapter.saveOrReplace(buildEcfFile(EcfFileType.ARQUIVO_PARCIAL, "2024 content"));

        List<EcfFile> found2025 = repositoryAdapter.findByCompanyAndFiscalYear(testCompanyId, 2025);
        assertTrue(found2025.isEmpty());
    }

    @Test
    @DisplayName("Should find by company, year and type returning empty when not found")
    void shouldReturnEmptyForTypeNotFound() {
        repositoryAdapter.saveOrReplace(buildEcfFile(EcfFileType.ARQUIVO_PARCIAL, "parcial"));

        Optional<EcfFile> importado = repositoryAdapter.findByCompanyAndFiscalYearAndType(
            testCompanyId, 2024, EcfFileType.IMPORTED_ECF);
        assertTrue(importado.isEmpty());
    }

    private EcfFile buildEcfFile(EcfFileType type, String content) {
        return EcfFile.builder()
            .fileType(type)
            .companyId(testCompanyId)
            .fiscalYear(2024)
            .content(content)
            .fileName("ECF_" + type.name() + "_2024.txt")
            .fileStatus(EcfFileStatus.DRAFT)
            .generatedAt(LocalDateTime.now())
            .generatedBy("test@example.com")
            .status(Status.ACTIVE)
            .build();
    }
}
