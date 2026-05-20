package br.com.lalurecf.infrastructure.adapter.in.rest;

import br.com.lalurecf.application.port.in.ExportLancamentoContabilUseCase;
import br.com.lalurecf.application.port.in.ImportLancamentoContabilUseCase;
import br.com.lalurecf.application.port.in.lancamentocontabil.CreateLancamentoContabilUseCase;
import br.com.lalurecf.application.port.in.lancamentocontabil.DeleteLancamentoContabilBatchUseCase;
import br.com.lalurecf.application.port.in.lancamentocontabil.GetLancamentoContabilUseCase;
import br.com.lalurecf.application.port.in.lancamentocontabil.ListLancamentoContabilUseCase;
import br.com.lalurecf.application.port.in.lancamentocontabil.ToggleLancamentoContabilStatusUseCase;
import br.com.lalurecf.application.port.in.lancamentocontabil.UpdateLancamentoContabilUseCase;
import br.com.lalurecf.domain.model.LancamentoContabil;
import br.com.lalurecf.infrastructure.dto.importschema.ImportFieldSchema;
import br.com.lalurecf.infrastructure.dto.importschema.ImportSchemaResponse;
import br.com.lalurecf.infrastructure.dto.lancamentocontabil.CreateLancamentoContabilRequest;
import br.com.lalurecf.infrastructure.dto.lancamentocontabil.DeleteLancamentoContabilBatchResponse;
import br.com.lalurecf.infrastructure.dto.lancamentocontabil.ImportLancamentoContabilResponse;
import br.com.lalurecf.infrastructure.dto.lancamentocontabil.LancamentoContabilResponse;
import br.com.lalurecf.infrastructure.dto.lancamentocontabil.UpdateLancamentoContabilRequest;
import br.com.lalurecf.infrastructure.dto.mapper.LancamentoContabilDtoMapper;
import br.com.lalurecf.infrastructure.dto.planodecontas.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.planodecontas.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.security.CompanyContext;
import br.com.lalurecf.infrastructure.security.FiscalYearContext;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * REST Controller para gerenciamento de Lançamentos Contábeis.
 *
 * <p>Endpoints para importação e CRUD de lançamentos contábeis com partidas dobradas.
 *
 * <p>Todos endpoints requerem autenticação como CONTADOR e header X-Company-Id.
 */
@RestController
@RequestMapping("/lancamento-contabil")
@RequiredArgsConstructor
@Slf4j
public class LancamentoContabilController {

  private final ImportLancamentoContabilUseCase importLancamentoContabilUseCase;
  private final ExportLancamentoContabilUseCase exportLancamentoContabilUseCase;
  private final CreateLancamentoContabilUseCase createLancamentoContabilUseCase;
  private final ListLancamentoContabilUseCase listLancamentoContabilUseCase;
  private final GetLancamentoContabilUseCase getLancamentoContabilUseCase;
  private final UpdateLancamentoContabilUseCase updateLancamentoContabilUseCase;
  private final ToggleLancamentoContabilStatusUseCase toggleLancamentoContabilStatusUseCase;
  private final DeleteLancamentoContabilBatchUseCase deleteLancamentoContabilBatchUseCase;
  private final LancamentoContabilDtoMapper lancamentoContabilDtoMapper;

  /**
   * Importa lançamentos contábeis via arquivo CSV/TXT.
   *
   * <p>Formato esperado: contaDebitoCode;contaCreditoCode;data;valor;historico;numeroDocumento
   *
   * <p>Separador: auto-detectado (; ou ,)
   *
   * <p>Validações:
   *
   * <ul>
   *   <li>Contas devem existir no plano de contas da empresa/ano
   *   <li>Débito != Crédito
   *   <li>Data >= Período Contábil
   *   <li>Valor > 0
   * </ul>
   *
   * @param file arquivo CSV/TXT (max 50MB)
   * @param dryRun se true, apenas retorna preview sem persistir (default: false)
   * @return relatório da importação
   */
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<ImportLancamentoContabilResponse> importLancamentos(
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
        "POST /api/v1/lancamento-contabil/import - fiscalYear: {}, dryRun: {}, file: {}",
        fiscalYear,
        dryRun,
        file.getOriginalFilename());

