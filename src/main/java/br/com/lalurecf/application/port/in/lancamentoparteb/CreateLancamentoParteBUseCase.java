package br.com.lalurecf.application.port.in.lancamentoparteb;

import br.com.lalurecf.infrastructure.dto.lancamentoparteb.CreateLancamentoParteBRequest;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.LancamentoParteBResponse;

/**
 * Port IN para caso de uso de criação de Lançamento da Parte B.
 *
 * <p>Define contrato para criar ajustes fiscais (adições/exclusões) IRPJ/CSLL com validações
 * condicionais de FK.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface CreateLancamentoParteBUseCase {

  /**
   * Cria um novo lançamento da Parte B.
   *
   * @param request dados do lançamento a criar
   * @return lançamento criado
   * @throws IllegalArgumentException se validações de negócio falharem
   */
  LancamentoParteBResponse createLancamentoParteB(CreateLancamentoParteBRequest request);
}
