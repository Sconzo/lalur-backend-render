package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import br.com.lalurecf.application.port.out.UserRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.User;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.UserEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.mapper.UserMapper;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Adapter que implementa UserRepositoryPort usando JPA.
 *
 * <p>Responsável por converter entre domain objects (User) e entidades JPA (UserEntity) usando
 * MapStruct, isolando a camada de domínio da infraestrutura de persistência.
 */
@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

  private final UserJpaRepository userJpaRepository;
  private final UserMapper userMapper;

  @Override
  public Optional<User> findByEmail(String email) {
    return userJpaRepository.findByEmail(email).map(userMapper::toDomain);
  }

  @Override
  public User save(User user) {
    UserEntity entity = userMapper.toEntity(user);
    UserEntity saved = userJpaRepository.save(entity);
    return userMapper.toDomain(saved);
  }

  @Override
  public Optional<User> findById(Long id) {
    return userJpaRepository.findById(id).map(userMapper::toDomain);
  }

  @Override
  public List<User> findAll() {
    List<UserEntity> entities = userJpaRepository.findAll();
    return userMapper.toDomainList(entities);
  }

  @Override
  public Page<User> findAll(Pageable pageable) {
    Page<UserEntity> entities = userJpaRepository.findAll(pageable);
    return entities.map(userMapper::toDomain);
  }

  @Override
  public Page<User> findByStatus(Status status, Pageable pageable) {
    Page<UserEntity> entities = userJpaRepository.findByStatus(status, pageable);
    return entities.map(userMapper::toDomain);
  }

  @Override
  public Page<User> findByNameContaining(String search, Pageable pageable) {
    Page<UserEntity> entities =
        userJpaRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            search, search, pageable);
    return entities.map(userMapper::toDomain);
  }

  @Override
  public Page<User> findByNameContainingAndStatus(String search, Status status, Pageable pageable) {
    Page<UserEntity> entities =
        userJpaRepository
            .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseAndStatus(
                search, search, status, pageable);
    return entities.map(userMapper::toDomain);
  }
}
