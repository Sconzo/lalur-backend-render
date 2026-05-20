package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.lalurecf.domain.enums.ParameterNature;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.TaxParameter;
import br.com.lalurecf.domain.model.TaxParameterType;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterEntity;
import br.com.lalurecf.util.IntegrationTestBase;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

/**
 * Teste de integração para TaxParameterRepositoryAdapter.
 *
 * <p>Valida a integração entre adapter, mapper e JPA repository, verificando conversões entity ↔
 * domain e operações de persistência.
 */
@DisplayName("TaxParameterRepositoryAdapter - Testes de Integração")
@Transactional
class TaxParameterRepositoryAdapterTest extends IntegrationTestBase {

  @Autowired private TaxParameterRepositoryAdapter adapter;
  @Autowired private TaxParameterTypeRepositoryAdapter typeAdapter;

  private TaxParameterType irpjType;
  private TaxParameterType csllType;
  private TaxParameterType cnaeType;
  private TaxParameterType geralType;

  @BeforeEach
  void setUp() {
    // Create test types with unique names to avoid conflict with seed data
    irpjType =
        typeAdapter.save(
            TaxParameterType.builder()
                .description("TEST_IRPJ")
                .nature(ParameterNature.QUARTERLY)
                .status(Status.ACTIVE)
                .build());

    csllType =
        typeAdapter.save(
            TaxParameterType.builder()
                .description("TEST_CSLL")
                .nature(ParameterNature.QUARTERLY)
                .status(Status.ACTIVE)
                .build());

    cnaeType =
        typeAdapter.save(
            TaxParameterType.builder()
                .description("TEST_CNAE")
                .nature(ParameterNature.GLOBAL)
                .status(Status.ACTIVE)
                .build());

    geralType =
        typeAdapter.save(
            TaxParameterType.builder()
                .description("TEST_GERAL")
                .nature(ParameterNature.GLOBAL)
                .status(Status.ACTIVE)
                .build());
  }

