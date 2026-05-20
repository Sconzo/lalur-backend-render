package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.contareferencial.CreateContaReferencialUseCase;
import br.com.lalurecf.application.port.in.contareferencial.GetContaReferencialUseCase;
import br.com.lalurecf.application.port.in.contareferencial.ListContaReferencialUseCase;
import br.com.lalurecf.application.port.in.contareferencial.ToggleContaReferencialStatusUseCase;
import br.com.lalurecf.application.port.in.contareferencial.UpdateContaReferencialUseCase;
import br.com.lalurecf.application.port.out.ContaReferencialRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.exception.BusinessRuleViolationException;
import br.com.lalurecf.domain.model.ContaReferencial;
import br.com.lalurecf.infrastructure.dto.contareferencial.ContaReferencialResponse;
import br.com.lalurecf.infrastructure.dto.contareferencial.CreateContaReferencialRequest;
import br.com.lalurecf.infrastructure.dto.contareferencial.UpdateContaReferencialRequest;
import br.com.lalurecf.infrastructure.dto.mapper.ContaReferencialDtoMapper;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusRequest;
import br.com.lalurecf.infrastructure.dto.user.ToggleStatusResponse;
import br.com.lalurecf.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de gerenciamento de contas referenciais RFB.
 *
 * <p>Implementa casos de uso para criar, listar, visualizar, editar e alternar status de contas
 * referenciais da tabela mestra oficial da Receita Federal Brasil.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContaReferencialService
    implements CreateContaReferencialUseCase,
        ListContaReferencialUseCase,
        GetContaReferencialUseCase,
        UpdateContaReferencialUseCase,
        ToggleContaReferencialStatusUseCase {

  private final ContaReferencialRepositoryPort contaReferencialRepository;
  private final ContaReferencialDtoMapper dtoMapper;

  @Override
  @Transactional
  public ContaReferencialResponse createContaReferencial(CreateContaReferencialRequest request) {
    log.debug("Criando conta referencial: codigoRfb={}", request.getCodigoRfb());

    // Validar unicidade (codigoRfb + anoValidade)
    // Se anoValidade for null, busca qualquer conta com mesmo codigoRfb e anoValidade null
    contaReferencialRepository
        .findByAnoValidade(request.getAnoValidade())
        .stream()
        .filter(c -> c.getCodigoRfb().equals(request.getCodigoRfb()))
        .findFirst()
        .ifPresent(
            c -> {
              log.warn(
                  "Tentativa de criar conta referencial com código duplicado: "
                      + "codigoRfb={}, anoValidade={}",
                  request.getCodigoRfb(),
                  request.getAnoValidade());
              throw new BusinessRuleViolationException(
                  "Já existe conta referencial com código "
                      + request.getCodigoRfb()
                      + " para o ano "
                      + request.getAnoValidade());
            });

    // Criar conta referencial
    ContaReferencial conta =
        ContaReferencial.builder()
            .codigoRfb(request.getCodigoRfb())
            .descricao(request.getDescricao())
            .anoValidade(request.getAnoValidade())
            .status(Status.ACTIVE)
            .build();

    ContaReferencial saved = contaReferencialRepository.save(conta);
    log.info(
        "Conta referencial criada com sucesso: id={}, codigoRfb={}",
        saved.getId(),
        saved.getCodigoRfb());

    return dtoMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ContaReferencialResponse> listContasReferenciais(
      String search, Integer anoValidade, Boolean includeInactive, Pageable pageable) {
    log.debug(
        "Listando contas referenciais: search={}, anoValidade={}, includeInactive={}, page={}",
        search,
        anoValidade,
        includeInactive,
        pageable.getPageNumber());

    Page<ContaReferencial> contas;

    boolean hasSearch = search != null && !search.trim().isEmpty();
    boolean hasAnoValidade = anoValidade != null;
    boolean shouldIncludeInactive = includeInactive != null && includeInactive;

    // 8 combinações possíveis de filtros
    if (hasSearch && hasAnoValidade && shouldIncludeInactive) {
      // Busca + ano + incluir inativos
      contas =
          contaReferencialRepository.findBySearchContainingAndAnoValidade(
              search, anoValidade, pageable);
    } else if (hasSearch && hasAnoValidade) {
      // Busca + ano + apenas ACTIVE
      contas =
          contaReferencialRepository.findBySearchContainingAndAnoValidadeAndStatus(
              search, anoValidade, Status.ACTIVE, pageable);
    } else if (hasSearch && shouldIncludeInactive) {
      // Busca + incluir inativos
      contas = contaReferencialRepository.findBySearchContaining(search, pageable);
    } else if (hasSearch) {
      // Busca + apenas ACTIVE
      contas =
          contaReferencialRepository.findBySearchContainingAndStatus(
              search, Status.ACTIVE, pageable);
    } else if (hasAnoValidade && shouldIncludeInactive) {
      // Ano + incluir inativos
      contas = contaReferencialRepository.findByAnoValidade(anoValidade, pageable);
    } else if (hasAnoValidade) {
      // Ano + apenas ACTIVE
      contas =
          contaReferencialRepository.findByAnoValidadeAndStatus(
              anoValidade, Status.ACTIVE, pageable);
    } else if (shouldIncludeInactive) {
      // Incluir inativos (sem outros filtros)
      contas = contaReferencialRepository.findAll(pageable);
    } else {
      // Apenas ACTIVE (padrão)
      contas = contaReferencialRepository.findByStatus(Status.ACTIVE, pageable);
    }

    return contas.map(dtoMapper::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public ContaReferencialResponse getContaReferencialById(Long id) {
    log.debug("Buscando conta referencial por ID: {}", id);

    ContaReferencial conta =
        contaReferencialRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Conta Referencial", id));

    return dtoMapper.toResponse(conta);
  }

  @Override
  @Transactional
  public ContaReferencialResponse updateContaReferencial(
      Long id, UpdateContaReferencialRequest request) {
    log.debug("Atualizando conta referencial: id={}", id);

    ContaReferencial conta =
        contaReferencialRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Conta Referencial", id));

    // Atualizar apenas descricao e anoValidade (codigoRfb é imutável)
    conta.setDescricao(request.getDescricao());
    conta.setAnoValidade(request.getAnoValidade());

    ContaReferencial updated = contaReferencialRepository.save(conta);
    log.info("Conta referencial atualizada com sucesso: id={}", updated.getId());

    return dtoMapper.toResponse(updated);
  }

  @Override
  @Transactional
  public ToggleStatusResponse toggleStatus(Long id, ToggleStatusRequest request) {
    log.debug(
        "Alterando status da conta referencial: id={}, newStatus={}", id, request.getStatus());

    ContaReferencial conta =
        contaReferencialRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Conta Referencial", id));

    conta.setStatus(request.getStatus());
    contaReferencialRepository.save(conta);

    log.info(
        "Status da conta referencial alterado com sucesso: id={}, newStatus={}",
        id,
        request.getStatus());

    return ToggleStatusResponse.builder()
        .success(true)
        .message("Status da conta referencial alterado com sucesso")
        .newStatus(request.getStatus())
        .build();
  }
}
