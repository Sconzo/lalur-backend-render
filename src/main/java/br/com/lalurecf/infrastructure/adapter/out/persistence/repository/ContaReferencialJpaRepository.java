package br.com.lalurecf.infrastructure.adapter.out.persistence.repository;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ContaReferencialEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository para ContaReferencialEntity.
 *
 * <p>Query methods customizados para busca por codigoRfb, descricao, anoValidade e status.
 */
@Repository
public interface ContaReferencialJpaRepository
    extends JpaRepository<ContaReferencialEntity, Long> {

  /**
   * Busca conta referencial por código RFB (primeira encontrada).
   *
   * @param codigoRfb código oficial RFB
   * @return Optional com entity se encontrada
   */
  Optional<ContaReferencialEntity> findByCodigoRfb(String codigoRfb);

  /**
   * Busca conta referencial por código RFB e ano de validade (chave única).
   *
   * @param codigoRfb código oficial RFB
   * @param anoValidade ano de validade (nullable)
   * @return Optional com entity se encontrada
   */
  Optional<ContaReferencialEntity> findByCodigoRfbAndAnoValidade(
      String codigoRfb, Integer anoValidade);

  /**
   * Busca todas contas por ano de validade.
   *
   * @param anoValidade ano de validade
   * @return lista de entities
   */
  List<ContaReferencialEntity> findByAnoValidade(Integer anoValidade);

  /**
   * Busca contas por ano de validade com paginação.
   *
   * @param anoValidade ano de validade
   * @param pageable configuração de paginação
   * @return página de contas do ano especificado
   */
  Page<ContaReferencialEntity> findByAnoValidade(Integer anoValidade, Pageable pageable);

  /**
   * Busca contas por ano de validade e status com paginação.
   *
   * @param anoValidade ano de validade
   * @param status status a filtrar
   * @param pageable configuração de paginação
   * @return página de contas do ano e status especificados
   */
  Page<ContaReferencialEntity> findByAnoValidadeAndStatus(
      Integer anoValidade, Status status, Pageable pageable);

  /**
   * Busca contas por status com paginação.
   *
   * @param status status a filtrar
   * @param pageable configuração de paginação
   * @return página de contas com o status especificado
   */
  Page<ContaReferencialEntity> findByStatus(Status status, Pageable pageable);

  /**
   * Busca contas por termo em codigoRfb ou descricao com paginação.
   *
   * @param codigoRfb termo de busca no codigoRfb
   * @param descricao termo de busca na descricao
   * @param pageable configuração de paginação
   * @return página de contas que contém o termo
   */
  Page<ContaReferencialEntity>
      findByCodigoRfbContainingIgnoreCaseOrDescricaoContainingIgnoreCase(
          String codigoRfb, String descricao, Pageable pageable);

  /**
   * Busca contas por termo e status com paginação.
   *
   * @param search termo de busca
   * @param status status a filtrar
   * @param pageable configuração de paginação
   * @return página de contas que contém o termo e tem o status especificado
   */
  @Query(
      "SELECT c FROM ContaReferencialEntity c WHERE "
          + "(LOWER(c.codigoRfb) LIKE LOWER(CONCAT('%', :search, '%')) "
          + "OR LOWER(c.descricao) LIKE LOWER(CONCAT('%', :search, '%'))) "
          + "AND c.status = :status")
  Page<ContaReferencialEntity>
      findByCodigoRfbContainingIgnoreCaseOrDescricaoContainingIgnoreCaseAndStatus(
          @Param("search") String search, @Param("status") Status status, Pageable pageable);

  /**
   * Busca contas por termo e ano de validade com paginação.
   *
   * @param search termo de busca
   * @param anoValidade ano de validade
   * @param pageable configuração de paginação
   * @return página de contas que contém o termo e são do ano especificado
   */
  @Query(
      "SELECT c FROM ContaReferencialEntity c WHERE "
          + "(LOWER(c.codigoRfb) LIKE LOWER(CONCAT('%', :search, '%')) "
          + "OR LOWER(c.descricao) LIKE LOWER(CONCAT('%', :search, '%'))) "
          + "AND c.anoValidade = :anoValidade")
  Page<ContaReferencialEntity>
      findByCodigoRfbContainingIgnoreCaseOrDescricaoContainingIgnoreCaseAndAnoValidade(
          @Param("search") String search,
          @Param("anoValidade") Integer anoValidade,
          Pageable pageable);

  /**
   * Busca contas por termo, ano de validade e status com paginação.
   *
   * @param search termo de busca
   * @param anoValidade ano de validade
   * @param status status a filtrar
   * @param pageable configuração de paginação
   * @return página de contas que atendem todos os critérios
   */
  @Query(
      "SELECT c FROM ContaReferencialEntity c WHERE "
          + "(LOWER(c.codigoRfb) LIKE LOWER(CONCAT('%', :search, '%')) "
          + "OR LOWER(c.descricao) LIKE LOWER(CONCAT('%', :search, '%'))) "
          + "AND c.anoValidade = :anoValidade AND c.status = :status")
  Page<ContaReferencialEntity>
      findByCodigoRfbContainingIgnoreCaseOrDescricaoContainingIgnoreCaseAndAnoValidadeAndStatus(
          @Param("search") String search,
          @Param("anoValidade") Integer anoValidade,
          @Param("status") Status status,
          Pageable pageable);
}
