package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.planodecontas.ImportPlanoDeContasUseCase;
import br.com.lalurecf.application.port.out.ContaReferencialRepositoryPort;
import br.com.lalurecf.application.port.out.PlanoDeContasRepositoryPort;
import br.com.lalurecf.domain.enums.AccountType;
import br.com.lalurecf.domain.enums.ClasseContabil;
import br.com.lalurecf.domain.enums.NaturezaConta;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.ContaReferencial;
import br.com.lalurecf.domain.model.PlanoDeContas;
import br.com.lalurecf.domain.util.MascaraNiveisUtils;
import br.com.lalurecf.infrastructure.dto.planodecontas.ImportPlanoDeContasResponse;
import br.com.lalurecf.infrastructure.dto.planodecontas.ImportPlanoDeContasResponse.ImportError;
import br.com.lalurecf.infrastructure.dto.planodecontas.ImportPlanoDeContasResponse.PlanoDeContasPreview;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service para importação de plano de contas via arquivo CSV/TXT.
 *
 * <p>Valida cada linha, busca Conta Referencial RFB por código, e persiste ou retorna preview.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImportPlanoDeContasService implements ImportPlanoDeContasUseCase {

  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
  private static final int CHUNK_SIZE = 1000;

  private final PlanoDeContasRepositoryPort planoDeContasRepository;
  private final ContaReferencialRepositoryPort contaReferencialRepository;

  @Override
  @Transactional
  public ImportPlanoDeContasResponse importPlanoDeContas(
      MultipartFile file, Long companyId, Integer fiscalYear, boolean dryRun) {

    log.info(
        "Importing PlanoDeContas for company {} and fiscalYear {} (dryRun: {})",
        companyId,
        fiscalYear,
        dryRun);

    // Validar arquivo
    if (file.isEmpty()) {
      throw new IllegalArgumentException("File cannot be empty");
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("File size exceeds maximum allowed (10MB)");
    }

    List<ImportError> errors = new ArrayList<>();
    List<PlanoDeContasPreview> preview = dryRun ? new ArrayList<>() : null;
    List<PlanoDeContas> accountsToSave = new ArrayList<>();
    Set<String> processedCodes = new HashSet<>();
    int totalLines = 0;
    int processedLines = 0;

    // Precarregar códigos existentes para este company+year (1 SELECT antes do loop)
    Set<String> existingCodes =
        planoDeContasRepository.findByCompanyIdAndFiscalYear(companyId, fiscalYear).stream()
            .map(PlanoDeContas::getCode)
            .collect(Collectors.toSet());

    // Precarregar contas referenciais por codigoRfb (evita N+1 queries)
    Map<String, ContaReferencial> contasRefByCode =
        contaReferencialRepository.findAll().stream()
            .collect(Collectors.toMap(ContaReferencial::getCodigoRfb, Function.identity(),
                (a, b) -> a));

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

    try (BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(
                    new ByteArrayInputStream(rawBytes, bomOffset, rawBytes.length - bomOffset),
                    charset));
        CSVParser csvParser = createCsvParser(reader, file)) {

      for (CSVRecord record : csvParser) {
        totalLines++;
        int lineNumber = (int) record.getRecordNumber() + 1; // 1-based (conta header)

        try {
          // Parse linha
          ParsedAccountLine parsedLine = parseLine(record, lineNumber);

          // Verificar duplicata dentro do arquivo
          if (processedCodes.contains(parsedLine.code)) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error("Duplicate code in file: " + parsedLine.code)
                    .build());
            continue;
          }

          // Verificar duplicata no banco (Set precarregado — sem roundtrip adicional)
          if (existingCodes.contains(parsedLine.code)) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Account code '"
                            + parsedLine.code
                            + "' already exists for company/year")
                    .build());
            continue;
          }

          // Buscar Conta Referencial por código RFB (lookup em memória)
          Long contaReferencialId = null;
          if (parsedLine.contaReferencialCodigo != null
              && !parsedLine.contaReferencialCodigo.isBlank()) {
            Optional<ContaReferencial> contaReferencialOpt =
                Optional.ofNullable(contasRefByCode.get(parsedLine.contaReferencialCodigo));
            if (contaReferencialOpt.isEmpty()) {
              errors.add(
                  ImportError.builder()
                      .lineNumber(lineNumber)
                      .error(
                          "Conta Referencial '"
                              + parsedLine.contaReferencialCodigo
                              + "' not found")
                      .build());
              continue;
            }
            ContaReferencial contaReferencial = contaReferencialOpt.get();
            if (contaReferencial.getStatus() != Status.ACTIVE) {
              errors.add(
                  ImportError.builder()
                      .lineNumber(lineNumber)
                      .error(
                          "Conta Referencial '"
                              + parsedLine.contaReferencialCodigo
                              + "' is not ACTIVE")
                      .build());
              continue;
            }
            contaReferencialId = contaReferencial.getId();
          }

          // Criar PlanoDeContas
          PlanoDeContas account =
              PlanoDeContas.builder()
                  .companyId(companyId)
                  .code(parsedLine.code)
                  .name(parsedLine.name)
                  .fiscalYear(fiscalYear)
                  .accountType(parsedLine.accountType)
                  .contaReferencialId(contaReferencialId)
                  .classe(parsedLine.classe)
                  .nivel(parsedLine.nivel)
                  .natureza(parsedLine.natureza)
                  .afetaResultado(parsedLine.afetaResultado)
                  .dedutivel(parsedLine.dedutivel)
                  .status(Status.ACTIVE)
                  .build();

          if (dryRun) {
            preview.add(
                PlanoDeContasPreview.builder()
                    .code(parsedLine.code)
                    .name(parsedLine.name)
                    .fiscalYear(fiscalYear)
                    .accountType(parsedLine.accountType)
                    .contaReferencialCodigo(parsedLine.contaReferencialCodigo)
                    .classe(parsedLine.classe)
                    .nivel(parsedLine.nivel)
                    .natureza(parsedLine.natureza)
                    .afetaResultado(parsedLine.afetaResultado)
                    .dedutivel(parsedLine.dedutivel)
                    .build());
          } else {
            accountsToSave.add(account);

            if (accountsToSave.size() >= CHUNK_SIZE) {
              planoDeContasRepository.saveAll(accountsToSave);
              log.info("Persisted chunk of {} contas", accountsToSave.size());
              accountsToSave.clear();
            }
          }

          processedCodes.add(parsedLine.code);
          processedLines++;

        } catch (Exception e) {
          log.warn("Error processing line {}: {}", lineNumber, e.getMessage());
          errors.add(
              ImportError.builder().lineNumber(lineNumber).error(e.getMessage()).build());
        }
      }

      // Persistir chunk final se não for dry-run
      if (!dryRun && !accountsToSave.isEmpty()) {
        planoDeContasRepository.saveAll(accountsToSave);
        log.info("Persisted final chunk of {} contas", accountsToSave.size());
      }

      // Montar response
      boolean success = errors.isEmpty();
      String message =
          dryRun
              ? String.format(
                  "Dry-run completed. %d accounts would be imported, %d errors found",
                  processedLines, errors.size())
              : String.format(
                  "Import completed. %d accounts imported, %d skipped",
                  processedLines, totalLines - processedLines);

      return ImportPlanoDeContasResponse.builder()
          .success(success)
          .message(message)
          .totalLines(totalLines)
          .processedLines(processedLines)
          .skippedLines(totalLines - processedLines)
          .errors(errors)
          .preview(preview)
          .build();

    } catch (Exception e) {
      log.error("Error importing PlanoDeContas: {}", e.getMessage(), e);
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

  private ParsedAccountLine parseLine(CSVRecord record, int lineNumber) {
    if (record.size() < 8) {
      throw new IllegalArgumentException(
          "Linha com menos de 8 colunas (esperado 8)");
    }
    // Extrair campos por posição (header opcional)
    String code = getRequired(record.get(0), "code", lineNumber);
    String name = getRequired(record.get(1), "name", lineNumber);
    String accountTypeStr = getRequired(record.get(2), "accountType", lineNumber);
    String contaReferencialCodigo = normalizeField(record.get(3));
    String classeStr = getRequired(record.get(4), "classe", lineNumber);
    String naturezaStr = getRequired(record.get(5), "natureza", lineNumber);
    String afetaResultadoStr = getRequired(record.get(6), "afetaResultado", lineNumber);
    String dedutivelStr = getRequired(record.get(7), "dedutivel", lineNumber);

    // Parse enums
    AccountType accountType = parseAccountType(accountTypeStr, lineNumber);
    ClasseContabil classe = parseClasseContabil(classeStr, lineNumber);
    NaturezaConta natureza = parseNaturezaConta(naturezaStr, lineNumber);

    // Derivar nivel do code (não é enviado no CSV)
    int nivel = MascaraNiveisUtils.derivarNivel(code);

    // Parse booleans
    Boolean afetaResultado = parseBoolean(afetaResultadoStr, "afetaResultado", lineNumber);
    Boolean dedutivel = parseBoolean(dedutivelStr, "dedutivel", lineNumber);

    return new ParsedAccountLine(
        code,
        name,
        accountType,
        contaReferencialCodigo,
        classe,
        nivel,
        natureza,
        afetaResultado,
        dedutivel);
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

  private AccountType parseAccountType(String value, int lineNumber) {
    try {
      return AccountType.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid accountType: '"
              + value
              + "'. Must be one of: "
              + String.join(", ", getAccountTypeValues()));
    }
  }

  private ClasseContabil parseClasseContabil(String value, int lineNumber) {
    try {
      return ClasseContabil.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid classe: '"
              + value
              + "'. Must be one of: "
              + String.join(", ", getClasseContabilValues()));
    }
  }

  private NaturezaConta parseNaturezaConta(String value, int lineNumber) {
    try {
      return NaturezaConta.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid natureza: '"
              + value
              + "'. Must be one of: "
              + String.join(", ", getNaturezaContaValues()));
    }
  }

  private Boolean parseBoolean(String value, String fieldName, int lineNumber) {
    String normalized = value.toLowerCase().trim();
    return switch (normalized) {
      case "true", "yes", "sim", "1" -> true;
      case "false", "no", "não", "nao", "0" -> false;
      default ->
          throw new IllegalArgumentException(
              "Invalid "
                  + fieldName
                  + ": '"
                  + value
                  + "'. Must be true/false/yes/no/sim/não");
    };
  }

  private List<String> getAccountTypeValues() {
    List<String> values = new ArrayList<>();
    for (AccountType type : AccountType.values()) {
      values.add(type.name());
    }
    return values;
  }

  private List<String> getClasseContabilValues() {
    List<String> values = new ArrayList<>();
    for (ClasseContabil classe : ClasseContabil.values()) {
      values.add(classe.name());
    }
    return values;
  }

  private List<String> getNaturezaContaValues() {
    List<String> values = new ArrayList<>();
    for (NaturezaConta natureza : NaturezaConta.values()) {
      values.add(natureza.name());
    }
    return values;
  }

  /**
   * DTO interno para armazenar dados parseados de uma linha CSV.
   */
  private record ParsedAccountLine(
      String code,
      String name,
      AccountType accountType,
      String contaReferencialCodigo,
      ClasseContabil classe,
      Integer nivel,
      NaturezaConta natureza,
      Boolean afetaResultado,
      Boolean dedutivel) {}
}
