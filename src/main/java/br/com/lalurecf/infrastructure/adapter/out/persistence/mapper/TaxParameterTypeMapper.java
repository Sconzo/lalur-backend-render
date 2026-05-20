package br.com.lalurecf.infrastructure.adapter.out.persistence.mapper;

import br.com.lalurecf.domain.model.TaxParameterType;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterTypeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct para conversão entre TaxParameterType (domain) e TaxParameterTypeEntity (JPA).
 *
 * <p>Mapeia os campos seguindo as convenções:
 * - Domain: camelCase (description, nature)
 * - Entity: snake_case no DB via @Column (descricao, natureza)
 */
@Mapper(componentModel = "spring")
public interface TaxParameterTypeMapper {

  /**
   * Converte TaxParameterTypeEntity (JPA) para TaxParameterType (domain).
   *
   * @param entity entidade JPA
   * @return modelo de domínio
   */
  @Mapping(source = "descricao", target = "description")
  @Mapping(source = "natureza", target = "nature")
  @Mapping(source = "obrigatorio", target = "required")
  @Mapping(source = "ordemExibicao", target = "displayOrder")
  @Mapping(source = "exclusivoLancamentos", target = "fiscalMovementExclusive")
  TaxParameterType toDomain(TaxParameterTypeEntity entity);

  /**
   * Converte TaxParameterType (domain) para TaxParameterTypeEntity (JPA).
   *
   * @param domain modelo de domínio
   * @return entidade JPA
   */
  @Mapping(source = "description", target = "descricao")
  @Mapping(source = "nature", target = "natureza")
  @Mapping(source = "required", target = "obrigatorio")
  @Mapping(source = "displayOrder", target = "ordemExibicao")
  @Mapping(source = "fiscalMovementExclusive", target = "exclusivoLancamentos")
  TaxParameterTypeEntity toEntity(TaxParameterType domain);
}
