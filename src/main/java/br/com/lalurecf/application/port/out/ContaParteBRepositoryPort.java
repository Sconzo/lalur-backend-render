package br.com.lalurecf.application.port.out;

import br.com.lalurecf.domain.model.ContaParteB;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port de saída para persistência de ContaParteB.
 *
 * <p>Interface de repositório para Contas da Parte B (e-Lalur/e-Lacs), vinculadas a empresas
 * específicas.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface ContaParteBRepositoryPort {

  /**
   * Salva uma conta Parte B (create ou update).
   *
   * @param conta conta Parte B a salvar
   * @return conta Parte B salva com ID gerado
   */
  ContaParteB save(ContaParteB conta);

  /**
   * Busca conta Parte B por ID.
   *
   * @param id ID da conta
   * @return Optional com conta se encontrada
   */
  Optional<ContaParteB> findById(Long id);

  /**
   * Busca várias contas Parte B por IDs em uma única query (para evitar N+1).
   *
   * @param ids IDs das contas
   * @return lista de contas encontradas
   */
  List<ContaParteB> findAllById(Collection<Long> ids);

  /**
   * Busca todas contas Parte B de uma empresa em um ano base.
   *
   * @param companyId ID da empresa
   * @param anoBase ano base
   * @return lista de contas da empresa no ano especificado
   */
  List<ContaParteB> findByCompanyIdAndAnoBase(Long companyId, Integer anoBase);

  /**
   * Busca conta Parte B por empresa, código e ano base.
   *
   * @param companyId ID da empresa
   * @param codigoConta código da conta
   * @param anoBase ano base
   * @return Optional com conta se encontrada
   */
  Optional<ContaParteB> findByCompanyIdAndCodigoContaAndAnoBase(
      Long companyId, String codigoConta, Integer anoBase);

  /**
   * Busca todas contas Parte B de uma empresa com paginação.
   *
   * @param companyId ID da empresa
   * @param pageable configuração de paginação
   * @return página de contas da empresa
   */
  Page<ContaParteB> findByCompanyId(Long companyId, Pageable pageable);
}
