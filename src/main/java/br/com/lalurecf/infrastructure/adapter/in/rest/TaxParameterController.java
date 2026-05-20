package br.com.lalurecf.infrastructure.adapter.in.rest;

import br.com.lalurecf.application.port.in.taxparameter.CreateTaxParameterUseCase;
import br.com.lalurecf.application.port.in.taxparameter.GetTaxParameterTypesUseCase;
import br.com.lalurecf.application.port.in.taxparameter.GetTaxParameterUseCase;
import br.com.lalurecf.application.port.in.taxparameter.ListTaxParametersUseCase;
import br.com.lalurecf.application.port.in.taxparameter.ToggleTaxParameterStatusUseCase;
import br.com.lalurecf.application.port.in.taxparameter.UpdateTaxParameterUseCase;
import br.com.lalurecf.domain.enums.ParameterNature;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.infrastructure.dto.company.FilterOptionsResponse;
import br.com.lalurecf.infrastructure.dto.company.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.company.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.dto.taxparameter.CreateTaxParameterRequest;
import br.com.lalurecf.infrastructure.dto.taxparameter.TaxParameterResponse;
import br.com.lalurecf.infrastructure.dto.taxparameter.TaxParameterTypeGroup;
import br.com.lalurecf.infrastructure.dto.taxparameter.UpdateTaxParameterRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

/**
 * Controller REST para operações com parâmetros tributários.
 *
 * <p>Endpoints disponíveis (todos ADMIN only):
 * <ul>
 *   <li>POST /tax-parameters - Criar parâmetro
 *   <li>GET /tax-parameters - Listar parâmetros com filtros
 *   <li>GET /tax-parameters/{id} - Visualizar parâmetro
 *   <li>PUT /tax-parameters/{id} - Editar parâmetro
 *   <li>PATCH /tax-parameters/{id}/status - Toggle status
 *   <li>GET /tax-parameters/filter-options/types - Listar tipos únicos (dropdown)
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/tax-parameters")
@RequiredArgsConstructor
public class TaxParameterController {

  private final CreateTaxParameterUseCase createTaxParameterUseCase;
  private final ListTaxParametersUseCase listTaxParametersUseCase;
  private final GetTaxParameterUseCase getTaxParameterUseCase;
  private final UpdateTaxParameterUseCase updateTaxParameterUseCase;
  private final ToggleTaxParameterStatusUseCase toggleTaxParameterStatusUseCase;
  private final GetTaxParameterTypesUseCase getTaxParameterTypesUseCase;

  /**
   * Cria um novo parâmetro tributário.
   *
   * @param request dados do parâmetro
   * @return parâmetro criado com status 201 CREATED
   */
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TaxParameterResponse> create(
      @Valid @RequestBody CreateTaxParameterRequest request) {
    log.info("POST /tax-parameters - Criando parâmetro com código: {}", request.code());
    TaxParameterResponse response = createTaxParameterUseCase.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Lista parâmetros tributários com filtros e paginação.
   *
   * @param type filtro por tipo (categoria) - opcional
   * @param nature filtro por natureza (GLOBAL, MONTHLY, QUARTERLY) - opcional
   * @param search busca em código e descrição - opcional
   * @param includeInactive incluir parâmetros inativos
   * @param pageable configuração de paginação
   * @return página de parâmetros
   */
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<TaxParameterResponse>> list(
      @RequestParam(required = false) String type,
      @RequestParam(required = false) Long typeId,
      @RequestParam(required = false) ParameterNature nature,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "false") boolean includeInactive,
      @RequestParam(required = false) Boolean fiscalMovementExclusive,
      @PageableDefault(size = 50, sort = "codigo") Pageable pageable) {

    log.info("GET /tax-parameters - Listando parâmetros. Nature: {}", nature);
    Page<TaxParameterResponse> response =
        listTaxParametersUseCase.list(type, typeId, nature, search, includeInactive,
            fiscalMovementExclusive, pageable);
    return ResponseEntity.ok(response);
  }

  /**
   * Busca parâmetro tributário por ID.
   *
   * @param id ID do parâmetro
   * @return parâmetro encontrado
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TaxParameterResponse> getById(@PathVariable Long id) {
    log.info("GET /tax-parameters/{} - Buscando parâmetro", id);
    TaxParameterResponse response = getTaxParameterUseCase.getById(id);
    return ResponseEntity.ok(response);
  }

  /**
   * Atualiza dados de um parâmetro tributário existente.
   *
   * @param id ID do parâmetro
   * @param request novos dados (código, tipo, descrição)
   * @return parâmetro atualizado
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TaxParameterResponse> update(
      @PathVariable Long id,
      @Valid @RequestBody UpdateTaxParameterRequest request) {

    log.info("PUT /tax-parameters/{} - Atualizando parâmetro", id);
    TaxParameterResponse response = updateTaxParameterUseCase.update(id, request);
    return ResponseEntity.ok(response);
  }

  /**
   * Alterna status de um parâmetro tributário (ACTIVE ↔ INACTIVE).
   *
   * @param id ID do parâmetro
   * @param request novo status desejado
   * @return resposta com sucesso e novo status
   */
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ToggleStatusResponse> toggleStatus(
      @PathVariable Long id,
      @Valid @RequestBody ToggleStatusRequest request) {

    log.info("PATCH /tax-parameters/{}/status - Alterando status para {}", id, request.status());
    ToggleStatusResponse response =
        toggleTaxParameterStatusUseCase.toggleStatus(id, request.status().toStatus());
    return ResponseEntity.ok(response);
  }

  /**
   * Retorna lista de tipos/categorias únicos para popular dropdown de filtro.
   *
   * @param search texto de busca (opcional)
   * @return lista de tipos únicos ordenados (limitado a 100 resultados)
   */
  @GetMapping("/filter-options/types")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<FilterOptionsResponse> getTypeFilterOptions(
      @RequestParam(required = false) String search) {

    log.debug("GET /tax-parameters/filter-options/types - search: {}", search);
    List<String> types = getTaxParameterTypesUseCase.getTypes(search);
    return ResponseEntity.ok(new FilterOptionsResponse(types));
  }


  /**
   * Retorna parâmetros tributários organizados por tipo.
   *
   * @return mapa de tipo -> grupo com natureza e lista de parâmetros
   */
  @GetMapping("/grouped")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<HashMap<String, TaxParameterTypeGroup>>
      getTaxParametersGrouped() {

    log.debug("GET /tax-parameters/grouped");
    HashMap<String, TaxParameterTypeGroup> response =
        getTaxParameterTypesUseCase.getTaxParametersForCompanyCreation();
    return ResponseEntity.ok(response);
  }
}
