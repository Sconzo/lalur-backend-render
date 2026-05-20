package br.com.lalurecf.infrastructure.adapter.in.rest;

import br.com.lalurecf.application.port.in.taxparametertype.CreateTaxParameterTypeUseCase;
import br.com.lalurecf.application.port.in.taxparametertype.ListTaxParameterTypesUseCase;
import br.com.lalurecf.infrastructure.dto.taxparametertype.CreateTaxParameterTypeRequest;
import br.com.lalurecf.infrastructure.dto.taxparametertype.TaxParameterTypeResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para operações com tipos de parâmetros tributários.
 *
 * <p>Endpoints disponíveis (todos ADMIN only):
 * <ul>
 *   <li>POST /tax-parameter-types - Criar tipo
 *   <li>GET /tax-parameter-types - Listar tipos ativos
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/tax-parameter-types")
@RequiredArgsConstructor
public class TaxParameterTypeController {

  private final CreateTaxParameterTypeUseCase createTaxParameterTypeUseCase;
  private final ListTaxParameterTypesUseCase listTaxParameterTypesUseCase;

  /**
   * Cria um novo tipo de parâmetro tributário.
   *
   * @param request dados do tipo
   * @return tipo criado com status 201 CREATED
   */
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TaxParameterTypeResponse> create(
      @Valid @RequestBody CreateTaxParameterTypeRequest request) {
    log.info("POST /tax-parameter-types - Criando tipo: {}", request.description());
    TaxParameterTypeResponse response = createTaxParameterTypeUseCase.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Lista todos os tipos de parâmetros tributários ativos.
   *
   * @return lista de tipos ordenados por descrição
   */
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<TaxParameterTypeResponse>> listAll() {
    log.debug("GET /tax-parameter-types - Listando tipos");
    List<TaxParameterTypeResponse> response = listTaxParameterTypesUseCase.listAll();
    return ResponseEntity.ok(response);
  }
}