    // Executar importação
    ImportLancamentoContabilResponse response =
        importLancamentoContabilUseCase.importLancamentos(file, companyId, fiscalYear, dryRun);

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  /**
   * Exporta lançamentos contábeis para arquivo CSV.
   *
   * <p>Formato gerado: contaDebitoCode;contaDebitoName;contaCreditoCode;contaCreditoName;
   * data;valor;historico;numeroDocumento
   *
   * <p>Separador: ; (ponto e vírgula)
   *
   * <p>Ordenação: data ASC
   *
   * @param dataInicio data inicial do filtro (opcional)
   * @param dataFim data final do filtro (opcional)
   * @return arquivo CSV para download
   */
  @GetMapping("/export")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<byte[]> exportLancamentos(
      @RequestParam(value = "dataInicio", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataInicio,
      @RequestParam(value = "dataFim", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataFim) {

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
        "GET /api/v1/lancamento-contabil/export - fiscalYear: {}, dataInicio: {}, dataFim: {}",
        fiscalYear,
        dataInicio,
        dataFim);

    // Executar exportação
    String csvContent =
        exportLancamentoContabilUseCase.exportLancamentos(
            companyId, fiscalYear, dataInicio, dataFim);

    // Preparar response com arquivo CSV
    String filename =
        String.format("lancamentos-contabeis-%d-%d.csv", companyId, fiscalYear);
    byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
    headers.setContentDispositionFormData("attachment", filename);
    headers.setContentLength(csvBytes.length);

    return ResponseEntity.ok().headers(headers).body(csvBytes);
  }

  /**
   * Cria um novo lançamento contábil manual com partidas dobradas.
   *
   * <p>Validações:
   *
   * <ul>
   *   <li>Conta débito != Conta crédito
   *   <li>Data >= Período Contábil da empresa
   *   <li>Valor > 0
   *   <li>Contas devem pertencer à empresa no contexto
   * </ul>
   *
   * @param request dados do lançamento
   * @return lançamento criado
   */
  @PostMapping
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<LancamentoContabilResponse> create(
      @Valid @RequestBody CreateLancamentoContabilRequest request) {

    log.info("POST /api/v1/lancamento-contabil - creating lancamento");

    // Obter ano fiscal do contexto (header X-Fiscal-Year)
    Integer fiscalYear = FiscalYearContext.getCurrentFiscalYear();
    if (fiscalYear == null) {
      throw new IllegalArgumentException(
          "Fiscal year context is required (header X-Fiscal-Year missing)");
    }

    // Converter DTO para domain
    LancamentoContabil lancamento =
        LancamentoContabil.builder()
            .contaDebitoId(request.getContaDebitoId())
            .contaCreditoId(request.getContaCreditoId())
            .data(request.getData())
            .valor(request.getValor())
            .historico(request.getHistorico())
            .numeroDocumento(request.getNumeroDocumento())
            .fiscalYear(fiscalYear)
            .build();

    // Criar
    LancamentoContabil created = createLancamentoContabilUseCase.create(lancamento);

    // Converter para DTO
    LancamentoContabilResponse response = lancamentoContabilDtoMapper.toResponse(created);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Lista lançamentos contábeis da empresa com filtros e paginação.
   *
   * <p>Filtros disponíveis:
   *
   * <ul>
   *   <li>contaDebitoId - filtrar por conta de débito
   *   <li>contaCreditoId - filtrar por conta de crédito
   *   <li>data - filtrar por data específica
   *   <li>dataInicio / dataFim - filtrar por range de data
   *   <li>fiscalYear - filtrar por ano fiscal
   *   <li>includeInactive - incluir inativos (default: false)
   * </ul>
   *
   * @param contaDebitoId filtro por conta débito (opcional)
   * @param contaCreditoId filtro por conta crédito (opcional)
   * @param data filtro por data (opcional)
   * @param dataInicio filtro por range - início (opcional)
   * @param dataFim filtro por range - fim (opcional)
   * @param includeInactive incluir inativos (opcional)
   * @param pageable configuração de paginação
   * @return página de lançamentos
   */
  @GetMapping
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<Page<LancamentoContabilResponse>> list(
      @RequestParam(value = "contaDebitoId", required = false) Long contaDebitoId,
      @RequestParam(value = "contaCreditoId", required = false) Long contaCreditoId,
      @RequestParam(value = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate data,
      @RequestParam(value = "dataInicio", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataInicio,
      @RequestParam(value = "dataFim", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataFim,
      @RequestParam(value = "includeInactive", required = false) Boolean includeInactive,
      @PageableDefault(size = 100, sort = "data", direction = Sort.Direction.DESC)
          Pageable pageable) {

    log.info("GET /api/v1/lancamento-contabil - listing lancamentos");

    // Obter ano fiscal do contexto (header X-Fiscal-Year)
    Integer fiscalYear = FiscalYearContext.getCurrentFiscalYear();

    // Listar
    Page<LancamentoContabil> lancamentos =
        listLancamentoContabilUseCase.list(
            contaDebitoId,
            contaCreditoId,
            data,
            dataInicio,
            dataFim,
            fiscalYear,
            includeInactive,
            pageable);

    // Converter para DTO
    Page<LancamentoContabilResponse> response =
        lancamentos.map(lancamentoContabilDtoMapper::toResponse);

    return ResponseEntity.ok(response);
  }

  /**
   * Busca lançamento contábil por ID.
   *
   * @param id ID do lançamento
   * @return lançamento encontrado
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<LancamentoContabilResponse> getById(@PathVariable Long id) {

    log.info("GET /api/v1/lancamento-contabil/{} - getting lancamento by id", id);

    // Buscar
    LancamentoContabil lancamento = getLancamentoContabilUseCase.getById(id);

    // Converter para DTO
    LancamentoContabilResponse response = lancamentoContabilDtoMapper.toResponse(lancamento);

    return ResponseEntity.ok(response);
  }

  /**
   * Atualiza lançamento contábil existente.
   *
   * <p>Validações:
   *
   * <ul>
   *   <li>Período Contábil: data original >= company.periodoContabil
   *   <li>Período Contábil: nova data >= company.periodoContabil
   *   <li>Conta débito != Conta crédito
   *   <li>Valor > 0
   * </ul>
   *
   * @param id ID do lançamento
   * @param request dados atualizados
   * @return lançamento atualizado
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<LancamentoContabilResponse> update(
      @PathVariable Long id, @Valid @RequestBody UpdateLancamentoContabilRequest request) {

    log.info("PUT /api/v1/lancamento-contabil/{} - updating lancamento", id);

    // Converter DTO para domain
    LancamentoContabil lancamento =
        LancamentoContabil.builder()
            .contaDebitoId(request.getContaDebitoId())
            .contaCreditoId(request.getContaCreditoId())
            .data(request.getData())
            .valor(request.getValor())
            .historico(request.getHistorico())
            .numeroDocumento(request.getNumeroDocumento())
            .build();

    // Atualizar
    LancamentoContabil updated = updateLancamentoContabilUseCase.update(id, lancamento);

    // Converter para DTO
    LancamentoContabilResponse response = lancamentoContabilDtoMapper.toResponse(updated);

    return ResponseEntity.ok(response);
  }

  /**
   * Alterna status do lançamento contábil (ACTIVE ↔ INACTIVE).
   *
   * <p>Validação: data >= company.periodoContabil (não pode alterar lançamentos de período
   * fechado).
   *
   * @param id ID do lançamento
   * @param request requisição de toggle (opcional, pode ser null)
   * @return lançamento com status atualizado
   */
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<ToggleStatusResponse> toggleStatus(
      @PathVariable Long id, @RequestBody(required = false) ToggleStatusRequest request) {

    log.info("PATCH /api/v1/lancamento-contabil/{}/status - toggling status", id);

    // Toggle status
    LancamentoContabil updated = toggleLancamentoContabilStatusUseCase.toggleStatus(id);

    // Converter para DTO
    ToggleStatusResponse response =
        ToggleStatusResponse.builder()
            .success(true)
            .newStatus(updated.getStatus())
            .message("Status toggled successfully")
            .build();

    return ResponseEntity.ok(response);
  }

  /**
   * Deleta em lote todos os lançamentos contábeis de um mês e ano específicos.
   *
   * <p>A deleção é física (hard delete) e irreversível.
   *
   * @param mes mês (1-12)
   * @param ano ano (ex: 2024)
   * @return quantidade de registros deletados
   */
  @DeleteMapping("/batch")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<DeleteLancamentoContabilBatchResponse> deleteBatch(
      @RequestParam("mes") Integer mes, @RequestParam("ano") Integer ano) {

    log.info("DELETE /api/v1/lancamento-contabil/batch - mes: {}, ano: {}", mes, ano);

    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    DeleteLancamentoContabilBatchResponse response =
        deleteLancamentoContabilBatchUseCase.deleteBatch(companyId, mes, ano);

    return ResponseEntity.ok(response);
  }

  /**
   * Retorna o schema do arquivo CSV de importação de lançamentos contábeis em formato CSV
   * (para visualização em Excel).
   */
  @GetMapping("/import-schema")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<byte[]> importSchema() {
    List<ImportFieldSchema> fields = List.of(
        new ImportFieldSchema("contaDebitoCode", "String", false, null, null,
            "Ao menos contaDebitoCode ou contaCreditoCode deve ser informado", null,
            "1.1.01.001"),
        new ImportFieldSchema("contaCreditoCode", "String", false, null, null,
            "Ao menos contaDebitoCode ou contaCreditoCode deve ser informado", null,
            "3.1.01.001"),
        new ImportFieldSchema("data", "Date", true, "YYYY-MM-DD", null, null, null,
            "2024-03-15"),
        new ImportFieldSchema("valor", "Decimal", true,
            "Ponto como separador decimal. Deve ser maior que zero", null, null, null,
            "1500.00"),
        new ImportFieldSchema("historico", "String", true, null, null, null, 2000,
            "Pagamento de fornecedor"),
        new ImportFieldSchema("numeroDocumento", "String", false, null, null, null, 100,
            "NF-001234")
    );
    ImportSchemaResponse schema = new ImportSchemaResponse(fields);
    byte[] bytes = schema.toCsv().getBytes(StandardCharsets.UTF_8);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
    headers.setContentDispositionFormData("attachment", "schema-lancamento-contabil.csv");
    return ResponseEntity.ok().headers(headers).body(bytes);
  }

  /**
   * Retorna um arquivo CSV de template para importação de lançamentos contábeis.
   */
  @GetMapping("/import-template")
  @PreAuthorize("hasRole('CONTADOR')")
  public ResponseEntity<byte[]> importTemplate() {
    String csv =
        "contaDebitoCode;contaCreditoCode;data;valor;historico;numeroDocumento\n"
        + "1.1.01.001;3.1.01.001;2024-03-15;1500.00;Pagamento de fornecedor;NF-001234\n";
    byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
    headers.setContentDispositionFormData("attachment", "template-lancamento-contabil.csv");
    return ResponseEntity.ok().headers(headers).body(bytes);
  }
}
