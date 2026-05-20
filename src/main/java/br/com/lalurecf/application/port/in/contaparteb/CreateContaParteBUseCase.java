package br.com.lalurecf.application.port.in.contaparteb;

import br.com.lalurecf.infrastructure.dto.contaparteb.ContaParteBResponse;
import br.com.lalurecf.infrastructure.dto.contaparteb.CreateContaParteBRequest;

/**
 * Port IN para caso de uso de criação de conta da Parte B (e-Lalur/e-Lacs).
 *
 * <p>Define contrato para criação de novas contas fiscais específicas de IRPJ/CSLL.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface CreateContaParteBUseCase {

  /**
   * Cria uma nova conta da Parte B para a empresa no contexto.
   *
   * @param request dados da nova conta Parte B
   * @return dados da conta criada
   * @throws br.com.lalurecf.infrastructure.exception.BusinessRuleViolationException se combinação
   *     (company + codigoConta + anoBase) já existe ou se validações de negócio falharem
   */
  ContaParteBResponse createContaParteB(CreateContaParteBRequest request);
}
