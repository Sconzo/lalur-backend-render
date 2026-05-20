package br.com.lalurecf.application.port.in.planodecontas;

import br.com.lalurecf.domain.enums.AccountType;
import br.com.lalurecf.domain.enums.ClasseContabil;
import br.com.lalurecf.domain.enums.NaturezaConta;
import br.com.lalurecf.infrastructure.dto.planodecontas.PlanoDeContasResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Port IN: Use case para listar contas contábeis (PlanoDeContas) com filtros.
 *
 * <p>Retorna apenas contas da empresa no contexto atual.
 */
public interface ListPlanoDeContasUseCase {

  /**
   * Lista contas contábeis da empresa no contexto com filtros opcionais.
   *
   * @param fiscalYear filtro por ano fiscal (opcional)
   * @param accountType filtro por tipo de conta (opcional)
   * @param classe filtro por classe contábil (opcional)
   * @param natureza filtro por natureza (opcional)
   * @param search busca em code e name (opcional)
   * @param includeInactive incluir contas inativas (default: false)
   * @param leafOnly se true, retorna apenas contas do último nível da máscara (default: false)
   * @param pageable configuração de paginação
   * @return página de contas
   */
  Page<PlanoDeContasResponse> execute(
      Integer fiscalYear,
      AccountType accountType,
      ClasseContabil classe,
      NaturezaConta natureza,
      String search,
      Boolean includeInactive,
      Boolean leafOnly,
      Pageable pageable);
}
