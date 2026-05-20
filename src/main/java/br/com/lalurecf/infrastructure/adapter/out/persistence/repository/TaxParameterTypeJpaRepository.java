package br.com.lalurecf.infrastructure.adapter.out.persistence.repository;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterTypeEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA para TaxParameterType.
 *
 * <p>Métodos de consulta para tipos de parâmetros tributários.
 */
@Repository
public interface TaxParameterTypeJpaRepository extends JpaRepository<TaxParameterTypeEntity, Long> {

  /**
   * Busca tipo de parâmetro tributário por descrição.
   *
   * @param descricao descrição do tipo
   * @return Optional contendo o tipo se encontrado
   */
  Optional<TaxParameterTypeEntity> findByDescricao(String descricao);

  /**
   * Busca todos os tipos de parâmetros tributários por status, ordenados por descrição.
   *
   * @param status status do tipo
   * @return lista de tipos ordenados por descrição
   */
  List<TaxParameterTypeEntity> findByStatusOrderByDescricaoAsc(Status status);

  /**
   * Busca todos os tipos obrigatórios ativos.
   */
  List<TaxParameterTypeEntity> findByObrigatorioTrueAndStatus(Status status);

  Optional<TaxParameterTypeEntity> findByOrdemExibicao(Integer ordemExibicao);

  List<TaxParameterTypeEntity> findByStatusAndExclusivoLancamentosFalseOrderByDescricaoAsc(
      Status status);
}
