package br.com.lalurecf.application.port.in.ecf;

import br.com.lalurecf.domain.enums.EcfFileType;
import br.com.lalurecf.domain.model.EcfFileDownloadData;

/**
 * Port IN para download de arquivo ECF.
 *
 * <p>Busca o arquivo por tipo (unique por empresa+ano+tipo), verifica ownership
 * e retorna o conteúdo em bytes (ISO-8859-1).
 */
public interface DownloadEcfFileUseCase {

  /**
   * Retorna o conteúdo do arquivo ECF para download.
   *
   * @param fileType tipo do arquivo (ARQUIVO_PARCIAL, IMPORTED_ECF, COMPLETE_ECF)
   * @param companyId ID da empresa
   * @param fiscalYear ano fiscal
   * @return dados para download: bytes, fileName, fileSizeBytes
   * @throws jakarta.persistence.EntityNotFoundException se arquivo não encontrado
   */
  EcfFileDownloadData download(EcfFileType fileType, Long companyId, Integer fiscalYear);
}
