package br.com.lalurecf.application.port.in.planodecontas;

import br.com.lalurecf.infrastructure.dto.planodecontas.CreatePlanoDeContasRequest;
import br.com.lalurecf.infrastructure.dto.planodecontas.PlanoDeContasResponse;

/**
 * Port IN: Use case para criar uma conta contábil (PlanoDeContas).
 *
 * <p>Valida empresa via CompanyContext, valida contaReferencialId, verifica unicidade e persiste.
 */
public interface CreatePlanoDeContasUseCase {

  /**
   * Cria uma nova conta contábil para a empresa no contexto.
   *
   * @param request dados da conta a criar
   * @return conta criada
   */
  PlanoDeContasResponse execute(CreatePlanoDeContasRequest request);
}
