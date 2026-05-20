package br.com.lalurecf.application.port.in.planodecontas;

import br.com.lalurecf.infrastructure.dto.planodecontas.PlanoDeContasResponse;
import br.com.lalurecf.infrastructure.dto.planodecontas.UpdatePlanoDeContasRequest;

/**
 * Port IN: Use case para atualizar uma conta contábil (PlanoDeContas).
 *
 * <p>Permite editar campos da conta, EXCETO code e fiscalYear.
 */
public interface UpdatePlanoDeContasUseCase {

  /**
   * Atualiza conta contábil existente.
   *
   * @param id ID da conta a atualizar
   * @param request novos dados da conta
   * @return conta atualizada
   */
  PlanoDeContasResponse execute(Long id, UpdatePlanoDeContasRequest request);
}
