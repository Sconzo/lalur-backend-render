package br.com.lalurecf.application.port.in.contareferencial;

import br.com.lalurecf.infrastructure.dto.contareferencial.ContaReferencialResponse;
import br.com.lalurecf.infrastructure.dto.contareferencial.CreateContaReferencialRequest;

/**
 * Port IN para caso de uso de criação de conta referencial RFB.
 *
 * <p>Define contrato para criação de novas contas na tabela mestra de contas oficiais da Receita
 * Federal Brasil.
 */
public interface CreateContaReferencialUseCase {

  /**
   * Cria uma nova conta referencial.
   *
   * @param request dados da nova conta referencial
   * @return dados da conta criada
   * @throws br.com.lalurecf.domain.exception.BusinessRuleViolationException se combinação
   *     (codigoRfb + anoValidade) já existe
   */
  ContaReferencialResponse createContaReferencial(CreateContaReferencialRequest request);
}
