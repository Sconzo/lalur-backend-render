package br.com.lalurecf.infrastructure.adapter.in.rest;

import br.com.lalurecf.application.port.in.company.CreateCompanyUseCase;
import br.com.lalurecf.application.port.in.company.CreateTemporalValueUseCase;
import br.com.lalurecf.application.port.in.company.DeleteTemporalValueUseCase;
import br.com.lalurecf.application.port.in.company.GetCompanyTaxParametersTimelineUseCase;
import br.com.lalurecf.application.port.in.company.GetCompanyUseCase;
import br.com.lalurecf.application.port.in.company.ListCompaniesUseCase;
import br.com.lalurecf.application.port.in.company.ListCompanyTaxParametersUseCase;
import br.com.lalurecf.application.port.in.company.ListTemporalValuesUseCase;
import br.com.lalurecf.application.port.in.company.SelectCompanyUseCase;
import br.com.lalurecf.application.port.in.company.ToggleCompanyStatusUseCase;
import br.com.lalurecf.application.port.in.company.UpdateCompanyTaxParametersUseCase;
import br.com.lalurecf.application.port.in.company.UpdateCompanyUseCase;
import br.com.lalurecf.application.port.out.CnpjData;
import br.com.lalurecf.application.port.out.CnpjSearchPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.CompanyStatus;
import br.com.lalurecf.domain.model.valueobject.CNPJ;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.dto.company.CompanyDetailResponse;
import br.com.lalurecf.infrastructure.dto.company.CompanyListItemResponse;
import br.com.lalurecf.infrastructure.dto.company.CompanyResponse;
import br.com.lalurecf.infrastructure.dto.company.CreateCompanyRequest;
import br.com.lalurecf.infrastructure.dto.company.CreateTemporalValueRequest;
import br.com.lalurecf.infrastructure.dto.company.FilterOptionsResponse;
import br.com.lalurecf.infrastructure.dto.company.PeriodoContabilAuditResponse;
import br.com.lalurecf.infrastructure.dto.company.SelectCompanyRequest;
import br.com.lalurecf.infrastructure.dto.company.SelectCompanyResponse;
import br.com.lalurecf.infrastructure.dto.company.TaxParameterSummary;
import br.com.lalurecf.infrastructure.dto.company.TemporalValueResponse;
import br.com.lalurecf.infrastructure.dto.company.TimelineResponse;
import br.com.lalurecf.infrastructure.dto.company.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.company.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.dto.company.UpdateCompanyRequest;
import br.com.lalurecf.infrastructure.dto.company.UpdatePeriodoContabilRequest;
import br.com.lalurecf.infrastructure.dto.company.UpdatePeriodoContabilResponse;
import br.com.lalurecf.infrastructure.dto.company.UpdateTaxParametersRequestV2;
import br.com.lalurecf.infrastructure.dto.company.UpdateTaxParametersResponse;
import br.com.lalurecf.infrastructure.security.CompanyContext;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

