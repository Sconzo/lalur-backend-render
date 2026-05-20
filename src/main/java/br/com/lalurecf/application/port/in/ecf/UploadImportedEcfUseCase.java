package br.com.lalurecf.application.port.in.ecf;

import br.com.lalurecf.infrastructure.dto.ecf.UploadImportedEcfResponse;

/**
 * Port IN para upload e armazenamento do ECF Importado.
 *
 * <p>Valida, lê e persiste o arquivo ECF externo como IMPORTED_ECF.
 */
public interface UploadImportedEcfUseCase {

  /**
   * Valida e armazena o ECF importado.
   *
   * @param fileContent bytes do arquivo (encoding ISO-8859-1)
   * @param originalFileName nome original do arquivo (para validar extensão .txt)
   * @param fiscalYear ano fiscal de referência
   * @param companyId ID da empresa
   * @param generatedBy identificador do usuário autenticado
   * @return metadados do arquivo persistido
   * @throws IllegalArgumentException se as validações falharem
   */
  UploadImportedEcfResponse upload(
      byte[] fileContent, String originalFileName,
      Integer fiscalYear, Long companyId, String generatedBy, boolean overwrite);
}
