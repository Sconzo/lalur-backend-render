package br.com.lalurecf.application.port.out;

import br.com.lalurecf.domain.enums.AccountType;
import br.com.lalurecf.domain.enums.ClasseContabil;
import br.com.lalurecf.domain.enums.NaturezaConta;
import br.com.lalurecf.domain.model.PlanoDeContas;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port de saída para persistência de PlanoDeContas (Plano de Contas Contábil).
 *
 * <p>Interface de repositório para contas contábeis de empresas por ano fiscal, vinculadas a
 * Contas Referenciais RFB.
 */
public interface PlanoDeContasRepositoryPort {

  /**
   * Salva uma conta contábil (create ou update).
   *
   * @param account conta contábil a salvar
   * @return conta contábil salva com ID gerado
   */
  PlanoDeContas save(PlanoDeContas account);

  /**
   * Busca conta contábil por ID.
   *
   * @param id ID da conta
   * @return Optional com conta se encontrada
   */
  Optional<PlanoDeContas> findById(Long id);

  /**
   * Busca várias contas contábeis por IDs em uma única query (para evitar N+1).
   *
   * @param ids IDs das contas
   * @return lista de contas encontradas
   */
  List<PlanoDeContas> findAllById(Collection<Long> ids);

  /**
   * Busca todas contas contábeis de uma empresa para um ano fiscal.
   *
   * @param companyId ID da empresa
   * @param fiscalYear ano fiscal
   * @return lista de contas da empresa no ano especificado
   */
  List<PlanoDeContas> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear);

  /**
   * Busca conta contábil por empresa, código e ano fiscal.
   *
   * @param companyId ID da empresa
   * @param code código da conta
   * @param fiscalYear ano fiscal
   * @return Optional com conta se encontrada
   */
  Optional<PlanoDeContas> findByCompanyIdAndCodeAndFiscalYear(
      Long companyId, String code, Integer fiscalYear);

  /**
   * Salva uma lista de contas contábeis em batch via JDBC (sem overhead do JPA por linha).
   *
   * @param accounts lista de contas a salvar
   */
  void saveAll(List<PlanoDeContas> accounts);

  /**
   * Deleta conta contábil por ID.
   *
   * @param id ID da conta a deletar
   */
  void deleteById(Long id);

  /**
   * Verifica se existe ao menos uma conta ACTIVE para uma empresa.
   *
   * @param companyId ID da empresa
   * @return true se existir ao menos uma conta ativa
   */
  boolean existsActiveByCompanyId(Long companyId);

  /**
   * Busca todas contas contábeis de uma empresa com paginação.
   *
   * @param companyId ID da empresa
   * @param pageable configuração de paginação
   * @return página de contas da empresa
   */
  Page<PlanoDeContas> findByCompanyId(Long companyId, Pageable pageable);

  /**
   * Busca contas aplicando filtros dinâmicos direto no banco com paginação.
   *
   * @param companyId ID da empresa (obrigatório)
   * @param fiscalYear filtro opcional por ano fiscal
   * @param accountType filtro opcional por tipo de conta
   * @param classe filtro opcional por classe contábil
   * @param natureza filtro opcional por natureza
   * @param search busca opcional em código ou nome (case-insensitive)
   * @param nivel filtro opcional por nível (usado para leafOnly)
   * @param includeInactive se true, inclui contas inativas
   * @param pageable configuração de paginação
   * @return página filtrada de contas
   */
  Page<PlanoDeContas> findFiltered(
      Long companyId,
      Integer fiscalYear,
      AccountType accountType,
      ClasseContabil classe,
      NaturezaConta natureza,
      String search,
      Integer nivel,
      boolean includeInactive,
      Pageable pageable);
}
