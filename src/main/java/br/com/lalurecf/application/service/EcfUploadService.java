package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.ecf.UploadImportedEcfUseCase;
import br.com.lalurecf.application.port.out.CompanyRepositoryPort;
import br.com.lalurecf.application.port.out.EcfFileRepositoryPort;
import br.com.lalurecf.domain.enums.EcfFileStatus;
import br.com.lalurecf.domain.enums.EcfFileType;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.Company;
import br.com.lalurecf.domain.model.EcfFile;
import br.com.lalurecf.infrastructure.dto.ecf.UploadImportedEcfResponse;
import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por validar e armazenar o ECF Importado.
 *
 * <p>Valida extensão, tamanho, formato SPED, presença do bloco M e ano fiscal.
 * Lê o conteúdo em ISO-8859-1 (LATIN-1) conforme padrão SPED ECF.
 * Persiste via upsert e rebaixa COMPLETE_ECF existente para DRAFT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EcfUploadService implements UploadImportedEcfUseCase {

  private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024; // 50MB

  private final EcfFileRepositoryPort ecfFileRepositoryPort;
  private final CompanyRepositoryPort companyRepositoryPort;

  @Override
  @Transactional
  public UploadImportedEcfResponse upload(
      byte[] fileContent, String originalFileName,
      Integer fiscalYear, Long companyId, String generatedBy, boolean overwrite) {

    log.info("Upload ECF Importado: companyId={}, fiscalYear={}, file={}, overwrite={}",
        companyId, fiscalYear, originalFileName, overwrite);

    // Passo 1: validar extensão
    if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".txt")) {
      throw new IllegalArgumentException("O arquivo deve ter extensão .txt");
    }

    // Passo 2: validar tamanho
    if (fileContent.length > MAX_FILE_SIZE_BYTES) {
      throw new IllegalArgumentException("O arquivo excede o tamanho máximo de 50MB");
    }

    // Passo 3: verificar se já existe e respeitar flag overwrite
    Optional<EcfFile> existing = ecfFileRepositoryPort
        .findByCompanyAndFiscalYearAndType(companyId, fiscalYear, EcfFileType.IMPORTED_ECF);

    if (existing.isPresent() && !overwrite) {
      return new UploadImportedEcfResponse(
          false,
          String.format(
              "Já existe uma ECF importada para o ano %d. "
                  + "Deseja sobrescrever? Envie novamente com overwrite=true.",
              fiscalYear),
          existing.get().getId(),
          existing.get().getFileName(),
          null,
          null);
    }

    // Passo 4: converter para String ISO-8859-1
    String content = new String(fileContent, StandardCharsets.ISO_8859_1);

    // Passo 5: varredura única — valida SPED, |0000|, |M001| e conta linhas
    ParseResult parsed = parseAndValidate(content, fiscalYear);

    // Passo 6: construir EcfFile
    Company company = companyRepositoryPort.findById(companyId)
        .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada: " + companyId));

    String cnpj = company.getCnpj() != null ? company.getCnpj().getValue() : companyId.toString();
    String fileName = String.format("ECF_Importado_%d_%s.txt", fiscalYear, cnpj);

    EcfFile ecfFile = EcfFile.builder()
        .fileType(EcfFileType.IMPORTED_ECF)
        .companyId(companyId)
        .fiscalYear(fiscalYear)
        .content(content)
        .fileName(fileName)
        .fileStatus(EcfFileStatus.DRAFT)
        .generatedAt(LocalDateTime.now())
        .generatedBy(generatedBy)
        .status(Status.ACTIVE)
        .build();

    // Passo 7: persistir (upsert)
    EcfFile saved = ecfFileRepositoryPort.saveOrReplace(ecfFile);
    log.info("ECF Importado salvo: id={}, fileName={}", saved.getId(), saved.getFileName());

    // Passo 8: rebaixar COMPLETE_ECF existente para DRAFT
    ecfFileRepositoryPort
        .findByCompanyAndFiscalYearAndType(companyId, fiscalYear, EcfFileType.COMPLETE_ECF)
        .filter(ecf -> ecf.getFileStatus() == EcfFileStatus.VALIDATED
            || ecf.getFileStatus() == EcfFileStatus.FINALIZED)
        .ifPresent(ecf -> {
          ecf.setFileStatus(EcfFileStatus.DRAFT);
          ecf.setValidationErrors(null);
          ecfFileRepositoryPort.saveOrReplace(ecf);
          log.info("COMPLETE_ECF {} rebaixado para DRAFT após upload ECF Importado",
              ecf.getId());
        });

    String message = existing.isPresent()
        ? "ECF Importado sobrescrito com sucesso"
        : "ECF Importado armazenado com sucesso";

    return new UploadImportedEcfResponse(
        true, message, saved.getId(), saved.getFileName(),
        (long) fileContent.length, parsed.lineCount);
  }

  /**
   * Varredura única do conteúdo: valida formato SPED (50 primeiras linhas),
   * extrai e valida ano fiscal do |0000|, verifica presença de |M001| e conta linhas.
   */
  private ParseResult parseAndValidate(String content, Integer fiscalYear) {
    int lineCount = 0;
    boolean found0000 = false;
    boolean foundM001 = false;

    try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
      String line;
      while ((line = reader.readLine()) != null) {
        lineCount++;

        if (!line.isBlank()) {
          if (lineCount <= 50
              && (!line.startsWith("|") || !line.endsWith("|"))) {
            throw new IllegalArgumentException(
                "O arquivo não está no formato SPED "
                    + "(linhas devem iniciar e terminar com |)");
          }

          if (!found0000 && line.startsWith("|0000|")) {
            found0000 = true;
            validateFiscalYearFromLine(line, fiscalYear);
          }

          if (!foundM001 && line.contains("|M001|")) {
            foundM001 = true;
          }
        }
      }
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Erro ao ler conteúdo do arquivo: " + e.getMessage(), e);
    }

    if (!found0000) {
      throw new IllegalArgumentException(
          "O arquivo não contém o registro |0000| obrigatório");
    }
    if (!foundM001) {
      throw new IllegalArgumentException(
          "O arquivo não contém bloco M (|M001| não encontrado)");
    }

    return new ParseResult(lineCount);
  }

  private void validateFiscalYearFromLine(String line, Integer fiscalYear) {
    // Layout SPED ECF |0000|:
    // |0000|LECF|CODE_VER|CNPJ|NOME|IND_SIT_INI_PER|SIT_ESPECIAL|
    //   PAT_REM_PJ|DT_SIT_ESP|DT_INI|DT_FIN|...
    // fields[0]="" fields[1]="0000" ... fields[10]=DT_INI (DDMMAAAA)
    // fields[11]=DT_FIN (DDMMAAAA)
    String[] fields = line.split("\\|", -1);
    if (fields.length < 12 || fields[11].length() < 8) {
      throw new IllegalArgumentException(
          "Registro |0000| com formato inválido — "
              + "não foi possível extrair o ano fiscal");
    }

    String dataFim = fields[11];
    int anoArquivo;
    try {
      anoArquivo = Integer.parseInt(dataFim.substring(4, 8));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Registro |0000| contém data final inválida: " + dataFim);
    }

    if (anoArquivo != fiscalYear) {
      throw new IllegalArgumentException(
          String.format(
              "O ano fiscal do arquivo (%d) não corresponde "
                  + "ao ano fiscal selecionado (%d)",
              anoArquivo, fiscalYear));
    }
  }

  private record ParseResult(int lineCount) {}
}
