package br.com.lalurecf.application.port.in.contaparteb;

import br.com.lalurecf.infrastructure.dto.contaparteb.ContaParteBResponse;
import br.com.lalurecf.infrastructure.dto.contaparteb.UpdateContaParteBRequest;

/**
 * Port IN para caso de uso de atualização de conta da Parte B (e-Lalur/e-Lacs).
 *
 * <p>Define contrato para edição de contas fiscais. Campos imutáveis (codigoConta, anoBase) não
 * podem ser alterados.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface UpdateContaParteBUseCase {

  /**
   * Atualiza dados de uma conta da Parte B.
   *
   * @param id ID da conta a ser atualizada
   * @param request dados atualizados (sem codigoConta e anoBase)
   * @return dados da conta atualizada
   * @throws br.com.lalurecf.infrastructure.exception.ResourceNotFoundException se conta não existir
   * @throws br.com.lalurecf.infrastructure.exception.BusinessRuleViolationException se validações
   *     falharem
   */
  ContaParteBResponse updateContaParteB(Long id, UpdateContaParteBRequest request);
}
