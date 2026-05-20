package br.com.lalurecf.infrastructure.adapter.in.rest;

import br.com.lalurecf.application.port.in.contaparteb.CreateContaParteBUseCase;
import br.com.lalurecf.application.port.in.contaparteb.GetContaParteBUseCase;
import br.com.lalurecf.application.port.in.contaparteb.ListContaParteBUseCase;
import br.com.lalurecf.application.port.in.contaparteb.ToggleContaParteBStatusUseCase;
import br.com.lalurecf.application.port.in.contaparteb.UpdateContaParteBUseCase;
import br.com.lalurecf.domain.enums.TipoTributo;
import br.com.lalurecf.infrastructure.dto.contaparteb.ContaParteBResponse;
import br.com.lalurecf.infrastructure.dto.contaparteb.CreateContaParteBRequest;
import br.com.lalurecf.infrastructure.dto.contaparteb.UpdateContaParteBRequest;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
 * Controller para gerenciamento de Contas da Parte B (e-Lalur/e-Lacs).
 *
 * <p>Todos endpoints requerem role CONTADOR e header X-Company-Id (contexto de empresa).
 */
@RestController
@RequestMapping("/conta-parte-b")
@RequiredArgsConstructor
@Tag(name = "Contas Parte B", description = "Gerenciamento de contas da Parte B (e-Lalur/e-Lacs)")
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class ContaParteBController {

  private final CreateContaParteBUseCase createContaParteBUseCase;
  private final ListContaParteBUseCase listContaParteBUseCase;
  private final GetContaParteBUseCase getContaParteBUseCase;
  private final UpdateContaParteBUseCase updateContaParteBUseCase;
  private final ToggleContaParteBStatusUseCase toggleContaParteBStatusUseCase;

  /**
   * Cria uma nova conta da Parte B.
   *
   * @param request dados da conta a ser criada
   * @return conta criada
   */
  @PostMapping
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Criar conta Parte B",
      description =
          "Cria uma nova conta fiscal da Parte B (e-Lalur/e-Lacs) "
              + "para a empresa no contexto (header X-Company-Id)")
  public ResponseEntity<ContaParteBResponse> createContaParteB(
      @Valid @RequestBody CreateContaParteBRequest request) {
    ContaParteBResponse response = createContaParteBUseCase.createContaParteB(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Lista contas da Parte B com paginação e filtros.
   *
   * @param search termo de busca para codigoConta/descricao (opcional)
   * @param anoBase filtro por ano base (opcional)
   * @param tipoTributo filtro por tipo de tributo (opcional)
   * @param includeInactive se deve incluir contas inativas
   * @param pageable configuração de paginação
   * @return página de contas Parte B
   */
  @GetMapping
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Listar contas Parte B",
      description =
          "Lista contas da Parte B da empresa no contexto com filtros e paginação "
              + "(header X-Company-Id)")
  public ResponseEntity<Page<ContaParteBResponse>> listContasParteB(
      @RequestParam(required = false) String search,
      @RequestParam(name = "ano_base", required = false) Integer anoBase,
      @RequestParam(name = "tipo_tributo", required = false) TipoTributo tipoTributo,
      @RequestParam(name = "include_inactive", required = false, defaultValue = "false")
          Boolean includeInactive,
      @PageableDefault(size = 100, sort = "codigoConta", direction = Sort.Direction.ASC)
          Pageable pageable) {
    Page<ContaParteBResponse> response =
        listContaParteBUseCase.listContasParteB(
            search, anoBase, tipoTributo, includeInactive, pageable);
    return ResponseEntity.ok(response);
  }

  /**
   * Obtém conta da Parte B por ID.
   *
   * @param id ID da conta
   * @return dados da conta
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Obter conta Parte B",
      description = "Obtém conta da Parte B por ID (header X-Company-Id)")
  public ResponseEntity<ContaParteBResponse> getContaParteBById(@PathVariable Long id) {
    ContaParteBResponse response = getContaParteBUseCase.getContaParteBById(id);
    return ResponseEntity.ok(response);
  }

  /**
   * Atualiza dados de uma conta da Parte B.
   *
   * @param id ID da conta
   * @param request dados atualizados (sem codigoConta e anoBase)
   * @return conta atualizada
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Atualizar conta Parte B",
      description =
          "Atualiza conta da Parte B (campos imutáveis: codigoConta, anoBase). "
              + "Requer header X-Company-Id")
  public ResponseEntity<ContaParteBResponse> updateContaParteB(
      @PathVariable Long id, @Valid @RequestBody UpdateContaParteBRequest request) {
    ContaParteBResponse response = updateContaParteBUseCase.updateContaParteB(id, request);
    return ResponseEntity.ok(response);
  }

  /**
   * Altera status de uma conta da Parte B.
   *
   * @param id ID da conta
   * @param request novo status
   * @return resposta com novo status
   */
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Alternar status",
      description = "Altera status da conta entre ACTIVE e INACTIVE (header X-Company-Id)")
  public ResponseEntity<ToggleStatusResponse> toggleStatus(
      @PathVariable Long id, @Valid @RequestBody ToggleStatusRequest request) {
    ToggleStatusResponse response = toggleContaParteBStatusUseCase.toggleStatus(id, request);
    return ResponseEntity.ok(response);
  }
}
