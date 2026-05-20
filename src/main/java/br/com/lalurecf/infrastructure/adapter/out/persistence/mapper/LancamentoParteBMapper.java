package br.com.lalurecf.infrastructure.adapter.out.persistence.mapper;

import br.com.lalurecf.domain.model.LancamentoParteB;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.LancamentoParteBEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper para conversão entre LancamentoParteBEntity e LancamentoParteB (domain).
 *
 * <p>Converte entidade JPA (infraestrutura) para/de modelo de domínio puro. Configuração
 * componentModel = "spring" permite injeção via @Autowired.
 */
@Mapper(componentModel = "spring")
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface LancamentoParteBMapper {

  /**
   * Converte LancamentoParteBEntity para LancamentoParteB (domain).
   *
   * @param entity entidade JPA
   * @return modelo de domínio
   */
  @Mapping(source = "company.id", target = "companyId")
  @Mapping(source = "contaContabil.id", target = "contaContabilId")
  @Mapping(source = "contaParteB.id", target = "contaParteBId")
  @Mapping(source = "parametroTributario.id", target = "parametroTributarioId")
  LancamentoParteB toDomain(LancamentoParteBEntity entity);

  /**
   * Converte LancamentoParteB (domain) para LancamentoParteBEntity.
   *
   * <p>Nota: Os campos company, contaContabil, contaParteB e parametroTributario não são mapeados
   * automaticamente. Devem ser setados manualmente no adapter.
   *
   * @param domain modelo de domínio
   * @return entidade JPA
   */
  @Mapping(target = "company", ignore = true)
  @Mapping(target = "contaContabil", ignore = true)
  @Mapping(target = "contaParteB", ignore = true)
  @Mapping(target = "parametroTributario", ignore = true)
  LancamentoParteBEntity toEntity(LancamentoParteB domain);

  /**
   * Atualiza uma LancamentoParteBEntity existente com dados do LancamentoParteB (domain). Usado
   * para operações de UPDATE preservando ID, relações de FK e campos de auditoria.
   *
   * @param domain modelo de domínio com dados atualizados
   * @param entity entidade JPA existente a ser atualizada
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "company", ignore = true)
  @Mapping(target = "contaContabil", ignore = true)
  @Mapping(target = "contaParteB", ignore = true)
  @Mapping(target = "parametroTributario", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  void updateEntity(
      LancamentoParteB domain, @org.mapstruct.MappingTarget LancamentoParteBEntity entity);
}