/**
 * Controller REST para operações relacionadas a empresas.
 *
 * <p>Endpoints disponíveis:
 * <ul>
 *   <li>POST /companies - Criar empresa (ADMIN only)
 *   <li>GET /companies - Listar empresas com filtros (ADMIN only)
 *   <li>GET /companies/{id} - Visualizar empresa (ADMIN only)
 *   <li>PUT /companies/{id} - Editar empresa (ADMIN only)
 *   <li>PATCH /companies/{id}/status - Toggle status (ADMIN only)
 *   <li>GET /companies/filter-options/cnpj - Opções de filtro CNPJ (ADMIN only)
 *   <li>GET /companies/filter-options/razao-social - Opções filtro Razão Social (ADMIN only)
 *   <li>GET /search-cnpj/{cnpj} - Busca dados de empresa por CNPJ na BrasilAPI (ADMIN only)
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {

  private final CnpjSearchPort cnpjSearchPort;
  private final CreateCompanyUseCase createCompanyUseCase;
  private final ListCompaniesUseCase listCompaniesUseCase;
  private final GetCompanyUseCase getCompanyUseCase;
  private final UpdateCompanyUseCase updateCompanyUseCase;
  private final ToggleCompanyStatusUseCase toggleCompanyStatusUseCase;
  private final SelectCompanyUseCase selectCompanyUseCase;
  private final CompanyJpaRepository companyRepository;
  private final br.com.lalurecf.application.port.in.company.UpdatePeriodoContabilUseCase
      updatePeriodoContabilUseCase;
  private final br.com.lalurecf.application.port.in.company.GetPeriodoContabilAuditUseCase
      getPeriodoContabilAuditUseCase;
  private final UpdateCompanyTaxParametersUseCase updateCompanyTaxParametersUseCase;
  private final ListCompanyTaxParametersUseCase listCompanyTaxParametersUseCase;
  private final CreateTemporalValueUseCase createTemporalValueUseCase;
  private final ListTemporalValuesUseCase listTemporalValuesUseCase;
  private final DeleteTemporalValueUseCase deleteTemporalValueUseCase;
  private final GetCompanyTaxParametersTimelineUseCase getCompanyTaxParametersTimelineUseCase;

  /**
   * Cria uma nova empresa.
   *
   * @param request dados da empresa a ser criada
   * @return empresa criada com status 201 CREATED
   */
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CompanyDetailResponse> createCompany(
      @Valid @RequestBody CreateCompanyRequest request) {
    log.info("POST /companies - Criando empresa com CNPJ: {}", request.cnpj());
    CompanyDetailResponse response = createCompanyUseCase.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Lista empresas com filtros e paginação.
   *
   * @param strSearch busca global em todos os campos
   * @param cnpjFilters lista de CNPJs para filtro (comparação exata)
   * @param razaoSocialFilters lista de Razões Sociais para filtro (comparação exata)
   * @param includeInactive incluir empresas inativas
   * @param pageable configuração de paginação
   * @return página de empresas
   */
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<CompanyResponse>> listCompanies(
      @RequestParam(required = false) String strSearch,
      @RequestParam(required = false) List<String> cnpjFilters,
      @RequestParam(required = false) List<String> razaoSocialFilters,
      @RequestParam(defaultValue = "false") boolean includeInactive,
      @PageableDefault(size = 20, sort = "razaoSocial") Pageable pageable) {

    log.info("GET /companies - Listando empresas");
    Page<CompanyResponse> response = listCompaniesUseCase.list(
        strSearch, cnpjFilters, razaoSocialFilters, includeInactive, pageable);
    return ResponseEntity.ok(response);
  }

  /**
   * Busca empresa por ID.
   *
   * @param id ID da empresa
   * @return detalhes completos da empresa
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CompanyDetailResponse> getCompany(@PathVariable Long id) {
    log.info("GET /companies/{} - Buscando empresa", id);
    CompanyDetailResponse response = getCompanyUseCase.getById(id);
    return ResponseEntity.ok(response);
  }

  /**
   * Atualiza dados de uma empresa existente.
   *
   * @param id ID da empresa
   * @param request novos dados da empresa
   * @return empresa atualizada
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CompanyDetailResponse> updateCompany(
      @PathVariable Long id,
      @Valid @RequestBody UpdateCompanyRequest request) {

    log.info("PUT /companies/{} - Atualizando empresa", id);
    CompanyDetailResponse response = updateCompanyUseCase.update(id, request);
    return ResponseEntity.ok(response);
  }

  /**
   * Alterna status de uma empresa (ACTIVE ↔ INACTIVE).
   *
   * @param id ID da empresa
   * @param request novo status desejado
   * @return resposta com sucesso e novo status
   */
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ToggleStatusResponse> toggleStatus(
      @PathVariable Long id,
      @Valid @RequestBody ToggleStatusRequest request) {

    log.info("PATCH /companies/{}/status - Alterando status para {}", id, request.status());
    ToggleStatusResponse response = toggleCompanyStatusUseCase.toggleStatus(id, request.status());
    return ResponseEntity.ok(response);
  }

  /**
   * Retorna lista de CNPJs únicos para popular dropdown de filtro.
   *
   * @param search texto de busca (opcional)
   * @param includeInactive incluir empresas inativas
   * @return lista de CNPJs formatados
   */
  @GetMapping("/filter-options/cnpj")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<FilterOptionsResponse> getCnpjFilterOptions(
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "false") boolean includeInactive) {

    log.debug("GET /companies/filter-options/cnpj - search: {}", search);

    Status status = includeInactive ? null : Status.ACTIVE;
    List<String> cnpjs = status != null
        ? companyRepository.findDistinctCnpjsByStatus(status)
        : companyRepository.findDistinctCnpjs();

    // Aplicar filtro de busca se fornecido
    if (search != null && !search.isBlank()) {
      String searchClean = search.replaceAll("[^0-9]", "");
      cnpjs = cnpjs.stream()
          .filter(cnpj -> cnpj.contains(searchClean))
          .limit(100)
          .toList();
    } else {
      cnpjs = cnpjs.stream().limit(100).toList();
    }

    // Formatar CNPJs
    List<String> formatted = cnpjs.stream()
        .map(this::formatCnpj)
        .toList();

    return ResponseEntity.ok(new FilterOptionsResponse(formatted));
  }

  /**
   * Retorna lista de Razões Sociais únicas para popular dropdown de filtro.
   *
   * @param search texto de busca (opcional)
   * @param includeInactive incluir empresas inativas
   * @return lista de Razões Sociais
   */
  @GetMapping("/filter-options/razao-social")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<FilterOptionsResponse> getRazaoSocialFilterOptions(
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "false") boolean includeInactive) {

    log.debug("GET /companies/filter-options/razao-social - search: {}", search);

    Status status = includeInactive ? null : Status.ACTIVE;
    List<String> razoesSociais = status != null
        ? companyRepository.findDistinctRazaoSocialByStatus(status)
        : companyRepository.findDistinctRazaoSocial();

    // Aplicar filtro de busca se fornecido
    if (search != null && !search.isBlank()) {
      String searchLower = search.toLowerCase();
      razoesSociais = razoesSociais.stream()
          .filter(razao -> razao.toLowerCase().contains(searchLower))
          .limit(100)
          .toList();
    } else {
      razoesSociais = razoesSociais.stream().limit(100).toList();
    }

    return ResponseEntity.ok(new FilterOptionsResponse(razoesSociais));
  }

  /**
   * Busca dados de uma empresa por CNPJ em API externa (BrasilAPI).
   *
   * <p>Endpoint protegido - apenas ADMIN pode acessar.
   *
   * <p>Responses:
   * <ul>
   *   <li>200 OK - Dados da empresa encontrados
   *   <li>400 Bad Request - CNPJ com formato inválido
   *   <li>404 Not Found - CNPJ não encontrado ou erro na API externa
   * </ul>
   *
   * @param cnpjStr CNPJ da empresa (14 dígitos, pode conter pontuação)
   * @return ResponseEntity com dados da empresa ou erro
   */
  @GetMapping("/search-cnpj/{cnpj}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CnpjData> searchByCnpj(@PathVariable("cnpj") String cnpjStr) {
    log.info("Recebida requisição de busca de CNPJ: {}", cnpjStr);

    try {
      // Valida formato do CNPJ usando Value Object
      CNPJ cnpj = CNPJ.of(cnpjStr);

      // Busca dados na API externa
      return cnpjSearchPort.searchByCnpj(cnpj.getValue())
          .map(data -> {
            log.info("Dados do CNPJ {} encontrados", cnpjStr);
            return ResponseEntity.ok(data);
          })
          .orElseGet(() -> {
            log.info("CNPJ {} não encontrado ou erro na API externa", cnpjStr);
            return ResponseEntity.notFound().build();
          });

    } catch (IllegalArgumentException e) {
      log.warn("CNPJ inválido recebido: {} - Erro: {}", cnpjStr, e.getMessage());
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Lista empresas disponíveis para o usuário autenticado (CONTADOR e ADMIN).
   * Endpoint para dropdown de seleção de empresa.
   */
  @GetMapping("/my-companies")
  public ResponseEntity<List<CompanyListItemResponse>> getMyCompanies() {
    log.info("Buscando empresas disponíveis para usuário autenticado");

    List<CompanyListItemResponse> companies = companyRepository.findByStatus(Status.ACTIVE)
        .stream()
        .map(entity -> new CompanyListItemResponse(
            entity.getId(),
            formatCnpj(entity.getCnpj()),
            entity.getRazaoSocial()
        ))
        .toList();

    log.info("Retornando {} empresas ativas", companies.size());
    return ResponseEntity.ok(companies);
  }

  /**
   * Seleciona uma empresa para trabalho (CONTADOR e ADMIN).
   * Valida que empresa existe e está ACTIVE.
   */
  @PostMapping("/select-company")
  public ResponseEntity<SelectCompanyResponse> selectCompany(
      @Valid @RequestBody SelectCompanyRequest request) {
    log.info("Requisição de seleção de empresa: companyId={}", request.companyId());

    try {
      br.com.lalurecf.domain.model.Company company =
          selectCompanyUseCase.selectCompany(request.companyId());

      SelectCompanyResponse response = new SelectCompanyResponse(
          true,
          company.getId(),
          company.getRazaoSocial(),
          "Empresa selecionada com sucesso"
      );

      log.info("Empresa selecionada: companyId={}, nome={}",
          company.getId(), company.getRazaoSocial());
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      log.error("Erro ao selecionar empresa: {}", e.getMessage());
      SelectCompanyResponse response = new SelectCompanyResponse(
          false,
          request.companyId(),
          null,
          e.getMessage()
      );
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
  }

  /**
   * Retorna a empresa atualmente selecionada no contexto (via header X-Company-Id).
   * Requer que header X-Company-Id esteja presente.
   */
  @GetMapping("/current-company")
  public ResponseEntity<CompanyListItemResponse> getCurrentCompany() {
    Long companyId = CompanyContext.getCurrentCompanyId();

    if (companyId == null) {
      log.warn("Tentativa de obter empresa atual sem contexto definido");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    log.info("Buscando empresa do contexto atual: companyId={}", companyId);

    return companyRepository.findById(companyId)
        .map(entity -> {
          CompanyListItemResponse response = new CompanyListItemResponse(
              entity.getId(),
              formatCnpj(entity.getCnpj()),
              entity.getRazaoSocial()
          );
          return ResponseEntity.ok(response);
        })
        .orElseGet(() -> {
          log.warn("Empresa do contexto não encontrada: companyId={}", companyId);
          return ResponseEntity.notFound().build();
        });
  }

  /**
   * Atualiza o Período Contábil de uma empresa.
   *
   * <p>Apenas ADMIN pode atualizar.
   * Validações:
   * <ul>
   *   <li>Nova data não pode ser no futuro
   *   <li>Nova data não pode retroagir (deve ser posterior à atual)
   *   <li>Registra alteração em log de auditoria
   * </ul>
   *
   * @param id ID da empresa
   * @param request novo período contábil
   * @return resposta com sucesso e dados anterior/novo
   */
  @PutMapping("/{id}/periodo-contabil")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UpdatePeriodoContabilResponse> updatePeriodoContabil(
      @PathVariable Long id,
      @Valid @RequestBody UpdatePeriodoContabilRequest request) {

    log.info("PUT /companies/{}/periodo-contabil - Atualizando período contábil", id);
    UpdatePeriodoContabilResponse response = updatePeriodoContabilUseCase.update(id, request);
    return ResponseEntity.ok(response);
  }

  /**
   * Retorna histórico de alterações do Período Contábil de uma empresa.
   *
   * <p>Apenas ADMIN pode acessar.
   *
   * @param id ID da empresa
   * @return lista de registros de auditoria ordenada do mais recente ao mais antigo
   */
  @GetMapping("/{id}/periodo-contabil/audit")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<PeriodoContabilAuditResponse>> getPeriodoContabilAudit(
      @PathVariable Long id) {

    log.info("GET /companies/{}/periodo-contabil/audit - Buscando histórico", id);
    List<PeriodoContabilAuditResponse> response =
        getPeriodoContabilAuditUseCase.getAuditHistory(id);
    return ResponseEntity.ok(response);
  }

  /**
   * Atualiza parâmetros tributários de uma empresa (suporta periódicos e globais).
   *
   * <p>Suporta dois tipos de parâmetros tributários:
   * <ul>
   *   <li><b>Globais:</b> Aplicam-se ao ano inteiro (ex: CNAE, Qualificação PJ)
   *   <li><b>Periódicos:</b> Precisam de mês/trimestre específico (mudam durante o ano)
   * </ul>
   *
   * <p>Operação atômica: substitui completamente os parâmetros anteriores.
   *
   * <p>Exemplo de request:
   * <pre>{@code
   * {
   *   "globalParameterIds": [1, 2, 3],
   *   "periodicParameters": [
   *     {
   *       "taxParameterId": 4,
   *       "temporalValues": [
   *         {"ano": 2024, "mes": 1},
   *         {"ano": 2024, "mes": 2}
   *       ]
   *     }
   *   ]
   * }
   * }</pre>
   *
   * @param id ID da empresa
   * @param request request contendo parâmetros globais e periódicos
   * @return resposta com lista atualizada de parâmetros
   */
  @PutMapping("/{id}/tax-parameters")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UpdateTaxParametersResponse> updateTaxParameters(
      @PathVariable Long id,
      @Valid @RequestBody UpdateTaxParametersRequestV2 request) {

    log.info("PUT /companies/{}/tax-parameters - Atualizando parâmetros tributários", id);
    UpdateTaxParametersResponse response =
        updateCompanyTaxParametersUseCase.updateTaxParameters(id, request);
    return ResponseEntity.ok(response);
  }

  /**
   * Lista os parâmetros tributários associados a uma empresa.
   *
   * @param id ID da empresa
   * @return lista de parâmetros tributários
   */
  @GetMapping("/{id}/tax-parameters")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<TaxParameterSummary>> listTaxParameters(@PathVariable Long id) {

    log.info("GET /companies/{}/tax-parameters - Listando parâmetros tributários", id);
    List<TaxParameterSummary> response = listCompanyTaxParametersUseCase.listTaxParameters(id);
    return ResponseEntity.ok(response);
  }

  /**
   * Cria um novo valor temporal para um parâmetro tributário da empresa.
   *
   * <p>Apenas ADMIN pode criar valores temporais.
   * Validações:
   * <ul>
   *   <li>XOR constraint: exatamente UM campo (mes OU trimestre) deve estar preenchido
   *   <li>mes deve estar entre 1-12
   *   <li>trimestre deve estar entre 1-4
   *   <li>Associação empresa-parâmetro deve existir
   * </ul>
   *
   * @param companyId ID da empresa
   * @param taxParameterId ID do parâmetro tributário
   * @param request dados do valor temporal (ano, mes ou trimestre)
   * @return valor temporal criado com status 201 CREATED
   */
  @PostMapping("/{companyId}/tax-parameters/{taxParameterId}/temporal-values")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TemporalValueResponse> createTemporalValue(
      @PathVariable Long companyId,
      @PathVariable Long taxParameterId,
      @Valid @RequestBody CreateTemporalValueRequest request) {

    log.info(
        "POST /companies/{}/tax-parameters/{}/temporal-values - Criando valor temporal",
        companyId,
        taxParameterId);

    TemporalValueResponse response =
        createTemporalValueUseCase.createTemporalValue(companyId, taxParameterId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Lista valores temporais de um parâmetro tributário da empresa.
   *
   * <p>Apenas ADMIN pode listar valores temporais.
   * Pode filtrar por ano específico.
   *
   * @param companyId ID da empresa
   * @param taxParameterId ID do parâmetro tributário
   * @param ano filtro opcional por ano
   * @return lista de valores temporais
   */
  @GetMapping("/{companyId}/tax-parameters/{taxParameterId}/temporal-values")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<TemporalValueResponse>> listTemporalValues(
      @PathVariable Long companyId,
      @PathVariable Long taxParameterId,
      @RequestParam(required = false) Integer ano) {

    log.info(
        "GET /companies/{}/tax-parameters/{}/temporal-values - "
            + "Listando valores temporais (ano: {})",
        companyId,
        taxParameterId,
        ano);

    List<TemporalValueResponse> response =
        listTemporalValuesUseCase.listTemporalValues(companyId, taxParameterId, ano);
    return ResponseEntity.ok(response);
  }

  /**
   * Deleta um valor temporal específico.
   *
   * <p>Apenas ADMIN pode deletar valores temporais.
   *
   * @param companyId ID da empresa
   * @param taxParameterId ID do parâmetro tributário
   * @param valorId ID do valor temporal
   * @return status 204 NO_CONTENT se deletado com sucesso
   */
  @DeleteMapping("/{companyId}/tax-parameters/{taxParameterId}/temporal-values/{valorId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteTemporalValue(
      @PathVariable Long companyId,
      @PathVariable Long taxParameterId,
      @PathVariable Long valorId) {

    log.info(
        "DELETE /companies/{}/tax-parameters/{}/temporal-values/{} - Deletando valor temporal",
        companyId,
        taxParameterId,
        valorId);

    deleteTemporalValueUseCase.deleteTemporalValue(companyId, taxParameterId, valorId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Obtém timeline agregada de parâmetros tributários da empresa.
   *
   * <p>Apenas ADMIN pode acessar timeline.
   * Agrupa parâmetros por tipo e mostra os períodos em que cada um está ativo.
   *
   * @param companyId ID da empresa
   * @param ano ano fiscal para a timeline
   * @return timeline agrupada por tipo de parâmetro
   */
  @GetMapping("/{companyId}/tax-parameters-timeline")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TimelineResponse> getTaxParametersTimeline(
      @PathVariable Long companyId,
      @RequestParam Integer ano) {

    log.info(
        "GET /companies/{}/tax-parameters-timeline?ano={} - Obtendo timeline",
        companyId,
        ano);

    TimelineResponse response =
        getCompanyTaxParametersTimelineUseCase.getTimeline(companyId, ano);
    return ResponseEntity.ok(response);
  }

  /**
   * Formata CNPJ para exibição: 00.000.000/0000-00.
   */
  private String formatCnpj(String cnpj) {
    if (cnpj == null || cnpj.length() != 14) {
      return cnpj;
    }
    return String.format("%s.%s.%s/%s-%s",
        cnpj.substring(0, 2),
        cnpj.substring(2, 5),
        cnpj.substring(5, 8),
        cnpj.substring(8, 12),
        cnpj.substring(12, 14)
    );
  }
}
