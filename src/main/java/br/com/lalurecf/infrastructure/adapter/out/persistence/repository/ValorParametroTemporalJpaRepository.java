package br.com.lalurecf.infrastructure.adapter.out.persistence.repository;

import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ValorParametroTemporalEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository para {@link ValorParametroTemporalEntity}.
 *
 * <p>Fornece operações de CRUD e queries customizadas para gerenciar valores temporais de
 * parâmetros tributários.
 *
 * <p>Métodos de busca:
 *
 * <ul>
 *   <li>Por empresa_parametros_tributarios_id
 *   <li>Por empresa_parametros_tributarios_id + ano
 *   <li>Por empresa_parametros_tributarios_id + ano + mes
 *   <li>Por empresa_parametros_tributarios_id + ano + trimestre
 * </ul>
 */
@Repository
public interface ValorParametroTemporalJpaRepository
    extends JpaRepository<ValorParametroTemporalEntity, Long> {

  /**
   * Busca todos os valores temporais de uma associação empresa-parâmetro.
   *
   * @param empresaParametroId ID da associação em tb_empresa_parametros_tributarios
   * @return lista de valores temporais (vazia se não encontrar)
   */
  List<ValorParametroTemporalEntity> findByEmpresaParametrosTributariosId(Long empresaParametroId);

  /**
   * Busca valores temporais de uma associação empresa-parâmetro filtrados por ano.
   *
   * @param empresaParametroId ID da associação em tb_empresa_parametros_tributarios
   * @param ano ano fiscal
   * @return lista de valores temporais do ano especificado
   */
  List<ValorParametroTemporalEntity> findByEmpresaParametrosTributariosIdAndAno(
      Long empresaParametroId, Integer ano);

  /**
   * Busca valor temporal específico por mês.
   *
   * @param empresaParametroId ID da associação em tb_empresa_parametros_tributarios
   * @param ano ano fiscal
   * @param mes mês (1-12)
   * @return Optional contendo o valor temporal se encontrado
   */
  Optional<ValorParametroTemporalEntity> findByEmpresaParametrosTributariosIdAndAnoAndMes(
      Long empresaParametroId, Integer ano, Integer mes);

  /**
   * Busca valor temporal específico por trimestre.
   *
   * @param empresaParametroId ID da associação em tb_empresa_parametros_tributarios
   * @param ano ano fiscal
   * @param trimestre trimestre (1-4)
   * @return Optional contendo o valor temporal se encontrado
   */
  Optional<ValorParametroTemporalEntity> findByEmpresaParametrosTributariosIdAndAnoAndTrimestre(
      Long empresaParametroId, Integer ano, Integer trimestre);

  /**
   * Busca todos os valores temporais de uma empresa em um ano, com JOIN FETCH para carregar dados
   * do parâmetro tributário.
   *
   * <p>Útil para construir timeline agregada.
   *
   * @param companyId ID da empresa
   * @param ano ano fiscal
   * @return lista de valores temporais com parâmetros carregados
   */
  @Query(
      "SELECT v FROM ValorParametroTemporalEntity v "
          + "JOIN FETCH v.empresaParametrosTributarios ept "
          + "JOIN FETCH ept.parametroTributario p "
          + "JOIN FETCH p.tipoParametro tp "
          + "WHERE ept.empresa.id = :companyId AND v.ano = :ano "
          + "ORDER BY tp.descricao, v.mes, v.trimestre")
  List<ValorParametroTemporalEntity> findByCompanyIdAndAnoWithParameters(
      @Param("companyId") Long companyId, @Param("ano") Integer ano);
}
