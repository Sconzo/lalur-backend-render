package br.com.lalurecf.infrastructure.adapter.in.rest;

import br.com.lalurecf.application.port.in.planodecontas.CreatePlanoDeContasUseCase;
import br.com.lalurecf.application.port.in.planodecontas.GetPlanoDeContasUseCase;
import br.com.lalurecf.application.port.in.planodecontas.ImportPlanoDeContasUseCase;
import br.com.lalurecf.application.port.in.planodecontas.ListPlanoDeContasUseCase;
import br.com.lalurecf.application.port.in.planodecontas.TogglePlanoDeContasStatusUseCase;
import br.com.lalurecf.application.port.in.planodecontas.UpdatePlanoDeContasUseCase;
import br.com.lalurecf.domain.enums.AccountType;
import br.com.lalurecf.domain.enums.ClasseContabil;
import br.com.lalurecf.domain.enums.NaturezaConta;
import br.com.lalurecf.infrastructure.dto.importschema.ImportFieldSchema;
import br.com.lalurecf.infrastructure.dto.importschema.ImportSchemaResponse;
import br.com.lalurecf.infrastructure.dto.planodecontas.CreatePlanoDeContasRequest;
import br.com.lalurecf.infrastructure.dto.planodecontas.ImportPlanoDeContasResponse;
import br.com.lalurecf.infrastructure.dto.planodecontas.PlanoDeContasResponse;
import br.com.lalurecf.infrastructure.dto.planodecontas.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.planodecontas.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.dto.planodecontas.UpdatePlanoDeContasRequest;
import br.com.lalurecf.infrastructure.security.CompanyContext;
import br.com.lalurecf.infrastructure.security.FiscalYearContext;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller para gerenciamento de Plano de Contas (PlanoDeContas).
 *
 * <p>Endpoints para CRUD de contas contábeis com validações ECF e vinculação a Conta Referencial
 * RFB.
 *
 * <p>Todos endpoints requerem autenticação como CONTADOR e header X-Company-Id.
 */
@RestController
@RequestMapping("/plano-de-contas")
@RequiredArgsConstructor
@Slf4j
public class PlanoDeContasController {

  private final CreatePlanoDeContasUseCase createPlanoDeContasUseCase;
  private final ListPlanoDeContasUseCase listPlanoDeContasUseCase;
  private final GetPlanoDeContasUseCase getPlanoDeContasUseCase;
  private final UpdatePlanoDeContasUseCase updatePlanoDeContasUseCase;
  private final TogglePlanoDeContasStatusUseCase togglePlanoDeContasStatusUseCase;
  private final ImportPlanoDeContasUseCase importPlanoDeContasUseCase;

