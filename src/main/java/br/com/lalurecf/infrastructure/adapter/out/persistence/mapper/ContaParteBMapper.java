package br.com.lalurecf.infrastructure.adapter.out.persistence.mapper;

import br.com.lalurecf.domain.model.ContaParteB;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ContaParteBEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper para conversão entre ContaParteBEntity e ContaParteB (domain).
 *
 * <p>Converte entidade JPA (infraestrutura) para/de modelo de domínio puro. Configuração
 * componentModel = "spring" permite injeção via @Autowired.
 */
@Mapper(componentModel = "spring")
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface ContaParteBMapper {

  /**
   * Converte ContaParteBEntity para ContaParteB (domain).
   *
   * @param entity entidade JPA
   * @return modelo de domínio
   */
  @Mapping(source = "company.id", target = "companyId")
  ContaParteB toDomain(ContaParteBEntity entity);

  /**
   * Converte ContaParteB (domain) para ContaParteBEntity.
   *
   * <p>Nota: O campo company não é mapeado automaticamente. Deve ser setado manualmente no adapter.
   *
   * @param domain modelo de domínio
   * @return entidade JPA
   */
  @Mapping(target = "company", ignore = true)
  ContaParteBEntity toEntity(ContaParteB domain);

  /**
   * Atualiza uma ContaParteBEntity existente com dados do ContaParteB (domain). Usado para
   * operações de UPDATE preservando ID, company e campos de auditoria.
   *
   * @param domain modelo de domínio com dados atualizados
   * @param entity entidade JPA existente a ser atualizada
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "company", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  void updateEntity(ContaParteB domain, @org.mapstruct.MappingTarget ContaParteBEntity entity);
}
