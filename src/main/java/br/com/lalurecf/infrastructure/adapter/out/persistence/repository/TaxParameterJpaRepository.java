package br.com.lalurecf.infrastructure.adapter.out.persistence.repository;

import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA para TaxParameter.
 *
 * <p>Métodos de consulta por código (único) e por tipo (categoria).
 */
@Repository
public interface TaxParameterJpaRepository
    extends JpaRepository<TaxParameterEntity, Long>, JpaSpecificationExecutor<TaxParameterEntity> {

  /**
   * Busca parâmetro tributário por código único.
   *
   * @param codigo código do parâmetro
   * @return Optional contendo o parâmetro se encontrado
   */
  Optional<TaxParameterEntity> findByCodigo(String codigo);

  /**
   * Busca parâmetros tributários por ID do tipo.
   *
   * @param tipoParametroId ID do tipo de parâmetro
   * @return lista de parâmetros do tipo especificado
   */
  List<TaxParameterEntity> findByTipoParametroId(Long tipoParametroId);

  /**
   * Busca parâmetros tributários por IDs e tipo específico. Usado para validar que os IDs
   * fornecidos são do tipo correto.
   *
   * @param ids lista de IDs
   * @param tipoParametroId ID do tipo esperado
   * @return lista de parâmetros que correspondem aos IDs E ao tipo
   */
  @Query("SELECT t FROM TaxParameterEntity t WHERE t.id IN :ids AND t.tipoParametro.id = :typeId")
  List<TaxParameterEntity> findByIdInAndTipoParametroId(
      @Param("ids") List<Long> ids, @Param("typeId") Long tipoParametroId);

  /**
   * Busca tipos/categorias distintos de parâmetros tributários. Útil para popular dropdowns de
   * filtros.
   *
   * @return lista de descrições dos tipos únicos ordenados
   */
  @Query(
      "SELECT DISTINCT t.tipoParametro.descricao FROM TaxParameterEntity t ORDER BY"
          + " t.tipoParametro.descricao")
  List<String> findDistinctTipos();

  /**
   * Busca todos os parâmetros tributários ordenados por tipo e descrição. Útil para agrupar
   * parâmetros por tipo.
   *
   * @return lista de parâmetros ordenados
   */
  @Query(
      "SELECT t FROM TaxParameterEntity t JOIN FETCH t.tipoParametro ORDER BY"
          + " t.tipoParametro.descricao, t.descricao")
  List<TaxParameterEntity> findTaxParametersOrderByType();
}
