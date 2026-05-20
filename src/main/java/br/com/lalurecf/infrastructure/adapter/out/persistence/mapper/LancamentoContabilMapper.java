package br.com.lalurecf.infrastructure.adapter.out.persistence.mapper;

import br.com.lalurecf.domain.model.LancamentoContabil;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.LancamentoContabilEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper MapStruct para conversão entre LancamentoContabilEntity e LancamentoContabil.
 *
 * <p>Converte entidades JPA para models de domínio e vice-versa.
 *
 * <p>Configurações:
 *
 * <ul>
 *   <li>componentModel = "spring": integração com Spring DI
 *   <li>unmappedTargetPolicy = IGNORE: ignora campos não mapeados
 * </ul>
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LancamentoContabilMapper {

  /**
   * Converte LancamentoContabilEntity para LancamentoContabil (domain).
   *
   * @param entity entidade JPA
   * @return model de domínio
   */
  @Mapping(source = "company.id", target = "companyId")
  @Mapping(source = "contaDebito.id", target = "contaDebitoId")
  @Mapping(source = "contaDebito.code", target = "contaDebitoCode")
  @Mapping(source = "contaDebito.name", target = "contaDebitoName")
  @Mapping(source = "contaCredito.id", target = "contaCreditoId")
  @Mapping(source = "contaCredito.code", target = "contaCreditoCode")
  @Mapping(source = "contaCredito.name", target = "contaCreditoName")
  LancamentoContabil toDomain(LancamentoContabilEntity entity);

  /**
   * Converte LancamentoContabil (domain) para LancamentoContabilEntity.
   *
   * <p>Nota: FKs (company, contaDebito, contaCredito) não são mapeadas automaticamente
   * e devem ser resolvidas manualmente no adapter.
   *
   * @param domain model de domínio
   * @return entidade JPA
   */
  @Mapping(target = "company", ignore = true)
  @Mapping(target = "contaDebito", ignore = true)
  @Mapping(target = "contaCredito", ignore = true)
  LancamentoContabilEntity toEntity(LancamentoContabil domain);
}
