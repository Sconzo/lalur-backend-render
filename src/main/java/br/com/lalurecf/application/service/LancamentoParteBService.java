package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.lancamentoparteb.CreateLancamentoParteBUseCase;
import br.com.lalurecf.application.port.in.lancamentoparteb.GetLancamentoParteBUseCase;
import br.com.lalurecf.application.port.in.lancamentoparteb.ListLancamentoParteBUseCase;
import br.com.lalurecf.application.port.in.lancamentoparteb.ToggleLancamentoParteBStatusUseCase;
import br.com.lalurecf.application.port.in.lancamentoparteb.UpdateLancamentoParteBUseCase;
import br.com.lalurecf.application.port.out.ContaParteBRepositoryPort;
import br.com.lalurecf.application.port.out.LancamentoParteBRepositoryPort;
import br.com.lalurecf.application.port.out.PlanoDeContasRepositoryPort;
import br.com.lalurecf.application.port.out.TaxParameterRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.TipoAjuste;
import br.com.lalurecf.domain.enums.TipoApuracao;
import br.com.lalurecf.domain.enums.TipoRelacionamento;
import br.com.lalurecf.domain.model.ContaParteB;
import br.com.lalurecf.domain.model.LancamentoParteB;
import br.com.lalurecf.domain.model.PlanoDeContas;
import br.com.lalurecf.domain.model.TaxParameter;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.CreateLancamentoParteBRequest;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.LancamentoParteBResponse;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.UpdateLancamentoParteBRequest;
import br.com.lalurecf.infrastructure.dto.mapper.LancamentoParteBDtoMapper;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.exception.ResourceNotFoundException;
import br.com.lalurecf.infrastructure.security.CompanyContext;
import br.com.lalurecf.infrastructure.security.FiscalYearContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service que implementa os Use Cases de LancamentoParteB.
 *
 * <p>Gerencia CRUD de lançamentos fiscais (ajustes) da Parte B com validações de FK condicionais e
 * verificação de parâmetros tributários.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LancamentoParteBService
    implements CreateLancamentoParteBUseCase,
        ListLancamentoParteBUseCase,
        GetLancamentoParteBUseCase,
        UpdateLancamentoParteBUseCase,
        ToggleLancamentoParteBStatusUseCase {

  private final LancamentoParteBRepositoryPort lancamentoParteBRepository;
  private final TaxParameterRepositoryPort taxParameterRepository;
  private final PlanoDeContasRepositoryPort planoDeContasRepository;
  private final ContaParteBRepositoryPort contaParteBRepository;
  private final LancamentoParteBDtoMapper dtoMapper;

  @Override
  @Transactional
  public LancamentoParteBResponse createLancamentoParteB(CreateLancamentoParteBRequest request) {
    // Obter ano fiscal do contexto (header X-Fiscal-Year)
    Integer anoReferencia = FiscalYearContext.getCurrentFiscalYear();
    if (anoReferencia == null) {
      throw new IllegalArgumentException(
          "Fiscal year context is required (header X-Fiscal-Year missing)");
    }

    log.info(
        "Creating LancamentoParteB for {} {}/{}",
        request.getTipoApuracao(),
        request.getMesReferencia(),
        anoReferencia);

    // Obter empresa do contexto
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    // Validar parâmetro tributário (obrigatório, deve existir e estar ACTIVE)
    TaxParameter parametroTributario =
        taxParameterRepository
            .findById(request.getParametroTributarioId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Parâmetro tributário não encontrado com id: "
                            + request.getParametroTributarioId()));

    if (parametroTributario.getStatus() != Status.ACTIVE) {
      throw new IllegalArgumentException(
          "Parâmetro tributário deve estar ACTIVE. Status atual: "
              + parametroTributario.getStatus());
    }

    // Validar FK condicionais conforme tipoRelacionamento
    validateConditionalForeignKeys(
        request.getTipoRelacionamento(),
        request.getContaContabilId(),
        request.getContaParteBId(),
        companyId);

    // Criar lançamento
    LancamentoParteB lancamento =
        LancamentoParteB.builder()
            .companyId(companyId)
            .mesReferencia(request.getMesReferencia())
            .anoReferencia(anoReferencia)
            .tipoApuracao(request.getTipoApuracao())
            .tipoRelacionamento(request.getTipoRelacionamento())
            .contaContabilId(request.getContaContabilId())
            .contaParteBId(request.getContaParteBId())
            .parametroTributarioId(request.getParametroTributarioId())
            .tipoAjuste(request.getTipoAjuste())
            .descricao(request.getDescricao())
            .valor(request.getValor())
            .status(Status.ACTIVE)
            .build();

    LancamentoParteB saved = lancamentoParteBRepository.save(lancamento);
    log.info("LancamentoParteB created successfully with id: {}", saved.getId());

    return toResponseWithCodes(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<LancamentoParteBResponse> listLancamentosParteB(
      Integer anoReferencia,
      Integer mesReferencia,
      TipoApuracao tipoApuracao,
      TipoAjuste tipoAjuste,
      Boolean includeInactive,
      Pageable pageable) {

    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    log.info("Listing LancamentosParteB for company: {}", companyId);

    Page<LancamentoParteB> lancamentosPage =
        lancamentoParteBRepository.findFiltered(
            companyId,
            anoReferencia,
            mesReferencia,
            tipoApuracao,
            tipoAjuste,
            Boolean.TRUE.equals(includeInactive),
            pageable);

    List<LancamentoParteB> content = lancamentosPage.getContent();

    // Batch-fetch códigos relacionados para evitar N+1
    Map<Long, String> contaContabilCodes = fetchPlanoDeContasCodes(content);
    Map<Long, String> contaParteBCodes = fetchContaParteBCodes(content);
    Map<Long, String> parametroCodes = fetchParametroCodes(content);

    return lancamentosPage.map(
        l ->
            dtoMapper.toResponse(
                l,
                contaContabilCodes.get(l.getContaContabilId()),
                contaParteBCodes.get(l.getContaParteBId()),
                parametroCodes.get(l.getParametroTributarioId())));
  }

  private Map<Long, String> fetchPlanoDeContasCodes(List<LancamentoParteB> lancamentos) {
    List<Long> ids =
        lancamentos.stream()
            .map(LancamentoParteB::getContaContabilId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    if (ids.isEmpty()) {
      return new HashMap<>();
    }
    return planoDeContasRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(PlanoDeContas::getId, PlanoDeContas::getCode));
  }

  private Map<Long, String> fetchContaParteBCodes(List<LancamentoParteB> lancamentos) {
    List<Long> ids =
        lancamentos.stream()
            .map(LancamentoParteB::getContaParteBId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    if (ids.isEmpty()) {
      return new HashMap<>();
    }
    return contaParteBRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(ContaParteB::getId, ContaParteB::getCodigoConta));
  }

  private Map<Long, String> fetchParametroCodes(List<LancamentoParteB> lancamentos) {
    List<Long> ids =
        lancamentos.stream()
            .map(LancamentoParteB::getParametroTributarioId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    if (ids.isEmpty()) {
      return new HashMap<>();
    }
    return taxParameterRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(TaxParameter::getId, TaxParameter::getCode));
  }

  private LancamentoParteBResponse toResponseWithCodes(LancamentoParteB lancamento) {
    String contaContabilCode = lancamento.getContaContabilId() == null
        ? null
        : planoDeContasRepository.findById(lancamento.getContaContabilId())
            .map(PlanoDeContas::getCode).orElse(null);
    String contaParteBCode = lancamento.getContaParteBId() == null
        ? null
        : contaParteBRepository.findById(lancamento.getContaParteBId())
            .map(ContaParteB::getCodigoConta).orElse(null);
    String parametroCodigo = lancamento.getParametroTributarioId() == null
        ? null
        : taxParameterRepository.findById(lancamento.getParametroTributarioId())
            .map(TaxParameter::getCode).orElse(null);
    return dtoMapper.toResponse(lancamento, contaContabilCode, contaParteBCode, parametroCodigo);
  }

  @Override
  @Transactional(readOnly = true)
  public LancamentoParteBResponse getLancamentoParteBById(Long id) {
    log.info("Getting LancamentoParteB by id: {}", id);

    LancamentoParteB lancamento =
        lancamentoParteBRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("LancamentoParteB not found with id: " + id));

    return toResponseWithCodes(lancamento);
  }

  @Override
  @Transactional
  public LancamentoParteBResponse updateLancamentoParteB(
      Long id, UpdateLancamentoParteBRequest request) {
    log.info("Updating LancamentoParteB with id: {}", id);

    // Obter empresa do contexto
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    // Validar parâmetro tributário (obrigatório, deve existir e estar ACTIVE)
    TaxParameter parametroTributario =
        taxParameterRepository
            .findById(request.getParametroTributarioId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Parâmetro tributário não encontrado com id: "
                            + request.getParametroTributarioId()));

    if (parametroTributario.getStatus() != Status.ACTIVE) {
      throw new IllegalArgumentException(
          "Parâmetro tributário deve estar ACTIVE. Status atual: "
              + parametroTributario.getStatus());
    }

    // Validar FK condicionais conforme tipoRelacionamento
    validateConditionalForeignKeys(
        request.getTipoRelacionamento(),
        request.getContaContabilId(),
        request.getContaParteBId(),
        companyId);

    // Buscar lançamento existente após validações
    LancamentoParteB lancamento =
        lancamentoParteBRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("LancamentoParteB not found with id: " + id));

    // Obter ano fiscal do contexto (header X-Fiscal-Year)
    Integer anoReferencia = FiscalYearContext.getCurrentFiscalYear();
    if (anoReferencia == null) {
      throw new IllegalArgumentException(
          "Fiscal year context is required (header X-Fiscal-Year missing)");
    }

    // Atualizar campos
    lancamento.setMesReferencia(request.getMesReferencia());
    lancamento.setAnoReferencia(anoReferencia);
    lancamento.setTipoApuracao(request.getTipoApuracao());
    lancamento.setTipoRelacionamento(request.getTipoRelacionamento());
    lancamento.setContaContabilId(request.getContaContabilId());
    lancamento.setContaParteBId(request.getContaParteBId());
    lancamento.setParametroTributarioId(request.getParametroTributarioId());
    lancamento.setTipoAjuste(request.getTipoAjuste());
    lancamento.setDescricao(request.getDescricao());
    lancamento.setValor(request.getValor());

    LancamentoParteB updated = lancamentoParteBRepository.save(lancamento);
    log.info("LancamentoParteB updated successfully with id: {}", updated.getId());

    return toResponseWithCodes(updated);
  }

  @Override
  @Transactional
  public ToggleStatusResponse toggleStatus(Long id, ToggleStatusRequest request) {
    log.info("Toggling status of LancamentoParteB id: {} to {}", id, request.getStatus());

    LancamentoParteB lancamento =
        lancamentoParteBRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("LancamentoParteB not found with id: " + id));

    Status oldStatus = lancamento.getStatus();
    lancamento.setStatus(request.getStatus());
    lancamentoParteBRepository.save(lancamento);

    log.info("LancamentoParteB status toggled from {} to {}", oldStatus, request.getStatus());

    return ToggleStatusResponse.builder()
        .success(true)
        .message("Status alterado com sucesso")
        .newStatus(request.getStatus())
        .build();
  }

  /**
   * Valida FK condicionais baseadas no tipoRelacionamento.
   *
   * @param tipoRelacionamento tipo de relacionamento
   * @param contaContabilId ID da conta contábil (pode ser null)
   * @param contaParteBId ID da conta Parte B (pode ser null)
   * @param companyId ID da empresa
   */
  private void validateConditionalForeignKeys(
      TipoRelacionamento tipoRelacionamento,
      Long contaContabilId,
      Long contaParteBId,
      Long companyId) {

    switch (tipoRelacionamento) {
      case CONTA_CONTABIL:
        if (contaContabilId == null) {
          throw new IllegalArgumentException(
              "contaContabilId é obrigatória quando tipoRelacionamento = CONTA_CONTABIL");
        }
        if (contaParteBId != null) {
          throw new IllegalArgumentException(
              "contaParteBId deve ser nula quando tipoRelacionamento = CONTA_CONTABIL");
        }
        // Validar que conta contábil existe e pertence à empresa
        validateContaContabil(contaContabilId, companyId);
        break;

      case CONTA_PARTE_B:
        if (contaParteBId == null) {
          throw new IllegalArgumentException(
              "contaParteBId é obrigatória quando tipoRelacionamento = CONTA_PARTE_B");
        }
        if (contaContabilId != null) {
          throw new IllegalArgumentException(
              "contaContabilId deve ser nula quando tipoRelacionamento = CONTA_PARTE_B");
        }
        // Validar que conta Parte B existe e pertence à empresa
        validateContaParteB(contaParteBId, companyId);
        break;

      case AMBOS:
        if (contaContabilId == null) {
          throw new IllegalArgumentException(
              "contaContabilId é obrigatória quando tipoRelacionamento = AMBOS");
        }
        if (contaParteBId == null) {
          throw new IllegalArgumentException(
              "contaParteBId é obrigatória quando tipoRelacionamento = AMBOS");
        }
        // Validar ambas contas
        validateContaContabil(contaContabilId, companyId);
        validateContaParteB(contaParteBId, companyId);
        break;

      default:
        throw new IllegalArgumentException("tipoRelacionamento inválido: " + tipoRelacionamento);
    }
  }

  /**
   * Valida que conta contábil existe e pertence à empresa.
   */
  private void validateContaContabil(Long contaContabilId, Long companyId) {
    PlanoDeContas contaContabil =
        planoDeContasRepository
            .findById(contaContabilId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Conta contábil não encontrada com id: " + contaContabilId));

    if (!contaContabil.getCompanyId().equals(companyId)) {
      throw new IllegalArgumentException(
          "Conta contábil não pertence à empresa no contexto (X-Company-Id)");
    }
  }

  /**
   * Valida que conta Parte B existe e pertence à empresa.
   */
  private void validateContaParteB(Long contaParteBId, Long companyId) {
    ContaParteB contaParteB =
        contaParteBRepository
            .findById(contaParteBId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Conta Parte B não encontrada com id: " + contaParteBId));

    if (!contaParteB.getCompanyId().equals(companyId)) {
      throw new IllegalArgumentException(
          "Conta Parte B não pertence à empresa no contexto (X-Company-Id)");
    }
  }
}
