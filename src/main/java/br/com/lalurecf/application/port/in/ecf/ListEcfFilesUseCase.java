package br.com.lalurecf.application.port.in.ecf;

import br.com.lalurecf.infrastructure.dto.ecf.EcfFileListResponse;

/**
 * Use case de listagem de arquivos ECF por empresa e ano fiscal.
 *
 * <p>Retorna um sumário dos três tipos de arquivo (ARQUIVO_PARCIAL, IMPORTED_ECF, COMPLETE_ECF)
 * para a empresa e ano fiscal informados. Campos são null quando o arquivo ainda não existe.
 * Suporta filtro opcional por fileType.
 */
public interface ListEcfFilesUseCase {

  /**
   * Lista os arquivos ECF de uma empresa para um ano fiscal.
   *
   * @param companyId ID da empresa (extraído do contexto de segurança)
   * @param fiscalYear ano fiscal de referência
   * @param fileType  tipo de arquivo para filtro (nullable — sem filtro retorna os 3 tipos)
   * @return DTO com sumário de cada tipo de arquivo (null se não existir)
   */
  EcfFileListResponse list(Long companyId, Integer fiscalYear, String fileType);
}
