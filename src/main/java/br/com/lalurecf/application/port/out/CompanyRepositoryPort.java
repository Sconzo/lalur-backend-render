package br.com.lalurecf.application.port.out;

import br.com.lalurecf.domain.model.Company;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port OUT para persistência de empresas (Company).
 *
 * <p>Define contrato para operações de repositório de empresas,
 * seguindo padrão Hexagonal Architecture (Ports & Adapters).
 */
public interface CompanyRepositoryPort {

  /**
   * Busca empresa por CNPJ.
   *
   * @param cnpj CNPJ da empresa (apenas números, 14 dígitos)
   * @return Optional contendo a empresa se encontrada
   */
  Optional<Company> findByCnpj(String cnpj);

  /**
   * Salva ou atualiza empresa.
   *
   * @param company empresa a ser salva
   * @return empresa salva com ID gerado
   */
  Company save(Company company);

  /**
   * Busca empresa por ID.
   *
   * @param id ID da empresa
   * @return Optional contendo a empresa se encontrada
   */
  Optional<Company> findById(Long id);

  /**
   * Lista todas as empresas com paginação.
   *
   * @param pageable configuração de paginação
   * @return página de empresas
   */
  Page<Company> findAll(Pageable pageable);
}
