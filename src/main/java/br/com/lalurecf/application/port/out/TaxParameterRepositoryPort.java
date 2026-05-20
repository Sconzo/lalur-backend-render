package br.com.lalurecf.application.port.out;

import br.com.lalurecf.domain.model.TaxParameter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Port de saída para operações de repositório de TaxParameter.
 *
 * <p>Define as operações que a camada de aplicação necessita
 * para persistência de parâmetros tributários, seguindo os princípios
 * da arquitetura hexagonal.
 */
public interface TaxParameterRepositoryPort {

  /**
   * Busca um parâmetro tributário por código.
   *
   * @param code código único do parâmetro
   * @return Optional contendo o parâmetro se encontrado
   */
  Optional<TaxParameter> findByCode(String code);

  /**
   * Salva um parâmetro tributário.
   *
   * @param taxParameter parâmetro a ser salvo
   * @return parâmetro salvo com ID gerado
   */
  TaxParameter save(TaxParameter taxParameter);

  /**
   * Busca um parâmetro tributário por ID.
   *
   * @param id ID do parâmetro
   * @return Optional contendo o parâmetro se encontrado
   */
  Optional<TaxParameter> findById(Long id);

  /**
   * Busca vários parâmetros tributários por IDs em uma única query (para evitar N+1).
   *
   * @param ids IDs dos parâmetros
   * @return lista de parâmetros encontrados
   */
  List<TaxParameter> findAllById(Collection<Long> ids);

  /**
   * Busca todos os parâmetros tributários.
   *
   * @return lista de todos os parâmetros
   */
  List<TaxParameter> findAll();

  /**
   * Busca parâmetros tributários com paginação e filtros.
   *
   * @param specification especificação de filtros
   * @param pageable configuração de paginação
   * @return página de parâmetros
   */
  Page<TaxParameter> findAll(Specification specification, Pageable pageable);

  /**
   * Busca parâmetros tributários por ID do tipo.
   *
   * @param typeId ID do tipo de parâmetro
   * @return lista de parâmetros do tipo especificado
   */
  List<TaxParameter> findByTypeId(Long typeId);

  /**
   * Busca parâmetros tributários por IDs e tipo específico.
   *
   * @param ids lista de IDs
   * @param typeId ID do tipo esperado
   * @return lista de parâmetros que correspondem aos IDs e ao tipo
   */
  List<TaxParameter> findByIdInAndTypeId(List<Long> ids, Long typeId);

  /**
   * Busca tipos/categorias distintos de parâmetros tributários.
   *
   * @return lista de tipos únicos ordenados
   */
  List<String> findDistinctTypes();


  /**
   * Busca tipos/categorias distintos de parâmetros tributários.
   *
   * @return lista de tipos únicos ordenados
   */
  List<TaxParameter> findTaxParametersOrderByType();
}
