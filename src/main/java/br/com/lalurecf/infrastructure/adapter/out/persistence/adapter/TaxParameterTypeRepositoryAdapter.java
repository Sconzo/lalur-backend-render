package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import br.com.lalurecf.application.port.out.TaxParameterTypeRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.TaxParameterType;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterTypeEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.mapper.TaxParameterTypeMapper;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.TaxParameterTypeJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adapter que implementa TaxParameterTypeRepositoryPort usando JPA.
 *
 * <p>Faz a ponte entre a camada de aplicação (ports) e a infraestrutura (JPA), convertendo entre
 * modelos de domínio e entidades JPA usando MapStruct.
 */
@Component
@RequiredArgsConstructor
public class TaxParameterTypeRepositoryAdapter implements TaxParameterTypeRepositoryPort {

  private final TaxParameterTypeJpaRepository jpaRepository;
  private final TaxParameterTypeMapper mapper;

  @Override
  public TaxParameterType save(TaxParameterType taxParameterType) {
    TaxParameterTypeEntity entity = mapper.toEntity(taxParameterType);
    TaxParameterTypeEntity saved = jpaRepository.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public List<TaxParameterType> findAllActive() {
    return jpaRepository.findByStatusOrderByDescricaoAsc(Status.ACTIVE).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public Optional<TaxParameterType> findByDescription(String description) {
    return jpaRepository.findByDescricao(description).map(mapper::toDomain);
  }

  @Override
  public Optional<TaxParameterType> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<TaxParameterType> findByDisplayOrder(Integer displayOrder) {
    return jpaRepository.findByOrdemExibicao(displayOrder).map(mapper::toDomain);
  }

  @Override
  public List<TaxParameterType> findAllActiveNonExclusive() {
    return jpaRepository
        .findByStatusAndExclusivoLancamentosFalseOrderByDescricaoAsc(Status.ACTIVE)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }
}