  @Test
  @DisplayName("Should save tax parameter and return domain model with generated ID")
  void shouldSaveTaxParameter() {
    // Given
    TaxParameter taxParameter =
        TaxParameter.builder()
            .code("TEST001")
            .typeId(irpjType.getId())
            .type(irpjType)
            .description("Teste de Parâmetro IRPJ")
            .status(Status.ACTIVE)
            .createdBy(1L)
            .build();

    // When
    TaxParameter saved = adapter.save(taxParameter);

    // Then
    assertThat(saved).isNotNull();
    assertThat(saved.getId()).isNotNull().isPositive();
    assertThat(saved.getCode()).isEqualTo("TEST001");
    assertThat(saved.getTypeId()).isEqualTo(irpjType.getId());
    assertThat(saved.getType()).isNotNull();
    assertThat(saved.getType().getDescription()).isEqualTo("TEST_IRPJ");
    assertThat(saved.getDescription()).isEqualTo("Teste de Parâmetro IRPJ");
    assertThat(saved.getStatus()).isEqualTo(Status.ACTIVE);
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should find tax parameter by code")
  void shouldFindByCode() {
    // Given - create a test parameter
    TaxParameter taxParameter =
        TaxParameter.builder()
            .code("TEST002")
            .typeId(csllType.getId())
            .type(csllType)
            .description("Teste de Parâmetro CSLL")
            .status(Status.ACTIVE)
            .createdBy(1L)
            .build();
    adapter.save(taxParameter);

    // When
    Optional<TaxParameter> found = adapter.findByCode("TEST002");

    // Then
    assertThat(found).isPresent();
    assertThat(found.get().getCode()).isEqualTo("TEST002");
    assertThat(found.get().getType().getDescription()).isEqualTo("TEST_CSLL");
    assertThat(found.get().getDescription()).isEqualTo("Teste de Parâmetro CSLL");
  }

  @Test
  @DisplayName("Should return empty when code not found")
  void shouldReturnEmptyWhenCodeNotFound() {
    // When
    Optional<TaxParameter> found = adapter.findByCode("NONEXISTENT");

    // Then
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should find tax parameter by ID")
  void shouldFindById() {
    // Given
    TaxParameter taxParameter =
        TaxParameter.builder()
            .code("TEST003")
            .typeId(geralType.getId())
            .type(geralType)
            .description("Teste de Parâmetro Geral")
            .status(Status.ACTIVE)
            .createdBy(1L)
            .build();
    TaxParameter saved = adapter.save(taxParameter);

    // When
    Optional<TaxParameter> found = adapter.findById(saved.getId());

    // Then
    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(saved.getId());
    assertThat(found.get().getCode()).isEqualTo("TEST003");
  }

  @Test
  @DisplayName("Should find tax parameters by type ID")
  void shouldFindByTypeId() {
    // Given - create multiple parameters of same type
    TaxParameter param1 =
        TaxParameter.builder()
            .code("CNAE001")
            .typeId(cnaeType.getId())
            .type(cnaeType)
            .description("CNAE 1")
            .status(Status.ACTIVE)
            .createdBy(1L)
            .build();
    TaxParameter param2 =
        TaxParameter.builder()
            .code("CNAE002")
            .typeId(cnaeType.getId())
            .type(cnaeType)
            .description("CNAE 2")
            .status(Status.ACTIVE)
            .createdBy(1L)
            .build();
    adapter.save(param1);
    adapter.save(param2);

    // When
    List<TaxParameter> found = adapter.findByTypeId(cnaeType.getId());

    // Then
    assertThat(found).hasSizeGreaterThanOrEqualTo(2);
    assertThat(found)
        .extracting(tp -> tp.getType().getDescription())
        .containsOnly("TEST_CNAE");
  }

  @Test
  @DisplayName("Should find all tax parameters")
  void shouldFindAll() {
    // Given - ensure at least one parameter exists
    TaxParameter taxParameter =
        TaxParameter.builder()
            .code("TEST_ALL")
            .typeId(geralType.getId())
            .type(geralType)
            .description("Teste FindAll")
            .status(Status.ACTIVE)
            .createdBy(1L)
            .build();
    adapter.save(taxParameter);

    // When
    List<TaxParameter> all = adapter.findAll();

    // Then
    assertThat(all).isNotEmpty();
    assertThat(all).allMatch(tp -> tp.getId() != null);
  }

  @Test
  @DisplayName("Should find tax parameters with pagination and specification")
  void shouldFindAllWithSpecificationAndPagination() {
    // Given - create test parameters
    TaxParameter param1 =
        TaxParameter.builder()
            .code("SPEC001")
            .typeId(irpjType.getId())
            .type(irpjType)
            .description("Especificação IRPJ")
            .status(Status.ACTIVE)
            .createdBy(1L)
            .build();
    TaxParameter param2 =
        TaxParameter.builder()
            .code("SPEC002")
            .typeId(irpjType.getId())
            .type(irpjType)
            .description("Especificação IRPJ 2")
            .status(Status.INACTIVE)
            .createdBy(1L)
            .build();
    adapter.save(param1);
    adapter.save(param2);

    // When - filter by type and active status
    Specification<TaxParameterEntity> spec =
        (root, query, criteriaBuilder) ->
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get("tipoParametro").get("id"), irpjType.getId()),
                criteriaBuilder.equal(root.get("status"), Status.ACTIVE));

    Pageable pageable = PageRequest.of(0, 10);
    Page<TaxParameter> page = adapter.findAll(spec, pageable);

    // Then
    assertThat(page).isNotNull();
    assertThat(page.getContent()).isNotEmpty();
    assertThat(page.getContent())
        .allMatch(
            tp ->
                tp.getType().getDescription().equals("TEST_IRPJ")
                    && Status.ACTIVE.equals(tp.getStatus()));
  }

