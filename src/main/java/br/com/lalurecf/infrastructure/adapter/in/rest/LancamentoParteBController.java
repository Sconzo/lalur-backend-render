package br.com.lalurecf.infrastructure.adapter.in.rest;

import br.com.lalurecf.application.port.in.lancamentoparteb.CreateLancamentoParteBUseCase;
import br.com.lalurecf.application.port.in.lancamentoparteb.GetLancamentoParteBUseCase;
import br.com.lalurecf.application.port.in.lancamentoparteb.ImportLancamentoParteBUseCase;
import br.com.lalurecf.application.port.in.lancamentoparteb.ListLancamentoParteBUseCase;
import br.com.lalurecf.application.port.in.lancamentoparteb.ToggleLancamentoParteBStatusUseCase;
import br.com.lalurecf.application.port.in.lancamentoparteb.UpdateLancamentoParteBUseCase;
import br.com.lalurecf.domain.enums.TipoAjuste;
import br.com.lalurecf.domain.enums.TipoApuracao;
import br.com.lalurecf.domain.enums.TipoRelacionamento;
import br.com.lalurecf.infrastructure.dto.importschema.ImportFieldSchema;
import br.com.lalurecf.infrastructure.dto.importschema.ImportSchemaResponse;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.CreateLancamentoParteBRequest;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.ImportLancamentoParteBResponse;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.LancamentoParteBResponse;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.UpdateLancamentoParteBRequest;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.security.CompanyContext;
import br.com.lalurecf.infrastructure.security.FiscalYearContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * Controller para gerenciamento de Lançamentos da Parte B (e-Lalur/e-Lacs).
 *
 * <p>Todos endpoints requerem role CONTADOR e header X-Company-Id (contexto de empresa).
 */
