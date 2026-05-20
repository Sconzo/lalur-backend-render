package br.com.lalurecf.infrastructure.adapter.out.persistence.mapper;

import br.com.lalurecf.domain.model.TaxParameter;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct para conversão entre TaxParameter (domain) e TaxParameterEntity (JPA).
 *
 * <p>Mapeia os campos seguindo as convenções:
 * - Domain: camelCase (code, typeId, type, description)
 * - Entity: snake_case no DB via @Column (codigo, tipo_parametro_id, descricao)
 */
@Mapper(componentModel = "spring", uses = TaxParameterTypeMapper.class)
public interface TaxParameterMapper {

  /**
   * Converte TaxParameterEntity (JPA) para TaxParameter (domain).
   *
   * @param entity entidade JPA
   * @return modelo de domínio
   */
  @Mapping(source = "codigo", target = "code")
  @Mapping(source = "tipoParametro.id", target = "typeId")
  @Mapping(source = "tipoParametro", target = "type")
  @Mapping(source = "descricao", target = "description")
  TaxParameter toDomain(TaxParameterEntity entity);

  /**
   * Converte TaxParameter (domain) para TaxParameterEntity (JPA).
   *
   * @param domain modelo de domínio
   * @return entidade JPA
   */
  @Mapping(source = "code", target = "codigo")
  @Mapping(source = "type", target = "tipoParametro")
  @Mapping(source = "description", target = "descricao")
  TaxParameterEntity toEntity(TaxParameter domain);
}
