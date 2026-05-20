package br.com.lalurecf.infrastructure.adapter.out.persistence.mapper;

import br.com.lalurecf.domain.model.Company;
import br.com.lalurecf.domain.model.valueobject.CNPJ;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper para conversão entre CompanyEntity e Company (domain).
 *
 * <p>Converte entidade JPA (infraestrutura) para/de modelo de domínio puro.
 * Configuração componentModel = "spring" permite injeção via @Autowired.
 */
@Mapper(componentModel = "spring")
public interface CompanyMapper {

  /**
   * Converte CompanyEntity para Company (domain).
   *
   * @param entity entidade JPA
   * @return modelo de domínio
   */
  @Mapping(target = "cnpj", source = "cnpj", qualifiedByName = "stringToCnpj")
  Company toDomain(CompanyEntity entity);

  /**
   * Converte Company (domain) para CompanyEntity.
   *
   * @param domain modelo de domínio
   * @return entidade JPA
   */
  @Mapping(target = "cnpj", source = "cnpj", qualifiedByName = "cnpjToString")
  CompanyEntity toEntity(Company domain);

  /**
   * Atualiza uma CompanyEntity existente com dados do Company (domain).
   * Usado para operações de UPDATE preservando ID e campos de auditoria.
   *
   * @param domain modelo de domínio com dados atualizados
   * @param entity entidade JPA existente a ser atualizada
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "cnpj", source = "cnpj", qualifiedByName = "cnpjToString")
  void updateEntity(Company domain, @org.mapstruct.MappingTarget CompanyEntity entity);

  /**
   * Converte String para CNPJ value object.
   */
  @Named("stringToCnpj")
  default CNPJ stringToCnpj(String cnpj) {
    return CNPJ.ofRaw(cnpj);
  }

  /**
   * Converte CNPJ value object para String.
   */
  @Named("cnpjToString")
  default String cnpjToString(CNPJ cnpj) {
    return cnpj != null ? cnpj.getValue() : null;
  }
}