@RestController
@RequestMapping("/lancamento-parte-b")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Lançamentos Parte B",
    description = "Gerenciamento de lançamentos da Parte B (e-Lalur/e-Lacs)")
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LancamentoParteBController {

  private final CreateLancamentoParteBUseCase createLancamentoParteBUseCase;
  private final ListLancamentoParteBUseCase listLancamentoParteBUseCase;
  private final GetLancamentoParteBUseCase getLancamentoParteBUseCase;
  private final UpdateLancamentoParteBUseCase updateLancamentoParteBUseCase;
  private final ToggleLancamentoParteBStatusUseCase toggleLancamentoParteBStatusUseCase;
  private final ImportLancamentoParteBUseCase importLancamentoParteBUseCase;

  /**
   * Importa lançamentos da Parte B via arquivo CSV/TXT em lote.
   *
   * <p>Formato esperado (10 colunas):
   * mesReferencia;anoReferencia;tipoApuracao;tipoRelacionamento;contaContabilCode;
   * contaParteBCode;parametroTributarioCodigo;tipoAjuste;descricao;valor
   *
   * <p>Separador: auto-detectado (; ou ,)
   *
   * <p>Validações:
   *
   * <ul>
   *   <li>tipoApuracao: IRPJ | CSLL
   *   <li>tipoRelacionamento: CONTA_CONTABIL | CONTA_PARTE_B | AMBOS
   *   <li>tipoAjuste: ADICAO | EXCLUSAO
   *   <li>contaContabilCode/contaParteBCode condicionais ao tipoRelacionamento
   *   <li>parametroTributarioCodigo deve existir e estar ACTIVE
   *   <li>valor > 0
   * </ul>
   *
   * @param file arquivo CSV/TXT (max 50MB)
   * @param dryRun se true, apenas retorna preview sem persistir (default: false)
   * @return relatório da importação
   */
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Importar lançamentos Parte B via CSV",
      description =
          "Importa lançamentos da Parte B em lote via arquivo CSV/TXT. "
              + "Requer header X-Company-Id. "
              + "Formato: mesReferencia;anoReferencia;tipoApuracao;tipoRelacionamento;"
              + "contaContabilCode;contaParteBCode;parametroTributarioCodigo;"
              + "tipoAjuste;descricao;valor")
  public ResponseEntity<ImportLancamentoParteBResponse> importLancamentos(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "dryRun", required = false, defaultValue = "false") boolean dryRun) {

    log.info(
        "POST /api/v1/lancamento-parte-b/import - dryRun: {}, file: {}",
        dryRun,
        file.getOriginalFilename());

    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    ImportLancamentoParteBResponse response =
        importLancamentoParteBUseCase.importLancamentos(file, companyId, dryRun);

    return ResponseEntity.ok(response);
  }

  /**
   * Cria um novo lançamento da Parte B.
   *
   * @param request dados do lançamento a ser criado
   * @return lançamento criado
   */
  @PostMapping
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Criar lançamento Parte B",
      description =
          "Cria um novo lançamento fiscal da Parte B (e-Lalur/e-Lacs) "
              + "para a empresa no contexto (header X-Company-Id)")
  public ResponseEntity<LancamentoParteBResponse> createLancamentoParteB(
      @Valid @RequestBody CreateLancamentoParteBRequest request) {
    LancamentoParteBResponse response =
        createLancamentoParteBUseCase.createLancamentoParteB(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Lista lançamentos da Parte B com paginação e filtros.
   *
   * @param mesReferencia filtro por mês de referência (opcional)
   * @param tipoApuracao filtro por tipo de apuração (opcional)
   * @param tipoAjuste filtro por tipo de ajuste (opcional)
   * @param includeInactive se deve incluir lançamentos inativos
   * @param pageable configuração de paginação
   * @return página de lançamentos Parte B
   */
  @GetMapping
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Listar lançamentos Parte B",
      description =
          "Lista lançamentos da Parte B da empresa no contexto com filtros e paginação "
              + "(header X-Company-Id)")
  public ResponseEntity<Page<LancamentoParteBResponse>> listLancamentosParteB(
      @RequestParam(name = "mes_referencia", required = false) Integer mesReferencia,
      @RequestParam(name = "tipo_apuracao", required = false) TipoApuracao tipoApuracao,
      @RequestParam(name = "tipo_ajuste", required = false) TipoAjuste tipoAjuste,
      @RequestParam(name = "include_inactive", required = false, defaultValue = "false")
          Boolean includeInactive,
      @PageableDefault(size = 100, sort = "anoReferencia", direction = Sort.Direction.DESC)
          Pageable pageable) {

    // Obter ano fiscal do contexto (header X-Fiscal-Year)
    Integer anoReferencia = FiscalYearContext.getCurrentFiscalYear();

    Page<LancamentoParteBResponse> response =
        listLancamentoParteBUseCase.listLancamentosParteB(
            anoReferencia, mesReferencia, tipoApuracao, tipoAjuste, includeInactive, pageable);
    return ResponseEntity.ok(response);
  }

  /**
   * Obtém lançamento da Parte B por ID.
   *
   * @param id ID do lançamento
   * @return dados do lançamento
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Obter lançamento Parte B",
      description = "Obtém lançamento da Parte B por ID (header X-Company-Id)")
  public ResponseEntity<LancamentoParteBResponse> getLancamentoParteBById(@PathVariable Long id) {
    LancamentoParteBResponse response = getLancamentoParteBUseCase.getLancamentoParteBById(id);
    return ResponseEntity.ok(response);
  }

  /**
   * Atualiza dados de um lançamento da Parte B.
   *
   * @param id ID do lançamento
   * @param request dados atualizados
   * @return lançamento atualizado
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Atualizar lançamento Parte B",
      description = "Atualiza lançamento da Parte B. Requer header X-Company-Id")
  public ResponseEntity<LancamentoParteBResponse> updateLancamentoParteB(
      @PathVariable Long id, @Valid @RequestBody UpdateLancamentoParteBRequest request) {
    LancamentoParteBResponse response =
        updateLancamentoParteBUseCase.updateLancamentoParteB(id, request);
    return ResponseEntity.ok(response);
  }

  /**
   * Altera status de um lançamento da Parte B.
   *
   * @param id ID do lançamento
   * @param request novo status
   * @return resposta com novo status
   */
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Alternar status",
      description = "Altera status do lançamento entre ACTIVE e INACTIVE (header X-Company-Id)")
  public ResponseEntity<ToggleStatusResponse> toggleStatus(
      @PathVariable Long id, @Valid @RequestBody ToggleStatusRequest request) {
    ToggleStatusResponse response = toggleLancamentoParteBStatusUseCase.toggleStatus(id, request);
    return ResponseEntity.ok(response);
  }

  /**
   * Retorna o schema do arquivo CSV de importação de lançamentos da Parte B em formato CSV
   * (para visualização em Excel).
   */
  @GetMapping("/import-schema")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<byte[]> importSchema() {
    List<String> tipoApuracaoValues = Arrays.stream(TipoApuracao.values())
        .map(Enum::name).toList();
    List<String> tipoRelacionamentoValues = Arrays.stream(TipoRelacionamento.values())
        .map(Enum::name).toList();
    List<String> tipoAjusteValues = Arrays.stream(TipoAjuste.values())
        .map(Enum::name).toList();

    List<ImportFieldSchema> fields = List.of(
        new ImportFieldSchema("mesReferencia", "Integer", true, "1 a 12", null, null, null, "3"),
        new ImportFieldSchema("tipoApuracao", "Enum", true, null, tipoApuracaoValues, null, null,
            "IRPJ"),
        new ImportFieldSchema("tipoRelacionamento", "Enum", true, null, tipoRelacionamentoValues,
            null, null, "CONTA_CONTABIL"),
        new ImportFieldSchema("contaContabilCode", "String", false, null, null,
            "Obrigatório quando tipoRelacionamento = CONTA_CONTABIL ou AMBOS", null,
            "1.1.01.001"),
        new ImportFieldSchema("contaParteBCode", "String", false, null, null,
            "Obrigatório quando tipoRelacionamento = CONTA_PARTE_B ou AMBOS", null, "P001"),
        new ImportFieldSchema("parametroTributarioCodigo", "String", true, null, null,
            "Deve existir e estar ACTIVE no sistema", null, "PARAM-001"),
        new ImportFieldSchema("tipoAjuste", "Enum", true, null, tipoAjusteValues, null, null,
            "ADICAO"),
        new ImportFieldSchema("descricao", "String", true, null, null, null, 2000,
            "Ajuste de depreciação acelerada"),
        new ImportFieldSchema("valor", "Decimal", true,
            "Ponto como separador decimal. Deve ser maior que zero", null, null, null, "5000.00")
    );
    ImportSchemaResponse schema = new ImportSchemaResponse(fields);
    byte[] bytes = schema.toCsv().getBytes(StandardCharsets.UTF_8);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
    headers.setContentDispositionFormData("attachment", "schema-lancamento-parte-b.csv");
    return ResponseEntity.ok().headers(headers).body(bytes);
  }

  /**
   * Retorna um arquivo CSV de template para importação de lançamentos da Parte B.
   */
  @GetMapping("/import-template")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<byte[]> importTemplate() {
    String csv =
        "mesReferencia;tipoApuracao;tipoRelacionamento;"
        + "contaContabilCode;contaParteBCode;parametroTributarioCodigo;"
        + "tipoAjuste;descricao;valor\n"
        + "3;IRPJ;CONTA_CONTABIL;1.1.01.001;;PARAM-001;"
        + "ADICAO;Ajuste de depreciação acelerada;5000.00\n";
    byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
    headers.setContentDispositionFormData("attachment", "template-lancamento-parte-b.csv");
    return ResponseEntity.ok().headers(headers).body(bytes);
  }
}
