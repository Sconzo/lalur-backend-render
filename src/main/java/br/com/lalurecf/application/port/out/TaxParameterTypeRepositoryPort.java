package br.com.lalurecf.application.port.out;

import br.com.lalurecf.domain.model.TaxParameterType;
import java.util.List;
import java.util.Optional;

/**
 * Port de saída para operações de repositório de TaxParameterType.
 *
 * <p>Define as operações que a camada de aplicação necessita para persistência de tipos de
 * parâmetros tributários, seguindo os princípios da arquitetura hexagonal.
 */
public interface TaxParameterTypeRepositoryPort {

  /**
   * Salva um tipo de parâmetro tributário.
   *
   * @param taxParameterType tipo a ser salvo
   * @return tipo salvo com ID gerado
   */
  TaxParameterType save(TaxParameterType taxParameterType);

  /**
   * Busca todos os tipos de parâmetros tributários ativos.
   *
   * @return lista de tipos ativos ordenados por descrição
   */
  List<TaxParameterType> findAllActive();

  /**
   * Busca um tipo de parâmetro tributário por descrição.
   *
   * @param description descrição do tipo
   * @return Optional contendo o tipo se encontrado
   */
  Optional<TaxParameterType> findByDescription(String description);

  /**
   * Busca um tipo de parâmetro tributário por ID.
   *
   * @param id ID do tipo
   * @return Optional contendo o tipo se encontrado
   */
  Optional<TaxParameterType> findById(Long id);

  Optional<TaxParameterType> findByDisplayOrder(Integer displayOrder);

  /**
   * Busca todos os tipos de parâmetros tributários ativos que NÃO são exclusivos de lançamentos.
   * Usado na tela de parâmetros tributários da empresa.
   *
   * @return lista de tipos não-exclusivos ativos ordenados por descrição
   */
  List<TaxParameterType> findAllActiveNonExclusive();
}
