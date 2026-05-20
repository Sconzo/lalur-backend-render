package br.com.lalurecf.application.port.out;

import br.com.lalurecf.domain.model.ContaReferencial;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port de saída para persistência de ContaReferencial.
 *
 * <p>Interface de repositório para tabela mestra de Contas Referenciais RFB (global, não
 * vinculada a empresas).
 */
public interface ContaReferencialRepositoryPort {

  /**
   * Salva uma conta referencial (create ou update).
   *
   * @param conta conta referencial a salvar
   * @return conta referencial salva com ID gerado
   */
  ContaReferencial save(ContaReferencial conta);

  /**
   * Busca conta referencial por ID.
   *
   * @param id ID da conta
   * @return Optional com conta se encontrada
   */
  Optional<ContaReferencial> findById(Long id);

  /**
   * Busca conta referencial por código RFB (primeira encontrada se múltiplas versões).
   *
   * @param codigoRfb código oficial RFB
   * @return Optional com conta se encontrada
   */
  Optional<ContaReferencial> findByCodigoRfb(String codigoRfb);

  /**
   * Busca conta referencial por código RFB e ano de validade (chave única).
   *
   * @param codigoRfb código oficial RFB
   * @param anoValidade ano de validade (nullable)
   * @return Optional com conta se encontrada
   */
  Optional<ContaReferencial> findByCodigoRfbAndAnoValidade(
      String codigoRfb, Integer anoValidade);

  /**
   * Busca todas contas referenciais por ano de validade.
   *
   * @param anoValidade ano de validade
   * @return lista de contas do ano especificado
   */
  List<ContaReferencial> findByAnoValidade(Integer anoValidade);

  /**
   * Busca contas referenciais por ano de validade com paginação.
   *
   * @param anoValidade ano de validade
   * @param pageable configuração de paginação
   * @return página de contas do ano especificado
   */
  Page<ContaReferencial> findByAnoValidade(Integer anoValidade, Pageable pageable);

  /**
   * Salva uma lista de contas referenciais em batch via JDBC.
   *
   * @param contas lista de contas a salvar
   */
  void saveAll(List<ContaReferencial> contas);

  /**
   * Busca todas contas referenciais (sem paginação).
   *
   * @return lista completa de contas
   */
  List<ContaReferencial> findAll();

  /**
   * Busca todas contas referenciais com paginação.
   *
   * @param pageable configuração de paginação
   * @return página de contas
   */
  Page<ContaReferencial> findAll(Pageable pageable);

  /**
   * Busca várias contas referenciais por IDs em uma única query (para evitar N+1).
   *
   * @param ids IDs das contas
   * @return lista de contas encontradas
   */
  List<ContaReferencial> findAllById(Collection<Long> ids);

  /**
   * Busca contas referenciais por status com paginação.
   *
   * @param status status a filtrar
   * @param pageable configuração de paginação
   * @return página de contas com o status especificado
   */
  Page<ContaReferencial> findByStatus(
      br.com.lalurecf.domain.enums.Status status, Pageable pageable);

  /**
   * Busca contas referenciais por ano de validade e status.
   *
   * @param anoValidade ano de validade
   * @param status status a filtrar
   * @param pageable configuração de paginação
   * @return página de contas do ano e status especificados
   */
  Page<ContaReferencial> findByAnoValidadeAndStatus(
      Integer anoValidade, br.com.lalurecf.domain.enums.Status status, Pageable pageable);

  /**
   * Busca contas referenciais por termo em codigoRfb ou descricao.
   *
   * @param search termo de busca
   * @param pageable configuração de paginação
   * @return página de contas que contém o termo
   */
  Page<ContaReferencial> findBySearchContaining(String search, Pageable pageable);

  /**
   * Busca contas referenciais por termo e status.
   *
   * @param search termo de busca
   * @param status status a filtrar
   * @param pageable configuração de paginação
   * @return página de contas que contém o termo e tem o status especificado
   */
  Page<ContaReferencial> findBySearchContainingAndStatus(
      String search, br.com.lalurecf.domain.enums.Status status, Pageable pageable);

  /**
   * Busca contas referenciais por termo e ano de validade.
   *
   * @param search termo de busca
   * @param anoValidade ano de validade
   * @param pageable configuração de paginação
   * @return página de contas que contém o termo e são do ano especificado
   */
  Page<ContaReferencial> findBySearchContainingAndAnoValidade(
      String search, Integer anoValidade, Pageable pageable);

  /**
   * Busca contas referenciais por termo, ano de validade e status.
   *
   * @param search termo de busca
   * @param anoValidade ano de validade
   * @param status status a filtrar
   * @param pageable configuração de paginação
   * @return página de contas que atendem todos os critérios
   */
  Page<ContaReferencial> findBySearchContainingAndAnoValidadeAndStatus(
      String search,
      Integer anoValidade,
      br.com.lalurecf.domain.enums.Status status,
      Pageable pageable);
}
