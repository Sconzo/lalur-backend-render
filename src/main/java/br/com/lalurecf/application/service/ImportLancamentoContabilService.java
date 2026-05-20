package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.ImportLancamentoContabilUseCase;
import br.com.lalurecf.application.port.out.CompanyRepositoryPort;
import br.com.lalurecf.application.port.out.LancamentoContabilRepositoryPort;
import br.com.lalurecf.application.port.out.PlanoDeContasRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.Company;
import br.com.lalurecf.domain.model.LancamentoContabil;
import br.com.lalurecf.domain.model.PlanoDeContas;
import br.com.lalurecf.infrastructure.dto.lancamentocontabil.ImportLancamentoContabilResponse;
import br.com.lalurecf.infrastructure.dto.lancamentocontabil.ImportLancamentoContabilResponse.ImportError;
import br.com.lalurecf.infrastructure.dto.lancamentocontabil.ImportLancamentoContabilResponse.LancamentoContabilPreview;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * Service para importação de lançamentos contábeis via arquivo CSV/TXT.
 *
 * <p>Implementa parsing com auto-detecção de separador, validação de partidas dobradas e validação
 * de Período Contábil.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImportLancamentoContabilService implements ImportLancamentoContabilUseCase {

  private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
  private static final int CHUNK_SIZE = 1000;
  private static final DateTimeFormatter DATE_FORMATTER_ISO = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter DATE_FORMATTER_BR =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final LancamentoContabilRepositoryPort lancamentoContabilRepository;
  private final PlanoDeContasRepositoryPort planoDeContasRepository;
  private final CompanyRepositoryPort companyRepository;

  @Override
  @Transactional
  public ImportLancamentoContabilResponse importLancamentos(
      MultipartFile file, Long companyId, Integer fiscalYear, boolean dryRun) {

    log.info(
        "Starting import of Lançamentos Contábeis for company {} and fiscalYear {} (dryRun: {})",
        companyId,
        fiscalYear,
        dryRun);

    // Validar tamanho do arquivo
    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException(
          "File size exceeds maximum allowed (50MB). Current size: " + file.getSize() + " bytes");
    }

    // Validar arquivo não vazio
    if (file.isEmpty()) {
      throw new IllegalArgumentException("File is empty");
    }

    // Buscar company para validar Período Contábil
    Company company =
        companyRepository
            .findById(companyId)
            .orElseThrow(
                () -> new IllegalArgumentException("Company not found with id: " + companyId));

    // Carregar todas as contas da empresa/ano de uma vez (evita N+1 queries)
    Map<String, PlanoDeContas> contasByCode =
        planoDeContasRepository.findByCompanyIdAndFiscalYear(companyId, fiscalYear).stream()
            .collect(Collectors.toMap(PlanoDeContas::getCode, Function.identity(),
                (a, b) -> a));

    log.info("Loaded {} contas for company {} / fiscalYear {}", contasByCode.size(),
        companyId, fiscalYear);

    List<ImportError> errors = new ArrayList<>();
    List<LancamentoContabil> lancamentosToSave = new ArrayList<>();
    List<LancamentoContabilPreview> previews = new ArrayList<>();
    int lineNumber = 0;
    int processedLines = 0;
    int skippedLines = 0;

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
        lineNumber++;

        try {
          // Extrair campos por posição (header opcional)
          if (record.size() < 5) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error("Linha com menos de 5 colunas (esperado 6)")
                    .build());
            skippedLines++;
            continue;
          }
          final String contaDebitoCode = normalizeField(record.get(0));
          final String contaCreditoCode = normalizeField(record.get(1));
          final String dataStr = record.get(2) == null ? null : record.get(2).trim();
          final String valorStr = record.get(3) == null ? null : record.get(3).trim();
          final String historico = record.get(4) == null ? null : record.get(4).trim();
          final String numeroDocumento =
              record.size() > 5 ? normalizeField(record.get(5)) : null;

          // Validar campos obrigatórios
          if (dataStr == null || dataStr.isEmpty()) {
            errors.add(ImportError.builder().lineNumber(lineNumber)
                .error("Campo 'data' é obrigatório").build());
            skippedLines++;
            continue;
          }
          if (valorStr == null || valorStr.isEmpty()) {
            errors.add(ImportError.builder().lineNumber(lineNumber)
                .error("Campo 'valor' é obrigatório").build());
            skippedLines++;
            continue;
          }
          if (historico == null || historico.isEmpty()) {
            errors.add(ImportError.builder().lineNumber(lineNumber)
                .error("Campo 'historico' é obrigatório").build());
            skippedLines++;
            continue;
          }

          // Validar ao menos uma conta informada
          if ((contaDebitoCode == null || contaDebitoCode.isEmpty())
              && (contaCreditoCode == null || contaCreditoCode.isEmpty())) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Ao menos contaDebitoCode ou contaCreditoCode deve ser informado")
                    .build());
            skippedLines++;
            continue;
          }

          // Validar contas diferentes (só quando ambas informadas)
          if (contaDebitoCode != null && contaDebitoCode.equals(contaCreditoCode)) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error("Debit and credit accounts must be different")
                    .build());
            skippedLines++;
            continue;
          }

          // Buscar contas no cache em memória (sem query por linha)
          Optional<PlanoDeContas> contaDebitoOpt = Optional.empty();
          if (contaDebitoCode != null && !contaDebitoCode.isEmpty()) {
            contaDebitoOpt = Optional.ofNullable(contasByCode.get(contaDebitoCode));
            if (contaDebitoOpt.isEmpty()) {
              errors.add(
                  ImportError.builder()
                      .lineNumber(lineNumber)
                      .error("Account code '" + contaDebitoCode + "' not found for company/year")
                      .build());
              skippedLines++;
              continue;
            }
          }

          Optional<PlanoDeContas> contaCreditoOpt = Optional.empty();
          if (contaCreditoCode != null && !contaCreditoCode.isEmpty()) {
            contaCreditoOpt = Optional.ofNullable(contasByCode.get(contaCreditoCode));
            if (contaCreditoOpt.isEmpty()) {
              errors.add(
                  ImportError.builder()
                      .lineNumber(lineNumber)
                      .error("Account code '" + contaCreditoCode + "' not found for company/year")
                      .build());
              skippedLines++;
              continue;
            }
          }

          // Parse data (aceita YYYY-MM-DD ou dd/MM/yyyy)
          LocalDate data;
          try {
            data = dataStr.contains("/")
                ? LocalDate.parse(dataStr, DATE_FORMATTER_BR)
                : LocalDate.parse(dataStr, DATE_FORMATTER_ISO);
          } catch (DateTimeParseException e) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error("Invalid date format. Expected YYYY-MM-DD or dd/MM/yyyy, got: "
                        + dataStr)
                    .build());
            skippedLines++;
            continue;
          }

          // Validar Período Contábil
          if (company.getPeriodoContabil() != null && data.isBefore(company.getPeriodoContabil())) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Data "
                            + dataStr
                            + " is before Período Contábil "
                            + company.getPeriodoContabil())
                    .build());
            skippedLines++;
            continue;
          }

          // Parse valor
          BigDecimal valor;
          try {
            valor = new BigDecimal(valorStr);
            if (valor.compareTo(BigDecimal.ZERO) <= 0) {
              errors.add(
                  ImportError.builder()
                      .lineNumber(lineNumber)
                      .error("Invalid value: must be > 0")
                      .build());
              skippedLines++;
              continue;
            }
          } catch (NumberFormatException e) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error("Invalid value format: " + valorStr)
                    .build());
            skippedLines++;
            continue;
          }

          // Criar lançamento
          LancamentoContabil lancamento =
              LancamentoContabil.builder()
                  .companyId(companyId)
                  .contaDebitoId(contaDebitoOpt.map(PlanoDeContas::getId).orElse(null))
                  .contaCreditoId(contaCreditoOpt.map(PlanoDeContas::getId).orElse(null))
                  .data(data)
                  .valor(valor)
                  .historico(historico)
                  .numeroDocumento(numeroDocumento)
                  .fiscalYear(fiscalYear)
                  .status(Status.ACTIVE)
                  .build();

          if (dryRun) {
            // Adicionar ao preview
            previews.add(
                LancamentoContabilPreview.builder()
                    .contaDebitoCode(contaDebitoCode)
                    .contaCreditoCode(contaCreditoCode)
                    .data(dataStr)
                    .valor(valorStr)
                    .historico(historico)
                    .numeroDocumento(numeroDocumento)
                    .build());
          } else {
            // Adicionar para persistir
            lancamentosToSave.add(lancamento);

            // Flush em chunks para limitar memória
            if (lancamentosToSave.size() >= CHUNK_SIZE) {
              lancamentoContabilRepository.saveAll(lancamentosToSave);
              log.info("Persisted chunk of {} lançamentos contábeis", lancamentosToSave.size());
              lancamentosToSave.clear();
            }
          }

          processedLines++;

        } catch (Exception e) {
          log.error("Error processing line {}: {}", lineNumber, e.getMessage(), e);
          errors.add(
              ImportError.builder()
                  .lineNumber(lineNumber)
                  .error("Unexpected error: " + e.getMessage())
                  .build());
          skippedLines++;
        }
      }

      // Persistir lançamentos restantes (chunk final)
      if (!dryRun && !lancamentosToSave.isEmpty()) {
        lancamentoContabilRepository.saveAll(lancamentosToSave);
        log.info("Persisted final chunk of {} lançamentos contábeis", lancamentosToSave.size());
      }

      // Montar resposta
      boolean success = skippedLines == 0;
      String message =
          success
              ? String.format("Successfully processed %d lines", processedLines)
              : String.format(
                  "Processed %d lines with %d errors", processedLines, skippedLines);

      return ImportLancamentoContabilResponse.builder()
          .success(success)
          .message(message)
          .totalLines(lineNumber)
          .processedLines(processedLines)
          .skippedLines(skippedLines)
          .errors(errors)
          .preview(dryRun ? previews : null)
          .build();

    } catch (Exception e) {
      log.error("Error during import: {}", e.getMessage(), e);
      throw new RuntimeException("Error processing CSV file: " + e.getMessage(), e);
    }
  }

  private String normalizeField(String value) {
    return (value == null || value.trim().isEmpty()) ? null : value.trim();
  }

  /**
   * Cria CSVParser com auto-detecção de separador e header opcional.
   */
  private CSVParser createCsvParser(BufferedReader reader, MultipartFile file) throws Exception {
    reader.mark(8192);

    String firstLine = reader.readLine();
    if (firstLine == null || firstLine.trim().isEmpty()) {
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
}
