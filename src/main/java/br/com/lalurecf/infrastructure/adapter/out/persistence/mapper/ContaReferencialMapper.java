package br.com.lalurecf.infrastructure.adapter.out.persistence.mapper;

import br.com.lalurecf.domain.model.ContaReferencial;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ContaReferencialEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper para conversão entre ContaReferencialEntity e ContaReferencial (domain).
 *
 * <p>Converte entidade JPA (infraestrutura) para/de modelo de domínio puro. Configuração
 * componentModel = "spring" permite injeção via @Autowired.
 */
@Mapper(componentModel = "spring")
public interface ContaReferencialMapper {

  /**
   * Converte ContaReferencialEntity para ContaReferencial (domain).
   *
   * @param entity entidade JPA
   * @return modelo de domínio
   */
  ContaReferencial toDomain(ContaReferencialEntity entity);

  /**
   * Converte ContaReferencial (domain) para ContaReferencialEntity.
   *
   * @param domain modelo de domínio
   * @return entidade JPA
   */
  ContaReferencialEntity toEntity(ContaReferencial domain);

  /**
   * Atualiza uma ContaReferencialEntity existente com dados do ContaReferencial (domain). Usado
   * para operações de UPDATE preservando ID e campos de auditoria.
   *
   * @param domain modelo de domínio com dados atualizados
   * @param entity entidade JPA existente a ser atualizada
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  void updateEntity(
      ContaReferencial domain, @org.mapstruct.MappingTarget ContaReferencialEntity entity);
}
