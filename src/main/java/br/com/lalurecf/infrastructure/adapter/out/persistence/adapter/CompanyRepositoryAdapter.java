package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import br.com.lalurecf.application.port.out.CompanyRepositoryPort;
import br.com.lalurecf.domain.model.Company;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.mapper.CompanyMapper;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Adapter de persistência para Company.
 *
 * <p>Implementa CompanyRepositoryPort (hexagonal port OUT)
 * usando Spring Data JPA como tecnologia de persistência.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Converter entre domain model (Company) e JPA entity (CompanyEntity)
 *   <li>Delegar operações de persistência ao CompanyJpaRepository
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class CompanyRepositoryAdapter implements CompanyRepositoryPort {

  private final CompanyJpaRepository jpaRepository;
  private final CompanyMapper mapper;

  @Override
  public Optional<Company> findByCnpj(String cnpj) {
    return jpaRepository.findByCnpj(cnpj)
        .map(mapper::toDomain);
  }

  @Override
  public Company save(Company company) {
    CompanyEntity entity;

    if (company.getId() != null) {
      // Update: busca entity existente e atualiza seus campos
      entity = jpaRepository.findById(company.getId())
          .orElseThrow(() -> new IllegalArgumentException(
              "Company not found with id: " + company.getId()));
      mapper.updateEntity(company, entity);
    } else {
      // Create: converte domain para nova entity
      entity = mapper.toEntity(company);
    }

    CompanyEntity savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public Optional<Company> findById(Long id) {
    return jpaRepository.findById(id)
        .map(mapper::toDomain);
  }

  @Override
  public Page<Company> findAll(Pageable pageable) {
    return jpaRepository.findAll(pageable)
        .map(mapper::toDomain);
  }
}
