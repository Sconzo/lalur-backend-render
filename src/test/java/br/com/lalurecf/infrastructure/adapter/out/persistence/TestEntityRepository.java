package br.com.lalurecf.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository de teste para TestEntity.
 *
 * <p>Usado apenas em testes de integração.
 */
public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {}
