package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import br.com.lalurecf.application.port.out.TaxParameterRepositoryPort;
import br.com.lalurecf.domain.model.TaxParameter;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.mapper.TaxParameterMapper;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.TaxParameterJpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Adapter que implementa TaxParameterRepositoryPort usando JPA.
 *
 * <p>Faz a ponte entre a camada de aplicação (ports) e a infraestrutura (JPA),
 * convertendo entre modelos de domínio e entidades JPA usando MapStruct.
 */
@Component
@RequiredArgsConstructor
public class TaxParameterRepositoryAdapter implements TaxParameterRepositoryPort {

  private final TaxParameterJpaRepository jpaRepository;
  private final TaxParameterMapper mapper;

  @Override
  public Optional<TaxParameter> findByCode(String code) {
    return jpaRepository.findByCodigo(code)
        .map(mapper::toDomain);
  }

  @Override
  public TaxParameter save(TaxParameter taxParameter) {
    TaxParameterEntity entity = mapper.toEntity(taxParameter);
    TaxParameterEntity saved = jpaRepository.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<TaxParameter> findById(Long id) {
    return jpaRepository.findById(id)
        .map(mapper::toDomain);
  }

  @Override
  public List<TaxParameter> findAllById(Collection<Long> ids) {
    return jpaRepository.findAllById(ids).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<TaxParameter> findAll() {
    return jpaRepository.findAll().stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public Page<TaxParameter> findAll(Specification specification, Pageable pageable) {
    Page<TaxParameterEntity> page = jpaRepository.findAll(specification, pageable);
    return page.map(mapper::toDomain);
  }

  @Override
  public List<TaxParameter> findByTypeId(Long typeId) {
    return jpaRepository.findByTipoParametroId(typeId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<TaxParameter> findByIdInAndTypeId(List<Long> ids, Long typeId) {
    return jpaRepository.findByIdInAndTipoParametroId(ids, typeId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<String> findDistinctTypes() {
    return jpaRepository.findDistinctTipos();
  }

  @Override
  public List<TaxParameter> findTaxParametersOrderByType() {
    return jpaRepository.findTaxParametersOrderByType().stream()
        .map(mapper::toDomain)
        .toList();
  }
}