  @Test
  @DisplayName("Should find tax parameters by IDs and type ID")
  void shouldFindByIdInAndTypeId() {
    // Given
    TaxParameter param1 =
        TaxParameter.builder()
            .code("IDIN001")
            .typeId(csllType.getId())
            .type(csllType)
            .description("ID In Test 1")
            .status(Status.ACTIVE)
            .createdBy(1L)
            .build();
    TaxParameter param2 =
        TaxParameter.builder()
            .code("IDIN002")
            .typeId(csllType.getId())
            .type(csllType)
            .description("ID In Test 2")
            .status(Status.ACTIVE)
            .createdBy(1L)
            .build();
    TaxParameter param3 =
        TaxParameter.builder()
            .code("IDIN003")
            .typeId(irpjType.getId()) // Different type
            .type(irpjType)
            .description("ID In Test 3")
            .status(Status.ACTIVE)
            .createdBy(1L)
            .build();

    TaxParameter saved1 = adapter.save(param1);
    TaxParameter saved2 = adapter.save(param2);
    TaxParameter saved3 = adapter.save(param3);

    // When - search for all 3 IDs but filter by type CSLL
    List<Long> ids = List.of(saved1.getId(), saved2.getId(), saved3.getId());
    List<TaxParameter> found = adapter.findByIdInAndTypeId(ids, csllType.getId());

    // Then - should only return param1 and param2
    assertThat(found).hasSize(2);
    assertThat(found)
        .extracting(tp -> tp.getType().getDescription())
        .containsOnly("TEST_CSLL");
    assertThat(found)
        .extracting(TaxParameter::getCode)
        .containsExactlyInAnyOrder("IDIN001", "IDIN002");
  }

  @Test
  @DisplayName("Should find distinct types")
  void shouldFindDistinctTypes() {
    // Given - ensure we have parameters with different types
    TaxParameterType typeA =
        typeAdapter.save(
            TaxParameterType.builder()
                .description("TYPE_A")
                .nature(ParameterNature.GLOBAL)
                .status(Status.ACTIVE)
                .build());
    TaxParameterType typeB =
        typeAdapter.save(
            TaxParameterType.builder()
                .description("TYPE_B")
                .nature(ParameterNature.MONTHLY)
                .status(Status.ACTIVE)
                .build());

    adapter.save(
        TaxParameter.builder()
            .code("TYPE001")
            .typeId(typeA.getId())
            .type(typeA)
            .description("Type A")
            .status(Status.ACTIVE)
            .createdBy(1L)
            .build());
    adapter.save(
        TaxParameter.builder()
            .code("TYPE002")
            .typeId(typeB.getId())
            .type(typeB)
            .description("Type B")
            .status(Status.ACTIVE)
            .createdBy(1L)
            .build());

    // When
    List<String> types = adapter.findDistinctTypes();

    // Then
    assertThat(types).isNotEmpty();
    assertThat(types).contains("TYPE_A", "TYPE_B");
  }

  @Test
  @DisplayName("Should update existing tax parameter")
  void shouldUpdateTaxParameter() {
    // Given - create initial parameter
    TaxParameter original =
        TaxParameter.builder()
            .code("UPDATE001")
            .typeId(geralType.getId())
            .type(geralType)
            .description("Descrição Original")
            .status(Status.ACTIVE)
            .createdBy(1L)
            .build();
    TaxParameter saved = adapter.save(original);

    // When - update the parameter
    TaxParameter updated =
        saved.toBuilder()
            .description("Descrição Atualizada")
            .status(Status.INACTIVE)
            .updatedBy(2L)
            .build();
    TaxParameter result = adapter.save(updated);

    // Then
    assertThat(result.getId()).isEqualTo(saved.getId());
    assertThat(result.getDescription()).isEqualTo("Descrição Atualizada");
    assertThat(result.getStatus()).isEqualTo(Status.INACTIVE);
    assertThat(result.getUpdatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should handle soft delete - inactive parameters still retrievable by ID")
  void shouldHandleSoftDelete() {
    // Given - create and then mark as inactive
    TaxParameter taxParameter =
        TaxParameter.builder()
            .code("SOFT001")
            .typeId(geralType.getId())
            .type(geralType)
            .description("Soft Delete Test")
            .status(Status.ACTIVE)
            .createdBy(1L)
            .build();
    TaxParameter saved = adapter.save(taxParameter);

    // When - soft delete by setting status to INACTIVE
    TaxParameter deleted = saved.toBuilder().status(Status.INACTIVE).build();
    adapter.save(deleted);

    // Then - should still be retrievable by ID and code
    Optional<TaxParameter> foundById = adapter.findById(saved.getId());
    assertThat(foundById).isPresent();
    assertThat(foundById.get().getStatus()).isEqualTo(Status.INACTIVE);

    Optional<TaxParameter> foundByCode = adapter.findByCode("SOFT001");
    assertThat(foundByCode).isPresent();
    assertThat(foundByCode.get().getStatus()).isEqualTo(Status.INACTIVE);
  }
}
