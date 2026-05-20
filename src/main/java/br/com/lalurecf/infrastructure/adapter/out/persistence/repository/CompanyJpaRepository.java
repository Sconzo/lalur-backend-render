package br.com.lalurecf.infrastructure.adapter.out.persistence.repository;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data JPA repository para CompanyEntity.
 *
 * <p>Estende JpaRepository para operações CRUD básicas,
 * JpaSpecificationExecutor para filtros dinâmicos,
 * e define queries customizadas.
 */
public interface CompanyJpaRepository extends JpaRepository<CompanyEntity, Long>,
    JpaSpecificationExecutor<CompanyEntity> {

  /**
   * Busca empresa por CNPJ.
   *
   * @param cnpj CNPJ da empresa (apenas números, 14 dígitos)
   * @return Optional contendo a entidade se encontrada
   */
  Optional<CompanyEntity> findByCnpj(String cnpj);

  /**
   * Busca empresa ATIVA por CNPJ.
   *
   * @param cnpj CNPJ da empresa
   * @param status status da empresa
   * @return Optional contendo a entidade se encontrada
   */
  Optional<CompanyEntity> findByCnpjAndStatus(String cnpj, Status status);

  /**
   * Busca todas as empresas por status.
   *
   * @param status status das empresas
   * @return lista de entidades com o status especificado
   */
  List<CompanyEntity> findByStatus(Status status);

  /**
   * Retorna lista de CNPJs únicos (apenas empresas ACTIVE).
   *
   * @param status status das empresas
   * @return lista de CNPJs únicos
   */
  @Query("SELECT DISTINCT c.cnpj FROM CompanyEntity c WHERE c.status = :status ORDER BY c.cnpj")
  List<String> findDistinctCnpjsByStatus(Status status);

  /**
   * Retorna lista de Razões Sociais únicas (apenas empresas ACTIVE).
   *
   * @param status status das empresas
   * @return lista de Razões Sociais únicas
   */
  @Query("SELECT DISTINCT c.razaoSocial FROM CompanyEntity c WHERE c.status = :status "
      + "ORDER BY c.razaoSocial")
  List<String> findDistinctRazaoSocialByStatus(Status status);

  /**
   * Retorna lista de CNPJs únicos (todas as empresas, independente do status).
   *
   * @return lista de CNPJs únicos
   */
  @Query("SELECT DISTINCT c.cnpj FROM CompanyEntity c ORDER BY c.cnpj")
  List<String> findDistinctCnpjs();

  /**
   * Retorna lista de Razões Sociais únicas (todas as empresas, independente do status).
   *
   * @return lista de Razões Sociais únicas
   */
  @Query("SELECT DISTINCT c.razaoSocial FROM CompanyEntity c ORDER BY c.razaoSocial")
  List<String> findDistinctRazaoSocial();
}
