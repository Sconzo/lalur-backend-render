package br.com.lalurecf.infrastructure.adapter.out.persistence.mapper;

import br.com.lalurecf.domain.model.PlanoDeContas;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.PlanoDeContasEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper para conversão entre PlanoDeContasEntity e PlanoDeContas (domain).
 *
 * <p>Converte entidade JPA (infraestrutura) para/de modelo de domínio puro.
 */
@Mapper(componentModel = "spring")
public interface PlanoDeContasMapper {

  /**
   * Converte PlanoDeContasEntity para PlanoDeContas (domain).
   *
   * @param entity entidade JPA
   * @return modelo de domínio
   */
  @Mapping(source = "company.id", target = "companyId")
  @Mapping(source = "contaReferencial.id", target = "contaReferencialId")
  PlanoDeContas toDomain(PlanoDeContasEntity entity);

  /**
   * Converte PlanoDeContas (domain) para PlanoDeContasEntity.
   *
   * <p>company e contaReferencial são ignorados aqui e definidos pelo adapter
   * via getReferenceById() para evitar instâncias transientes.
   *
   * @param domain modelo de domínio
   * @return entidade JPA
   */
  @Mapping(target = "company", ignore = true)
  @Mapping(target = "contaReferencial", ignore = true)
  PlanoDeContasEntity toEntity(PlanoDeContas domain);

  /**
   * Atualiza uma PlanoDeContasEntity existente com dados do PlanoDeContas (domain).
   *
   * <p>Usado para operações de UPDATE preservando ID e campos de auditoria.
   *
   * @param domain modelo de domínio com dados atualizados
   * @param entity entidade JPA existente a ser atualizada
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "company", ignore = true)
  @Mapping(target = "contaReferencial", ignore = true)
  void updateEntity(
      PlanoDeContas domain, @org.mapstruct.MappingTarget PlanoDeContasEntity entity);
}
