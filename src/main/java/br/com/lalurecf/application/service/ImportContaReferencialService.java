package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.contareferencial.ImportContaReferencialUseCase;
import br.com.lalurecf.application.port.out.ContaReferencialRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.ContaReferencial;
import br.com.lalurecf.infrastructure.dto.contareferencial.ImportContaReferencialResponse;
import br.com.lalurecf.infrastructure.dto.contareferencial.ImportContaReferencialResponse.ContaReferencialPreview;
import br.com.lalurecf.infrastructure.dto.contareferencial.ImportContaReferencialResponse.ImportError;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service para importação de contas referenciais via arquivo CSV/TXT.
 *
 * <p>Valida cada linha, verifica duplicatas, e persiste ou retorna preview.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImportContaReferencialService implements ImportContaReferencialUseCase {

  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
  private static final int MAX_YEAR = Year.now().getValue() + 5;
  private static final int MIN_YEAR = 2000;

  private final ContaReferencialRepositoryPort contaReferencialRepository;

  @Override
  @Transactional
  public ImportContaReferencialResponse importContasReferenciais(
      MultipartFile file, boolean dryRun) {

    log.info("Importing ContaReferencial (dryRun: {})", dryRun);

    // Validar arquivo
    if (file.isEmpty()) {
      throw new IllegalArgumentException("File cannot be empty");
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("File size exceeds maximum allowed (10MB)");
    }

    // Precarregar chaves existentes no banco (evita N+1 queries)
    Set<String> existingKeys = new HashSet<>();
    for (ContaReferencial existing : contaReferencialRepository.findAll()) {
      existingKeys.add(createUniqueKey(existing.getCodigoRfb(), existing.getAnoValidade()));
    }

    List<ImportError> errors = new ArrayList<>();
    List<ContaReferencialPreview> preview = dryRun ? new ArrayList<>() : null;
    List<ContaReferencial> contasToSave = new ArrayList<>();
    Map<String, Integer> processedKeys = new HashMap<>();
    int totalLines = 0;
    int processedLines = 0;

    // Detectar e remover BOM; se presente, usar UTF-8
    byte[] rawBytes;
    try {
      rawBytes = file.getBytes();
    } catch (Exception e) {
      throw new RuntimeException("Error reading file: " + e.getMessage(), e);
    }
    int bomOffset = 0;
    Charset charset = StandardCharsets.ISO_8859_1;
    if (rawBytes.length >= 3
        && (rawBytes[0] & 0xFF) == 0xEF
        && (rawBytes[1] & 0xFF) == 0xBB
        && (rawBytes[2] & 0xFF) == 0xBF) {
      bomOffset = 3;
      charset = StandardCharsets.UTF_8;
    }

    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                new ByteArrayInputStream(rawBytes, bomOffset, rawBytes.length - bomOffset),
                charset));
        CSVParser csvParser = createCsvParser(reader, file)) {

      for (CSVRecord record : csvParser) {
        totalLines++;
        int lineNumber = (int) record.getRecordNumber() + 1; // 1-based (conta header)

        try {
          // Parse linha
          ParsedContaReferencialLine parsedLine = parseLine(record, lineNumber);

          // Criar chave única (codigoRfb + anoValidade)
          String uniqueKey = createUniqueKey(parsedLine.codigoRfb, parsedLine.anoValidade);

          // Verificar duplicata dentro do arquivo
          if (processedKeys.containsKey(uniqueKey)) {
            int firstOccurrence = processedKeys.get(uniqueKey);
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Duplicate entry in file (codigoRfb='"
                            + parsedLine.codigoRfb
                            + "', anoValidade="
                            + parsedLine.anoValidade
                            + "). First occurrence at line "
                            + firstOccurrence)
                    .build());
            continue;
          }

          // Verificar duplicata no banco (lookup em memória)
          String dbKey = createUniqueKey(parsedLine.codigoRfb, parsedLine.anoValidade);
          if (existingKeys.contains(dbKey)) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "ContaReferencial with codigoRfb='"
                            + parsedLine.codigoRfb
                            + "' and anoValidade="
                            + parsedLine.anoValidade
                            + " already exists")
                    .build());
            continue;
          }

          // Criar ContaReferencial
          ContaReferencial conta =
              ContaReferencial.builder()
                  .codigoRfb(parsedLine.codigoRfb)
                  .descricao(parsedLine.descricao)
                  .anoValidade(parsedLine.anoValidade)
                  .status(Status.ACTIVE)
                  .build();

          if (dryRun) {
            preview.add(
                ContaReferencialPreview.builder()
                    .codigoRfb(parsedLine.codigoRfb)
                    .descricao(parsedLine.descricao)
                    .anoValidade(parsedLine.anoValidade)
                    .build());
          } else {
            contasToSave.add(conta);
          }

          processedKeys.put(uniqueKey, lineNumber);
          processedLines++;

        } catch (Exception e) {
          log.warn("Error processing line {}: {}", lineNumber, e.getMessage());
          errors.add(
              ImportError.builder().lineNumber(lineNumber).error(e.getMessage()).build());
        }
      }

      // Persistir em batch se não for dry-run
      if (!dryRun && !contasToSave.isEmpty()) {
        contaReferencialRepository.saveAll(contasToSave);
      }

      // Montar response
      boolean success = errors.isEmpty();
      String message =
          dryRun
              ? String.format(
                  "Dry-run completed. %d contas referenciais would be imported, %d errors found",
                  processedLines, errors.size())
              : String.format(
                  "Import completed. %d contas referenciais imported, %d skipped",
                  processedLines, totalLines - processedLines);

      return ImportContaReferencialResponse.builder()
          .success(success)
          .message(message)
          .totalLines(totalLines)
          .processedLines(processedLines)
          .skippedLines(totalLines - processedLines)
          .errors(errors)
          .preview(preview)
          .build();

    } catch (Exception e) {
      log.error("Error importing ContaReferencial: {}", e.getMessage(), e);
      throw new RuntimeException("Error importing file: " + e.getMessage(), e);
    }
  }

  private CSVParser createCsvParser(BufferedReader reader, MultipartFile file) throws Exception {
    reader.mark(8192);
    String firstLine = reader.readLine();
    if (firstLine == null) {
      throw new IllegalArgumentException("File is empty");
    }
    char delimiter = firstLine.contains(";") ? ';' : ',';

    reader.reset();

    CSVFormat.Builder builder = CSVFormat.DEFAULT.builder()
        .setDelimiter(delimiter)
        .setIgnoreEmptyLines(true)
        .setTrim(true)
        .setHeader()
        .setSkipHeaderRecord(true);

    return new CSVParser(reader, builder.build());
  }

  private ParsedContaReferencialLine parseLine(CSVRecord record, int lineNumber) {
    if (record.size() < 2) {
      throw new IllegalArgumentException("Linha com menos de 2 colunas (esperado 2-3)");
    }
    // Extrair campos por posição (header opcional)
    String codigoRfb = getRequired(record.get(0), "codigoRfb", lineNumber);
    String descricao = getRequired(record.get(1), "descricao", lineNumber);

    // Campo opcional anoValidade (coluna 3)
    Integer anoValidade = null;
    if (record.size() > 2) {
      String anoValidadeStr = normalizeField(record.get(2));
      if (anoValidadeStr != null) {
        anoValidade = parseAnoValidade(anoValidadeStr, lineNumber);
      }
    }

    // Validar tamanho da descrição
    if (descricao.length() > 1000) {
      throw new IllegalArgumentException(
          "Field 'descricao' exceeds maximum length of 1000 characters");
    }

    return new ParsedContaReferencialLine(codigoRfb, descricao, anoValidade);
  }

  private String normalizeField(String value) {
    return (value == null || value.trim().isEmpty()) ? null : value.trim();
  }

  private String getRequired(String value, String fieldName, int lineNumber) {
    String normalized = normalizeField(value);
    if (normalized == null) {
      throw new IllegalArgumentException(
          "Campo '" + fieldName + "' é obrigatório (coluna vazia na linha " + lineNumber + ")");
    }
    return normalized;
  }

  private Integer parseAnoValidade(String value, int lineNumber) {
    try {
      int ano = Integer.parseInt(value);
      if (ano < MIN_YEAR || ano > MAX_YEAR) {
        throw new IllegalArgumentException(
            "Invalid anoValidade: '"
                + value
                + "'. Must be between "
                + MIN_YEAR
                + " and "
                + MAX_YEAR);
      }
      return ano;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Invalid anoValidade: '" + value + "'. Must be an integer");
    }
  }

  private String createUniqueKey(String codigoRfb, Integer anoValidade) {
    return codigoRfb + "|" + anoValidade;
  }

  /** DTO interno para armazenar dados parseados de uma linha CSV. */
  private record ParsedContaReferencialLine(
      String codigoRfb, String descricao, Integer anoValidade) {}
}
