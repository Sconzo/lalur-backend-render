package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import br.com.lalurecf.application.port.out.ContaParteBRepositoryPort;
import br.com.lalurecf.domain.model.ContaParteB;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ContaParteBEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.mapper.ContaParteBMapper;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.ContaParteBJpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Adapter de persistência para ContaParteB.
 *
 * <p>Implementa ContaParteBRepositoryPort (hexagonal port OUT) usando Spring Data JPA como
 * tecnologia de persistência.
 *
 * <p>Responsabilidades:
 *
 * <ul>
 *   <li>Converter entre domain model (ContaParteB) e JPA entity (ContaParteBEntity)
 *   <li>Delegar operações de persistência ao ContaParteBJpaRepository
 *   <li>Resolver relacionamento com CompanyEntity
 * </ul>
 */
@Component
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class ContaParteBRepositoryAdapter implements ContaParteBRepositoryPort {

  private final ContaParteBJpaRepository jpaRepository;
  private final CompanyJpaRepository companyJpaRepository;
  private final ContaParteBMapper mapper;

  @Override
  public ContaParteB save(ContaParteB conta) {
    ContaParteBEntity entity;

    if (conta.getId() != null) {
      // Update: busca entity existente e atualiza seus campos
      entity =
          jpaRepository
              .findById(conta.getId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "ContaParteB not found with id: " + conta.getId()));
      mapper.updateEntity(conta, entity);
    } else {
      // Create: converte domain para nova entity
      entity = mapper.toEntity(conta);

      // Resolver relacionamento com Company
      CompanyEntity company =
          companyJpaRepository
              .findById(conta.getCompanyId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Company not found with id: " + conta.getCompanyId()));
      entity.setCompany(company);
    }

    ContaParteBEntity savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public Optional<ContaParteB> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<ContaParteB> findAllById(Collection<Long> ids) {
    return jpaRepository.findAllById(ids).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<ContaParteB> findByCompanyIdAndAnoBase(Long companyId, Integer anoBase) {
    return jpaRepository.findByCompanyIdAndAnoBase(companyId, anoBase).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<ContaParteB> findByCompanyIdAndCodigoContaAndAnoBase(
      Long companyId, String codigoConta, Integer anoBase) {
    return jpaRepository
        .findByCompanyIdAndCodigoContaAndAnoBase(companyId, codigoConta, anoBase)
        .map(mapper::toDomain);
  }

  @Override
  public Page<ContaParteB> findByCompanyId(Long companyId, Pageable pageable) {
    return jpaRepository.findByCompanyId(companyId, pageable).map(mapper::toDomain);
  }
}
