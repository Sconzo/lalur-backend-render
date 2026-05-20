package br.com.lalurecf.infrastructure.adapter.out.persistence.mapper;

import br.com.lalurecf.domain.model.User;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.UserEntity;
import java.util.List;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper para conversão entre UserEntity (JPA) e User (domain).
 *
 * <p>Isola a camada de domínio da camada de infraestrutura, permitindo que o domínio permaneça puro
 * sem dependências de JPA.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

  /**
   * Converte UserEntity para User (domain).
   *
   * @param entity entidade JPA
   * @return domain object
   */
  User toDomain(UserEntity entity);

  /**
   * Converte User (domain) para UserEntity.
   *
   * @param domain domain object
   * @return entidade JPA
   */
  UserEntity toEntity(User domain);

  /**
   * Converte lista de UserEntity para lista de User.
   *
   * @param entities lista de entidades JPA
   * @return lista de domain objects
   */
  List<User> toDomainList(List<UserEntity> entities);
}
