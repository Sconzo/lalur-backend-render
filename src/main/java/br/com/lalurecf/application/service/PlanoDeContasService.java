package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.planodecontas.CreatePlanoDeContasUseCase;
import br.com.lalurecf.application.port.in.planodecontas.GetPlanoDeContasUseCase;
import br.com.lalurecf.application.port.in.planodecontas.ListPlanoDeContasUseCase;
import br.com.lalurecf.application.port.in.planodecontas.TogglePlanoDeContasStatusUseCase;
import br.com.lalurecf.application.port.in.planodecontas.UpdatePlanoDeContasUseCase;
import br.com.lalurecf.application.port.out.CompanyRepositoryPort;
import br.com.lalurecf.application.port.out.ContaReferencialRepositoryPort;
import br.com.lalurecf.application.port.out.PlanoDeContasRepositoryPort;
import br.com.lalurecf.domain.enums.AccountType;
import br.com.lalurecf.domain.enums.ClasseContabil;
import br.com.lalurecf.domain.enums.NaturezaConta;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.Company;
import br.com.lalurecf.domain.model.ContaReferencial;
import br.com.lalurecf.domain.model.PlanoDeContas;
import br.com.lalurecf.domain.util.MascaraNiveisUtils;
import br.com.lalurecf.infrastructure.dto.mapper.PlanoDeContasDtoMapper;
import br.com.lalurecf.infrastructure.dto.planodecontas.CreatePlanoDeContasRequest;
import br.com.lalurecf.infrastructure.dto.planodecontas.PlanoDeContasResponse;
import br.com.lalurecf.infrastructure.dto.planodecontas.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.planodecontas.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.dto.planodecontas.UpdatePlanoDeContasRequest;
import br.com.lalurecf.infrastructure.exception.ResourceNotFoundException;
import br.com.lalurecf.infrastructure.security.CompanyContext;
import br.com.lalurecf.infrastructure.security.FiscalYearContext;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service que implementa os Use Cases de PlanoDeContas (Plano de Contas).
 *
 * <p>Gerencia CRUD de contas contábeis com validações ECF e vinculação a Conta Referencial RFB.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlanoDeContasService
    implements CreatePlanoDeContasUseCase,
        ListPlanoDeContasUseCase,
        GetPlanoDeContasUseCase,
        UpdatePlanoDeContasUseCase,
        TogglePlanoDeContasStatusUseCase {

  private final PlanoDeContasRepositoryPort planoDeContasRepository;
  private final ContaReferencialRepositoryPort contaReferencialRepository;
  private final CompanyRepositoryPort companyRepository;
  private final PlanoDeContasDtoMapper dtoMapper;

  @Override
  @Transactional
  public PlanoDeContasResponse execute(CreatePlanoDeContasRequest request) {
    log.info("Creating PlanoDeContas with code: {}", request.getCode());

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
    validateFiscalYear(fiscalYear);

    // Validar code não vazio
    if (request.getCode() == null || request.getCode().trim().isEmpty()) {
      throw new IllegalArgumentException("Code cannot be empty");
    }

    // Buscar empresa e validar code contra máscara de níveis
    Company company = companyRepository.findById(companyId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Company not found with id: " + companyId));

    if (company.getMascaraNiveis() != null) {
      MascaraNiveisUtils.validarCodigoContraMascara(request.getCode(), company.getMascaraNiveis());
    }

    // Derivar nível do código
    int nivelDerivado = MascaraNiveisUtils.derivarNivel(request.getCode());

    // Validar contaReferencialId existe e está ACTIVE (se informado)
    ContaReferencial contaReferencial = null;
    if (request.getContaReferencialId() != null) {
      contaReferencial =
          contaReferencialRepository
              .findById(request.getContaReferencialId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "ContaReferencial not found with id: "
                              + request.getContaReferencialId()));

      if (contaReferencial.getStatus() != Status.ACTIVE) {
        throw new IllegalArgumentException(
            "ContaReferencial must be ACTIVE. Current status: " + contaReferencial.getStatus());
      }
    }

    // Verificar unicidade (company + code + fiscalYear)
    Optional<PlanoDeContas> existing =
        planoDeContasRepository.findByCompanyIdAndCodeAndFiscalYear(
            companyId, request.getCode(), fiscalYear);

    if (existing.isPresent()) {
      throw new IllegalArgumentException(
          String.format(
              "PlanoDeContas with code '%s' already exists for company %d and fiscal year %d",
              request.getCode(), companyId, fiscalYear));
    }

    // Criar conta
    PlanoDeContas account =
        PlanoDeContas.builder()
            .companyId(companyId)
            .code(request.getCode())
            .name(request.getName())
            .fiscalYear(fiscalYear)
            .accountType(request.getAccountType())
            .contaReferencialId(request.getContaReferencialId())
            .classe(request.getClasse())
            .nivel(nivelDerivado)
            .natureza(request.getNatureza())
            .afetaResultado(request.getAfetaResultado())
            .dedutivel(request.getDedutivel())
            .status(Status.ACTIVE)
            .build();

    PlanoDeContas saved = planoDeContasRepository.save(account);
    log.info("PlanoDeContas created successfully with id: {}", saved.getId());

    String codigoRfb = contaReferencial != null ? contaReferencial.getCodigoRfb() : null;
    return dtoMapper.toResponse(saved, codigoRfb);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PlanoDeContasResponse> execute(
      Integer fiscalYear,
      AccountType accountType,
      ClasseContabil classe,
      NaturezaConta natureza,
      String search,
      Boolean includeInactive,
      Boolean leafOnly,
      Pageable pageable) {

    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    log.info(
        "Listing PlanoDeContas for company: {}, fiscalYear: {}, leafOnly: {}",
        companyId, fiscalYear, leafOnly);

    // Determinar último nível da máscara (se leafOnly)
    Integer nivelFilter = null;
    if (Boolean.TRUE.equals(leafOnly)) {
      Company company = companyRepository.findById(companyId)
          .orElseThrow(() -> new IllegalArgumentException(
              "Company not found with id: " + companyId));
      nivelFilter = company.getMascaraNiveis().split("\\.").length;
    }

    Page<PlanoDeContas> accountsPage =
        planoDeContasRepository.findFiltered(
            companyId,
            fiscalYear,
            accountType,
            classe,
            natureza,
            search,
            nivelFilter,
            Boolean.TRUE.equals(includeInactive),
            pageable);

    // Batch-fetch contas referenciais da página (evita N+1)
    List<Long> contaRefIds =
        accountsPage.getContent().stream()
            .map(PlanoDeContas::getContaReferencialId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

    Map<Long, String> codigoRfbById =
        contaRefIds.isEmpty()
            ? Map.of()
            : contaReferencialRepository.findAllById(contaRefIds).stream()
                .collect(
                    Collectors.toMap(ContaReferencial::getId, ContaReferencial::getCodigoRfb));

    return accountsPage.map(
        acc -> {
          String codigoRfb =
              acc.getContaReferencialId() != null
                  ? codigoRfbById.get(acc.getContaReferencialId())
                  : null;
          return dtoMapper.toResponse(acc, codigoRfb);
        });
  }

  @Override
  @Transactional(readOnly = true)
  public PlanoDeContasResponse execute(Long id) {
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    log.info("Getting PlanoDeContas with id: {}", id);

    PlanoDeContas account =
        planoDeContasRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("PlanoDeContas not found with id: " + id));

    // Validar que pertence à empresa do contexto
    if (!account.getCompanyId().equals(companyId)) {
      throw new IllegalArgumentException(
          "PlanoDeContas does not belong to company in context");
    }

    // Buscar código da conta referencial (se vinculada)
    String codigoRfb = null;
    if (account.getContaReferencialId() != null) {
      codigoRfb =
          contaReferencialRepository
              .findById(account.getContaReferencialId())
              .map(ContaReferencial::getCodigoRfb)
              .orElse(null);
    }

    return dtoMapper.toResponse(account, codigoRfb);
  }

  @Override
  @Transactional
  public PlanoDeContasResponse execute(Long id, UpdatePlanoDeContasRequest request) {
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    log.info("Updating PlanoDeContas with id: {}", id);

    PlanoDeContas account =
        planoDeContasRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("PlanoDeContas not found with id: " + id));

    // Validar que pertence à empresa do contexto
    if (!account.getCompanyId().equals(companyId)) {
      throw new IllegalArgumentException(
          "PlanoDeContas does not belong to company in context");
    }

    // Validar contaReferencialId existe e está ACTIVE (se informado)
    ContaReferencial contaReferencial = null;
    if (request.getContaReferencialId() != null) {
      contaReferencial =
          contaReferencialRepository
              .findById(request.getContaReferencialId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "ContaReferencial not found with id: "
                              + request.getContaReferencialId()));

      if (contaReferencial.getStatus() != Status.ACTIVE) {
        throw new IllegalArgumentException(
            "ContaReferencial must be ACTIVE. Current status: " + contaReferencial.getStatus());
      }
    }

    // Atualizar campos (code e fiscalYear são imutáveis; nivel é derivado do code)
    account.setName(request.getName());
    account.setAccountType(request.getAccountType());
    account.setContaReferencialId(request.getContaReferencialId());
    account.setClasse(request.getClasse());
    account.setNatureza(request.getNatureza());
    account.setAfetaResultado(request.getAfetaResultado());
    account.setDedutivel(request.getDedutivel());

    PlanoDeContas updated = planoDeContasRepository.save(account);
    log.info("PlanoDeContas updated successfully with id: {}", updated.getId());

    String codigoRfb = contaReferencial != null ? contaReferencial.getCodigoRfb() : null;
    return dtoMapper.toResponse(updated, codigoRfb);
  }

  @Override
  @Transactional
  public ToggleStatusResponse execute(Long id, ToggleStatusRequest request) {
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    log.info("Toggling status of PlanoDeContas with id: {} to {}", id, request.getStatus());

    PlanoDeContas account =
        planoDeContasRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("PlanoDeContas not found with id: " + id));

    // Validar que pertence à empresa do contexto
    if (!account.getCompanyId().equals(companyId)) {
      throw new IllegalArgumentException(
          "PlanoDeContas does not belong to company in context");
    }

    // Alternar status
    account.setStatus(request.getStatus());
    planoDeContasRepository.save(account);

    String message =
        String.format(
            "PlanoDeContas '%s' status changed to %s", account.getName(), request.getStatus());

    log.info("PlanoDeContas status toggled successfully: {}", message);

    return ToggleStatusResponse.builder()
        .success(true)
        .message(message)
        .newStatus(request.getStatus())
        .build();
  }

  /**
   * Valida que fiscal year está no range permitido (2000 a ano atual + 1).
   *
   * @param fiscalYear ano fiscal a validar
   */
  private void validateFiscalYear(Integer fiscalYear) {
    int currentYear = Year.now().getValue();
    int maxYear = currentYear + 1;

    if (fiscalYear < 2000 || fiscalYear > maxYear) {
      throw new IllegalArgumentException(
          String.format("Fiscal year must be between 2000 and %d. Got: %d", maxYear, fiscalYear));
    }
  }
}
