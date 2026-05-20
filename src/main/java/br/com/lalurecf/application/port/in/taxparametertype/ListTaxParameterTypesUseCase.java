package br.com.lalurecf.application.port.in.taxparametertype;

import br.com.lalurecf.infrastructure.dto.taxparametertype.TaxParameterTypeResponse;
import java.util.List;

/**
 * Use case para listagem de tipos de parâmetros tributários.
 *
 * <p>Retorna todos os tipos ativos ordenados por descrição.
 */
public interface ListTaxParameterTypesUseCase {

  /**
   * Lista todos os tipos de parâmetros tributários ativos.
   *
   * @return lista de tipos ordenados por descrição
   */
  List<TaxParameterTypeResponse> listAll();

  /**
   * Lista tipos de parâmetros tributários ativos para uso na tela de parâmetros da empresa.
   * Exclui tipos exclusivos de Lançamentos E-LALUR e E-LACS.
   *
   * @return lista de tipos não-exclusivos ativos ordenados por descrição
   */
  List<TaxParameterTypeResponse> listForTaxParameters();
}
