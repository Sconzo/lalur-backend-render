package br.com.lalurecf.application.port.in.company;

import java.util.List;

/**
 * Use case para listar anos selecionáveis para uma empresa, com base no seu Período Contábil.
 */
public interface GetCompanyYearSelectUseCase {

  /**
   * Retorna os anos que podem ser selecionados para a empresa, do Período Contábil até o ano
   * atual.
   *
   * <p>Regras:
   *
   * <ul>
   *   <li>Se o Período Contábil é {@code 31/12/Y}, retorna {@code [Y+1, Y+2, ..., anoAtual]}
   *       (ano Y é excluído).
   *   <li>Caso contrário, sendo {@code X} o ano do Período Contábil, retorna
   *       {@code [X, X+1, X+2, ..., anoAtual]}.
   *   <li>Se o ano inicial for posterior ao ano atual, retorna lista vazia.
   * </ul>
   *
   * @param companyId ID da empresa
   * @return lista de anos em ordem ascendente
   */
  List<Integer> getYearSelect(Long companyId);
}