  /**
   * Cria uma nova conta contábil.
   *
   * @param request dados da conta a criar
   * @return conta criada
   */
  @PostMapping
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<PlanoDeContasResponse> create(
      @Valid @RequestBody CreatePlanoDeContasRequest request) {
    log.info("POST /api/v1/plano-de-contas - Creating plano de contas");
    PlanoDeContasResponse response = createPlanoDeContasUseCase.execute(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Importa plano de contas via arquivo CSV/TXT.
   *
   * <p>Formato esperado:
   * code;name;accountType;contaReferencialCodigo;classe;nivel;natureza;afetaResultado;dedutivel
   *
   * <p>Separador: ; ou , (detectado automaticamente)
   *
   * <p>Validações:
   *
   * <ul>
   *   <li>contaReferencialCodigo deve existir e estar ACTIVE
   *   <li>nivel deve estar entre 1 e 5
   *   <li>Combinação (company + code + fiscalYear) deve ser única
   * </ul>
   *
   * @param file arquivo CSV/TXT (max 10MB)
   * @param dryRun se true, apenas retorna preview sem persistir (default: false)
   * @return relatório da importação
   */
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<ImportPlanoDeContasResponse> importPlanoDeContas(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "dryRun", required = false, defaultValue = "false") boolean dryRun) {

    // Obter empresa do contexto
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    // Obter ano fiscal do contexto (header X-Fiscal-Year)
    Integer fiscalYear = FiscalYearContext.getCurrentFiscalYear();
    if (fiscalYear == null) {
      throw new IllegalArgumentException(
          "Fiscal year context is required (header X-Fiscal-Year missing)");
    }

    log.info(
        "POST /api/v1/plano-de-contas/import - fiscalYear: {}, dryRun: {}, file: {}",
        fiscalYear,
        dryRun,
        file.getOriginalFilename());

    // Executar importação
    ImportPlanoDeContasResponse response =
        importPlanoDeContasUseCase.importPlanoDeContas(file, companyId, fiscalYear, dryRun);

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  /**
   * Lista contas contábeis com filtros e paginação.
   *
   * @param accountType filtro por tipo de conta (opcional)
   * @param classe filtro por classe contábil (opcional)
   * @param natureza filtro por natureza (opcional)
   * @param search busca em code e name (opcional)
   * @param includeInactive incluir contas inativas (default: false)
   * @param leafOnly se true, retorna apenas contas do último nível (default: false)
   * @param pageable configuração de paginação
   * @return página de contas
   */
  @GetMapping
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<Page<PlanoDeContasResponse>> list(
      @RequestParam(required = false) AccountType accountType,
      @RequestParam(required = false) ClasseContabil classe,
      @RequestParam(required = false) NaturezaConta natureza,
      @RequestParam(required = false) String search,
      @RequestParam(required = false, defaultValue = "false") Boolean includeInactive,
      @RequestParam(required = false, defaultValue = "false") Boolean leafOnly,
      @PageableDefault(size = 100, sort = "code", direction = Sort.Direction.ASC)
          Pageable pageable) {
    log.info("GET /api/v1/plano-de-contas - Listing plano de contas, leafOnly={}", leafOnly);

    // Obter ano fiscal do contexto (header X-Fiscal-Year)
    Integer fiscalYear = FiscalYearContext.getCurrentFiscalYear();

    Page<PlanoDeContasResponse> response =
        listPlanoDeContasUseCase.execute(
            fiscalYear, accountType, classe, natureza, search, includeInactive, leafOnly,
            pageable);
    return ResponseEntity.ok(response);
  }

  /**
   * Busca conta contábil por ID.
   *
   * @param id ID da conta
   * @return conta encontrada
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<PlanoDeContasResponse> getById(@PathVariable Long id) {
    log.info("GET /api/v1/plano-de-contas/{} - Getting plano de contas", id);
    PlanoDeContasResponse response = getPlanoDeContasUseCase.execute(id);
    return ResponseEntity.ok(response);
  }

  /**
   * Atualiza conta contábil existente.
   *
   * <p>Não permite editar code e fiscalYear (campos imutáveis).
   *
   * @param id ID da conta a atualizar
   * @param request novos dados da conta
   * @return conta atualizada
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<PlanoDeContasResponse> update(
      @PathVariable Long id, @Valid @RequestBody UpdatePlanoDeContasRequest request) {
    log.info("PUT /api/v1/plano-de-contas/{} - Updating plano de contas", id);
    PlanoDeContasResponse response = updatePlanoDeContasUseCase.execute(id, request);
    return ResponseEntity.ok(response);
  }

  /**
   * Alterna status de conta contábil (ACTIVE/INACTIVE).
   *
   * @param id ID da conta
   * @param request novo status desejado
   * @return confirmação da operação
   */
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<ToggleStatusResponse> toggleStatus(
      @PathVariable Long id, @Valid @RequestBody ToggleStatusRequest request) {
    log.info("PATCH /api/v1/plano-de-contas/{}/status - Toggling status", id);
    ToggleStatusResponse response = togglePlanoDeContasStatusUseCase.execute(id, request);
    return ResponseEntity.ok(response);
  }

  /**
   * Retorna o schema do arquivo CSV de importação de plano de contas em formato CSV
   * (para visualização em Excel).
   */
  @GetMapping("/import-schema")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<byte[]> importSchema() {
    List<String> accountTypeValues = Arrays.stream(AccountType.values())
        .map(Enum::name).toList();
    List<String> classeValues = Arrays.stream(ClasseContabil.values())
        .map(Enum::name).toList();
    List<String> naturezaValues = Arrays.stream(NaturezaConta.values())
        .map(Enum::name).toList();

    List<ImportFieldSchema> fields = List.of(
        new ImportFieldSchema("code", "String", true, null, null,
            "Deve seguir a máscara de níveis configurada na empresa", null, "1.1.01.001"),
        new ImportFieldSchema("name", "String", true, null, null, null, null, "Caixa"),
        new ImportFieldSchema("accountType", "Enum", true, null, accountTypeValues, null, null,
            "ATIVO"),
        new ImportFieldSchema("contaReferencialCodigo", "String", false, null, null,
            "Código RFB da Conta Referencial. Deve existir e estar ACTIVE", null,
            "1.01.01.01.01"),
        new ImportFieldSchema("classe", "Enum", true, null, classeValues, null, null,
            "ANALITICO"),
        new ImportFieldSchema("natureza", "Enum", true, null, naturezaValues, null, null,
            "DEVEDORA"),
        new ImportFieldSchema("afetaResultado", "Boolean", true, null,
            ImportFieldSchema.BOOLEAN_ALLOWED_VALUES, null, null, "false"),
        new ImportFieldSchema("dedutivel", "Boolean", true, null,
            ImportFieldSchema.BOOLEAN_ALLOWED_VALUES, null, null, "false")
    );
    ImportSchemaResponse schema = new ImportSchemaResponse(fields);
    byte[] bytes = schema.toCsv().getBytes(StandardCharsets.UTF_8);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
    headers.setContentDispositionFormData("attachment", "schema-plano-de-contas.csv");
    return ResponseEntity.ok().headers(headers).body(bytes);
  }

  /**
   * Retorna um arquivo CSV de template para importação de plano de contas.
   */
  @GetMapping("/import-template")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<byte[]> importTemplate() {
    String csv =
        "code;name;accountType;contaReferencialCodigo;classe;natureza;afetaResultado;dedutivel\n"
        + "1.1.01.001;Caixa;ATIVO;1.01.01.01.01;ANALITICO;DEVEDORA;false;false\n";
    byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
    headers.setContentDispositionFormData("attachment", "template-plano-de-contas.csv");
    return ResponseEntity.ok().headers(headers).body(bytes);
  }
}
