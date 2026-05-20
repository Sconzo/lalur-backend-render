package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.taxparametertype.CreateTaxParameterTypeUseCase;
import br.com.lalurecf.application.port.in.taxparametertype.ListTaxParameterTypesUseCase;
import br.com.lalurecf.application.port.out.TaxParameterTypeRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.TaxParameterType;
import br.com.lalurecf.infrastructure.dto.taxparametertype.CreateTaxParameterTypeRequest;
import br.com.lalurecf.infrastructure.dto.taxparametertype.TaxParameterTypeResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementando use cases de TaxParameterType.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxParameterTypeService
    implements CreateTaxParameterTypeUseCase, ListTaxParameterTypesUseCase {

  private final TaxParameterTypeRepositoryPort taxParameterTypeRepository;

  @Override
  @Transactional
  public TaxParameterTypeResponse create(CreateTaxParameterTypeRequest request) {
    log.info("Criando tipo de parâmetro tributário: {}", request.description());

    // Verificar se descrição já existe
    taxParameterTypeRepository
        .findByDescription(request.description())
        .ifPresent(
            existing -> {
              log.warn(
                  "Tentativa de criar tipo com descrição duplicada: {}", request.description());
              throw new IllegalArgumentException(
                  "Já existe um tipo de parâmetro tributário com a descrição: "
                      + request.description());
            });

    // Verificar se ordem de exibição já está em uso
    if (request.displayOrder() != null) {
      taxParameterTypeRepository
          .findByDisplayOrder(request.displayOrder())
          .ifPresent(existing -> {
            log.warn("Tentativa de criar tipo com ordem duplicada: {}", request.displayOrder());
            throw new IllegalArgumentException(
                "Já existe um tipo de parâmetro tributário com a ordem de exibição: "
                    + request.displayOrder());
          });
    }

    // Criar domain model
    TaxParameterType taxParameterType =
        TaxParameterType.builder()
            .description(request.description())
            .nature(request.nature())
            .required(request.required() != null ? request.required() : false)
            .displayOrder(request.displayOrder())
            .fiscalMovementExclusive(
                Boolean.TRUE.equals(request.fiscalMovementExclusive()))
            .status(Status.ACTIVE)
            .build();

    TaxParameterType saved = taxParameterTypeRepository.save(taxParameterType);
    log.info(
        "Tipo de parâmetro tributário criado com sucesso. ID: {}, Descrição: {}",
        saved.getId(),
        saved.getDescription());

    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TaxParameterTypeResponse> listAll() {
    log.info("Listando todos os tipos de parâmetros tributários ativos");

    List<TaxParameterType> types = taxParameterTypeRepository.findAllActive();
    log.info("Encontrados {} tipos de parâmetros tributários ativos", types.size());

    return types.stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<TaxParameterTypeResponse> listForTaxParameters() {
    log.info("Listando tipos de parâmetros tributários ativos (sem exclusivos de lançamentos)");

    List<TaxParameterType> types = taxParameterTypeRepository.findAllActiveNonExclusive();
    log.info("Encontrados {} tipos de parâmetros tributários para uso em empresas", types.size());

    return types.stream().map(this::toResponse).toList();
  }

  /**
   * Converte domain model para DTO de resposta.
   */
  private TaxParameterTypeResponse toResponse(TaxParameterType taxParameterType) {
    return new TaxParameterTypeResponse(
        taxParameterType.getId(),
        taxParameterType.getDescription(),
        taxParameterType.getNature(),
        taxParameterType.getStatus(),
        taxParameterType.getRequired(),
        taxParameterType.getDisplayOrder(),
        taxParameterType.getFiscalMovementExclusive(),
        taxParameterType.getCreatedAt(),
        taxParameterType.getUpdatedAt());
  }
}
