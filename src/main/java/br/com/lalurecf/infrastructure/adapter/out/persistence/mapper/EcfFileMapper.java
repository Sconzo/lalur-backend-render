package br.com.lalurecf.infrastructure.adapter.out.persistence.mapper;

import br.com.lalurecf.domain.model.EcfFile;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.EcfFileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper para conversão entre EcfFileEntity (JPA) e EcfFile (domain).
 *
 * <p>As FK de relacionamento (company, sourceImportedEcf, sourceParcialFile) são mapeadas
 * apenas pelo ID no domínio. A resolução dos objetos relacionados é responsabilidade do adapter.
 */
@Mapper(componentModel = "spring")
public interface EcfFileMapper {

  /**
   * Converte EcfFileEntity para EcfFile (domain).
   *
   * <p>Mapeia company.id → companyId e ids das referências de source.
   *
   * @param entity entidade JPA
   * @return modelo de domínio
   */
  @Mapping(source = "company.id", target = "companyId")
  @Mapping(source = "sourceImportedEcf.id", target = "sourceImportedEcfId")
  @Mapping(source = "sourceParcialFile.id", target = "sourceParcialFileId")
  EcfFile toDomain(EcfFileEntity entity);

  /**
   * Converte EcfFile (domain) para EcfFileEntity.
   *
   * <p>As FKs (company, sourceImportedEcf, sourceParcialFile) são ignoradas aqui
   * e devem ser resolvidas e setadas manualmente no adapter.
   *
   * @param domain modelo de domínio
   * @return entidade JPA
   */
  @Mapping(target = "company", ignore = true)
  @Mapping(target = "sourceImportedEcf", ignore = true)
  @Mapping(target = "sourceParcialFile", ignore = true)
  EcfFileEntity toEntity(EcfFile domain);

  /**
   * Atualiza uma EcfFileEntity existente com dados do EcfFile (domain).
   *
   * <p>Preserva id, company (FK-chave), fileType (chave), fiscalYear (chave)
   * e campos de auditoria imutáveis. FKs de source são gerenciadas pelo adapter.
   *
   * @param domain modelo de domínio com dados atualizados
   * @param entity entidade JPA existente a ser atualizada
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "company", ignore = true)
  @Mapping(target = "fileType", ignore = true)
  @Mapping(target = "fiscalYear", ignore = true)
  @Mapping(target = "sourceImportedEcf", ignore = true)
  @Mapping(target = "sourceParcialFile", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  void updateEntity(EcfFile domain, @org.mapstruct.MappingTarget EcfFileEntity entity);
}
