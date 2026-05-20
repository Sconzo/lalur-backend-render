package br.com.lalurecf.application.port.in.planodecontas;

import br.com.lalurecf.infrastructure.dto.planodecontas.PlanoDeContasResponse;

/**
 * Port IN: Use case para buscar uma conta contábil (PlanoDeContas) por ID.
 *
 * <p>Valida que a conta pertence à empresa no contexto.
 */
public interface GetPlanoDeContasUseCase {

  /**
   * Busca conta contábil por ID.
   *
   * @param id ID da conta
   * @return conta encontrada
   */
  PlanoDeContasResponse execute(Long id);
}
