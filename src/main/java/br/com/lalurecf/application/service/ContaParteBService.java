package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.contaparteb.CreateContaParteBUseCase;
import br.com.lalurecf.application.port.in.contaparteb.GetContaParteBUseCase;
import br.com.lalurecf.application.port.in.contaparteb.ListContaParteBUseCase;
import br.com.lalurecf.application.port.in.contaparteb.ToggleContaParteBStatusUseCase;
import br.com.lalurecf.application.port.in.contaparteb.UpdateContaParteBUseCase;
import br.com.lalurecf.application.port.out.ContaParteBRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.TipoTributo;
import br.com.lalurecf.domain.model.ContaParteB;
import br.com.lalurecf.infrastructure.dto.contaparteb.ContaParteBResponse;
import br.com.lalurecf.infrastructure.dto.contaparteb.CreateContaParteBRequest;
import br.com.lalurecf.infrastructure.dto.contaparteb.UpdateContaParteBRequest;
import br.com.lalurecf.infrastructure.dto.mapper.ContaParteBDtoMapper;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.exception.ResourceNotFoundException;
import br.com.lalurecf.infrastructure.security.CompanyContext;
import java.time.Year;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service que implementa os Use Cases de ContaParteB (Contas da Parte B e-Lalur/e-Lacs).
 *
 * <p>Gerencia CRUD de contas fiscais específicas de IRPJ/CSLL com validações de vigência temporal
 * e saldo inicial.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class ContaParteBService
    implements CreateContaParteBUseCase,
        ListContaParteBUseCase,
        GetContaParteBUseCase,
        UpdateContaParteBUseCase,
        ToggleContaParteBStatusUseCase {

  private final ContaParteBRepositoryPort contaParteBRepository;
  private final ContaParteBDtoMapper dtoMapper;

  @Override
  @Transactional
  public ContaParteBResponse createContaParteB(CreateContaParteBRequest request) {
    log.info("Creating ContaParteB with code: {}", request.getCodigoConta());

    // Obter empresa do contexto
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    // Validar dataVigenciaFim >= dataVigenciaInicio
    if (request.getDataVigenciaFim() != null
        && request.getDataVigenciaFim().isBefore(request.getDataVigenciaInicio())) {
      throw new IllegalArgumentException("dataVigenciaFim must be >= dataVigenciaInicio");
    }

    // Verificar unicidade (company + codigoConta + anoBase)
    contaParteBRepository
        .findByCompanyIdAndCodigoContaAndAnoBase(
            companyId, request.getCodigoConta(), request.getAnoBase())
        .ifPresent(
            existing -> {
              throw new IllegalArgumentException(
                  String.format(
                      "Já existe conta Parte B com código '%s' para o ano base %d",
                      request.getCodigoConta(), request.getAnoBase()));
            });

    // Criar conta
    ContaParteB conta =
        ContaParteB.builder()
            .companyId(companyId)
            .codigoConta(request.getCodigoConta())
            .descricao(request.getDescricao())
            .anoBase(request.getAnoBase())
            .dataVigenciaInicio(request.getDataVigenciaInicio())
            .dataVigenciaFim(request.getDataVigenciaFim())
            .tipoTributo(request.getTipoTributo())
            .saldoInicial(request.getSaldoInicial())
            .tipoSaldo(request.getTipoSaldo())
            .status(Status.ACTIVE)
            .build();

    ContaParteB saved = contaParteBRepository.save(conta);
    log.info("ContaParteB created successfully with id: {}", saved.getId());

    return dtoMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ContaParteBResponse> listContasParteB(
      String search,
      Integer anoBase,
      TipoTributo tipoTributo,
      Boolean includeInactive,
      Pageable pageable) {

    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context is required (header X-Company-Id missing)");
    }

    log.info("Listing ContasParteB for company: {}, anoBase: {}", companyId, anoBase);

    // Buscar todas contas da empresa
    Page<ContaParteB> contasPage =
        contaParteBRepository.findByCompanyId(companyId, pageable);

    // Filtrar por critérios
    var filteredContas =
        contasPage.getContent().stream()
            .filter(
                conta -> {
                  // Filtro de status
                  if (!Boolean.TRUE.equals(includeInactive)
                      && conta.getStatus() == Status.INACTIVE) {
                    return false;
                  }

                  // Filtro de anoBase
                  if (anoBase != null && !conta.getAnoBase().equals(anoBase)) {
                    return false;
                  }

                  // Filtro de tipoTributo
                  if (tipoTributo != null && conta.getTipoTributo() != tipoTributo) {
                    return false;
                  }

                  // Filtro de search (codigoConta ou descricao)
                  if (search != null && !search.isEmpty()) {
                    String searchLower = search.toLowerCase();
                    boolean matchCode =
                        conta.getCodigoConta() != null
                            && conta.getCodigoConta().toLowerCase().contains(searchLower);
                    boolean matchDesc =
                        conta.getDescricao() != null
                            && conta.getDescricao().toLowerCase().contains(searchLower);
                    return matchCode || matchDesc;
                  }

                  return true;
                })
            .map(dtoMapper::toResponse)
            .toList();

    return new org.springframework.data.domain.PageImpl<>(filteredContas, pageable,
        filteredContas.size());
  }

  @Override
  @Transactional(readOnly = true)
  public ContaParteBResponse getContaParteBById(Long id) {
    log.info("Getting ContaParteB by id: {}", id);

    ContaParteB conta =
        contaParteBRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("ContaParteB not found with id: " + id));

    return dtoMapper.toResponse(conta);
  }

  @Override
  @Transactional
  public ContaParteBResponse updateContaParteB(Long id, UpdateContaParteBRequest request) {
    log.info("Updating ContaParteB with id: {}", id);

    // Buscar conta existente
    ContaParteB conta =
        contaParteBRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("ContaParteB not found with id: " + id));

    // Validar dataVigenciaFim >= dataVigenciaInicio
    if (request.getDataVigenciaFim() != null
        && request.getDataVigenciaFim().isBefore(request.getDataVigenciaInicio())) {
      throw new IllegalArgumentException("dataVigenciaFim must be >= dataVigenciaInicio");
    }

    // Atualizar apenas campos mutáveis (codigoConta e anoBase são imutáveis)
    conta.setDescricao(request.getDescricao());
    conta.setDataVigenciaInicio(request.getDataVigenciaInicio());
    conta.setDataVigenciaFim(request.getDataVigenciaFim());
    conta.setTipoTributo(request.getTipoTributo());
    conta.setSaldoInicial(request.getSaldoInicial());
    conta.setTipoSaldo(request.getTipoSaldo());

    ContaParteB updated = contaParteBRepository.save(conta);
    log.info("ContaParteB updated successfully with id: {}", updated.getId());

    return dtoMapper.toResponse(updated);
  }

  @Override
  @Transactional
  public ToggleStatusResponse toggleStatus(Long id, ToggleStatusRequest request) {
    log.info("Toggling status of ContaParteB id: {} to {}", id, request.getStatus());

    ContaParteB conta =
        contaParteBRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("ContaParteB not found with id: " + id));

    Status oldStatus = conta.getStatus();
    conta.setStatus(request.getStatus());
    contaParteBRepository.save(conta);

    log.info("ContaParteB status toggled from {} to {}", oldStatus, request.getStatus());

    return ToggleStatusResponse.builder()
        .success(true)
        .message("Status alterado com sucesso")
        .newStatus(request.getStatus())
        .build();
  }

  /**
   * Valida ano base.
   *
   * @param anoBase ano base a validar
   */
  private void validateAnoBase(Integer anoBase) {
    int currentYear = Year.now().getValue();
    if (anoBase < 2000 || anoBase > currentYear + 1) {
      throw new IllegalArgumentException(
          String.format(
              "Ano base must be between 2000 and %d (current year + 1)", currentYear + 1));
    }
  }
}
