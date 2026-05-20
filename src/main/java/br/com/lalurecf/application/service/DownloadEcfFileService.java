package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.ecf.DownloadEcfFileUseCase;
import br.com.lalurecf.application.port.out.EcfFileRepositoryPort;
import br.com.lalurecf.domain.enums.EcfFileType;
import br.com.lalurecf.domain.model.EcfFile;
import br.com.lalurecf.domain.model.EcfFileDownloadData;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por preparar o download de arquivos ECF.
 *
 * <p>Busca o EcfFile por tipo (unique por empresa+ano+tipo) e converte o conteúdo String
 * para byte[] usando encoding ISO-8859-1 (padrão SPED ECF).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DownloadEcfFileService implements DownloadEcfFileUseCase {

  private final EcfFileRepositoryPort ecfFileRepositoryPort;

  @Override
  @Transactional(readOnly = true)
  public EcfFileDownloadData download(EcfFileType fileType, Long companyId, Integer fiscalYear) {
    log.info("Download EcfFile: fileType={}, companyId={}, fiscalYear={}",
        fileType, companyId, fiscalYear);

    EcfFile ecfFile = ecfFileRepositoryPort
        .findByCompanyAndFiscalYearAndType(companyId, fiscalYear, fileType)
        .orElseThrow(() -> new EntityNotFoundException(
            String.format("Arquivo ECF do tipo %s não encontrado para empresa %d e ano %d",
                fileType, companyId, fiscalYear)));

    byte[] bytes = ecfFile.getContent().getBytes(StandardCharsets.ISO_8859_1);

    return new EcfFileDownloadData(bytes, ecfFile.getFileName(), bytes.length);
  }
}
