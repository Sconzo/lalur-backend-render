package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import br.com.lalurecf.application.port.out.EcfFileRepositoryPort;
import br.com.lalurecf.domain.enums.EcfFileType;
import br.com.lalurecf.domain.model.EcfFile;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.EcfFileEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.mapper.EcfFileMapper;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.EcfFileJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adapter de persistência para EcfFile.
 *
 * <p>Implementa EcfFileRepositoryPort (hexagonal port OUT) usando Spring Data JPA.
 *
 * <p>O método {@link #saveOrReplace} implementa semântica de upsert: busca por
 * (fileType, companyId, fiscalYear) e atualiza se existir, insere se não existir.
 */
@Component
@RequiredArgsConstructor
public class EcfFileRepositoryAdapter implements EcfFileRepositoryPort {

  private final EcfFileJpaRepository jpaRepository;
  private final CompanyJpaRepository companyJpaRepository;
  private final EcfFileMapper mapper;

  @Override
  public EcfFile saveOrReplace(EcfFile ecfFile) {
    Optional<EcfFileEntity> existing = jpaRepository.findByCompanyIdAndFiscalYearAndFileType(
        ecfFile.getCompanyId(), ecfFile.getFiscalYear(), ecfFile.getFileType());

    EcfFileEntity entity;
    if (existing.isPresent()) {
      entity = existing.get();
      entity.setContent(ecfFile.getContent());
      entity.setFileName(ecfFile.getFileName());
      entity.setFileStatus(ecfFile.getFileStatus());
      entity.setValidationErrors(ecfFile.getValidationErrors());
      entity.setGeneratedAt(ecfFile.getGeneratedAt());
      entity.setGeneratedBy(ecfFile.getGeneratedBy());
      entity.setSourceImportedEcf(resolveSourceEcf(ecfFile.getSourceImportedEcfId()));
      entity.setSourceParcialFile(resolveSourceEcf(ecfFile.getSourceParcialFileId()));
    } else {
      entity = mapper.toEntity(ecfFile);
      CompanyEntity company = companyJpaRepository.findById(ecfFile.getCompanyId())
          .orElseThrow(() -> new IllegalArgumentException(
              "Company not found with id: " + ecfFile.getCompanyId()));
      entity.setCompany(company);
      entity.setSourceImportedEcf(resolveSourceEcf(ecfFile.getSourceImportedEcfId()));
      entity.setSourceParcialFile(resolveSourceEcf(ecfFile.getSourceParcialFileId()));
    }

    return mapper.toDomain(jpaRepository.save(entity));
  }

  @Override
  public Optional<EcfFile> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<EcfFile> findByCompanyAndFiscalYear(Long companyId, Integer fiscalYear) {
    return jpaRepository.findByCompanyIdAndFiscalYear(companyId, fiscalYear).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<EcfFile> findByCompanyAndFiscalYearAndType(
      Long companyId, Integer fiscalYear, EcfFileType type) {
    return jpaRepository.findByCompanyIdAndFiscalYearAndFileType(companyId, fiscalYear, type)
        .map(mapper::toDomain);
  }

  private EcfFileEntity resolveSourceEcf(Long id) {
    if (id == null) {
      return null;
    }
    return jpaRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("EcfFile not found with id: " + id));
  }
}
