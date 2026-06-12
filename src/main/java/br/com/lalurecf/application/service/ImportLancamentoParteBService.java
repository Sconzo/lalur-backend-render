package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.lancamentoparteb.ImportLancamentoParteBUseCase;
import br.com.lalurecf.application.port.out.ContaParteBRepositoryPort;
import br.com.lalurecf.application.port.out.LancamentoParteBRepositoryPort;
import br.com.lalurecf.application.port.out.PlanoDeContasRepositoryPort;
import br.com.lalurecf.application.port.out.TaxParameterRepositoryPort;
import br.com.lalurecf.application.util.CsvCharsetUtils;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.TipoAjuste;
import br.com.lalurecf.domain.enums.TipoApuracao;
import br.com.lalurecf.domain.enums.TipoRelacionamento;
import br.com.lalurecf.domain.model.ContaParteB;
import br.com.lalurecf.domain.model.LancamentoParteB;
import br.com.lalurecf.domain.model.PlanoDeContas;
import br.com.lalurecf.domain.model.TaxParameter;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.ImportLancamentoParteBResponse;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.ImportLancamentoParteBResponse.ImportError;
import br.com.lalurecf.infrastructure.dto.lancamentoparteb.ImportLancamentoParteBResponse.LancamentoParteBPreview;
import br.com.lalurecf.infrastructure.exception.LancamentoParteBImportConflictException;
import br.com.lalurecf.infrastructure.security.FiscalYearContext;
import java.io.BufferedReader;
import java.math.BigDecimal;
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
 * Service para importação de lançamentos da Parte B via arquivo CSV/TXT.
 *
 * <p>Formato CSV esperado (9 colunas):
 * mesReferencia;tipoApuracao;tipoRelacionamento;contaContabilCode;
 * contaParteBCode;parametroTributarioCodigo;tipoAjuste;descricao;valor
 *
 * <p>O anoReferencia vem do header X-Fiscal-Year (FiscalYearContext).
 *
 * <p>Separador: auto-detectado (; ou ,)
 */
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class ImportLancamentoParteBService implements ImportLancamentoParteBUseCase {

  private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
  private static final int CHUNK_SIZE = 1000;

  private final LancamentoParteBRepositoryPort lancamentoParteBRepository;
  private final PlanoDeContasRepositoryPort planoDeContasRepository;
  private final ContaParteBRepositoryPort contaParteBRepository;
  private final TaxParameterRepositoryPort taxParameterRepository;

  @Override
  @Transactional
  public ImportLancamentoParteBResponse importLancamentos(
      MultipartFile file, Long companyId, boolean dryRun, boolean overwrite) {

    log.info(
        "Starting import of LancamentosParteB for company {} (dryRun: {}, overwrite: {})",
        companyId,
        dryRun,
        overwrite);

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException(
          "File size exceeds maximum allowed (50MB). Current size: " + file.getSize() + " bytes");
    }

    if (file.isEmpty()) {
      throw new IllegalArgumentException("File is empty");
    }

    // anoReferencia vem do header X-Fiscal-Year (FiscalYearContext)
    final Integer anoReferencia = FiscalYearContext.getCurrentFiscalYear();
    if (anoReferencia == null) {
      throw new IllegalArgumentException(
          "Fiscal year context is required (header X-Fiscal-Year missing)");
    }

    // Detecção de conflito: se já existem lançamentos para (companyId, anoReferencia),
    // exigir confirmação de sobrescrita (overwrite=true) ou abortar com 409.
    // dryRun ignora a verificação porque nada é persistido.
    if (!dryRun) {
      long existingCount =
          lancamentoParteBRepository.countByCompanyIdAndAnoReferencia(companyId, anoReferencia);
      if (existingCount > 0) {
        if (!overwrite) {
          throw new LancamentoParteBImportConflictException(existingCount, anoReferencia);
        }
        int deleted =
            lancamentoParteBRepository.deleteByCompanyIdAndAnoReferencia(companyId, anoReferencia);
        log.info(
            "Overwrite=true: deleted {} existing LancamentosParteB for company {} year {}",
            deleted,
            companyId,
            anoReferencia);
      }
    }

    // Carregar lookups de uma vez (evita N+1 queries)
    Map<String, PlanoDeContas> contasByCode =
        planoDeContasRepository.findByCompanyIdAndFiscalYear(companyId, anoReferencia).stream()
            .collect(Collectors.toMap(PlanoDeContas::getCode, Function.identity(),
                (a, b) -> a));
    Map<String, ContaParteB> contasParteBByCode =
        contaParteBRepository.findByCompanyIdAndAnoBase(companyId, anoReferencia).stream()
            .collect(Collectors.toMap(ContaParteB::getCodigoConta, Function.identity(),
                (a, b) -> a));
    // Filtra apenas TaxParameters do tipo "CÓDIGOS LANÇAMENTOS E-LALUR E E-LACS"
    // (fiscalMovementExclusive=true). Sem o filtro, codes como "6" e "8" colidem
    // com tipos como "FORMA TRIBUTAÇÃO LUCRO REAL", causando lookup ambíguo.
    Map<String, TaxParameter> taxParamsByCode =
        taxParameterRepository.findAll().stream()
            .filter(p -> p.getType() != null
                && Boolean.TRUE.equals(p.getType().getFiscalMovementExclusive()))
            .collect(Collectors.toMap(TaxParameter::getCode, Function.identity(),
                (a, b) -> a));

    log.info("Loaded {} contas, {} contasParteB, {} taxParams for lookup",
        contasByCode.size(), contasParteBByCode.size(), taxParamsByCode.size());

    List<ImportError> errors = new ArrayList<>();
    List<LancamentoParteB> lancamentosToSave = new ArrayList<>();
    List<LancamentoParteBPreview> previews = new ArrayList<>();
    int lineNumber = 0;
    int processedLines = 0;
    int skippedLines = 0;

    byte[] rawBytes;
    try {
      rawBytes = file.getBytes();
    } catch (Exception e) {
      throw new RuntimeException("Error reading file: " + e.getMessage(), e);
    }

    try (BufferedReader reader = CsvCharsetUtils.newReader(rawBytes);
        CSVParser csvParser = createCsvParser(reader)) {

      for (CSVRecord record : csvParser) {
        lineNumber++;

        try {
          // Extrair campos por posição (header opcional)
          if (record.size() < 9) {
            errors.add(ImportError.builder().lineNumber(lineNumber)
                .error("Linha " + lineNumber + ": tem " + record.size()
                    + " coluna(s), esperado 9").build());
            skippedLines++;
            continue;
          }
          final String mesReferenciaStr = normalizeRequired(record.get(0),
              "mesReferencia", 1, lineNumber);
          final String tipoApuracaoStr = normalizeRequired(record.get(1),
              "tipoApuracao", 2, lineNumber);
          final String tipoRelacionamentoStr = normalizeRequired(record.get(2),
              "tipoRelacionamento", 3, lineNumber);
          final String contaContabilCode = normalizeField(record.get(3));
          final String contaParteBCode = normalizeField(record.get(4));
          final String parametroTributarioCodigo = normalizeRequired(record.get(5),
              "parametroTributarioCodigo", 6, lineNumber);
          final String tipoAjusteStr = normalizeRequired(record.get(6),
              "tipoAjuste", 7, lineNumber);
          final String descricao = normalizeRequired(record.get(7),
              "descricao", 8, lineNumber);
          final String valorStr = normalizeRequired(record.get(8),
              "valor", 9, lineNumber);

          // Parse mesReferencia
          int mesReferencia;
          try {
            mesReferencia = Integer.parseInt(mesReferenciaStr);
            if (mesReferencia < 1 || mesReferencia > 12) {
              errors.add(
                  ImportError.builder()
                      .lineNumber(lineNumber)
                      .error(formatError(lineNumber, 1, "mesReferencia",
                          "valor '" + mesReferenciaStr
                              + "' fora do intervalo permitido (1 a 12)"))
                      .build());
              skippedLines++;
              continue;
            }
          } catch (NumberFormatException e) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(formatError(lineNumber, 1, "mesReferencia",
                        "valor '" + mesReferenciaStr + "' não é um número inteiro válido"))
                    .build());
            skippedLines++;
            continue;
          }

          // Parse tipoApuracao
          TipoApuracao tipoApuracao;
          try {
            tipoApuracao = TipoApuracao.valueOf(tipoApuracaoStr.toUpperCase());
          } catch (IllegalArgumentException e) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(formatError(lineNumber, 2, "tipoApuracao",
                        "valor '" + tipoApuracaoStr + "' inválido. Aceitos: IRPJ, CSLL"))
                    .build());
            skippedLines++;
            continue;
          }

          // Parse tipoRelacionamento
          TipoRelacionamento tipoRelacionamento;
          try {
            tipoRelacionamento = TipoRelacionamento.valueOf(tipoRelacionamentoStr.toUpperCase());
          } catch (IllegalArgumentException e) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(formatError(lineNumber, 3, "tipoRelacionamento",
                        "valor '" + tipoRelacionamentoStr
                            + "' inválido. Aceitos: CONTA_CONTABIL, CONTA_PARTE_B, AMBOS"))
                    .build());
            skippedLines++;
            continue;
          }

          // Parse tipoAjuste
          TipoAjuste tipoAjuste;
          try {
            tipoAjuste = TipoAjuste.valueOf(tipoAjusteStr.toUpperCase());
          } catch (IllegalArgumentException e) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(formatError(lineNumber, 7, "tipoAjuste",
                        "valor '" + tipoAjusteStr + "' inválido. Aceitos: ADICAO, EXCLUSAO"))
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
                      .error(formatError(lineNumber, 9, "valor",
                          "valor '" + valorStr + "' deve ser maior que zero"))
                      .build());
              skippedLines++;
              continue;
            }
          } catch (NumberFormatException e) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(formatError(lineNumber, 9, "valor",
                        "valor '" + valorStr + "' não é um número decimal válido"))
                    .build());
            skippedLines++;
            continue;
          }

          // Validar parâmetro tributário por código (lookup em memória)
          Optional<TaxParameter> parametroOpt =
              Optional.ofNullable(taxParamsByCode.get(parametroTributarioCodigo));
          if (parametroOpt.isEmpty()) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(formatError(lineNumber, 6, "parametroTributarioCodigo",
                        "código '" + parametroTributarioCodigo
                            + "' não encontrado nos Parâmetros Tributários"))
                    .build());
            skippedLines++;
            continue;
          }
          TaxParameter parametro = parametroOpt.get();
          if (parametro.getStatus() != Status.ACTIVE) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(formatError(lineNumber, 6, "parametroTributarioCodigo",
                        "parâmetro '" + parametroTributarioCodigo
                            + "' está INACTIVE (status atual: " + parametro.getStatus() + ")"))
                    .build());
            skippedLines++;
            continue;
          }

          // Validar FKs condicionais conforme tipoRelacionamento
          Long contaContabilId = null;
          Long contaParteBId = null;

          switch (tipoRelacionamento) {
            case CONTA_CONTABIL:
              if (contaContabilCode == null) {
                errors.add(
                    ImportError.builder()
                        .lineNumber(lineNumber)
                        .error(formatError(lineNumber, 4, "contaContabilCode",
                            "valor obrigatório quando tipoRelacionamento = CONTA_CONTABIL"))
                        .build());
                skippedLines++;
                continue;
              }
              Optional<PlanoDeContas> contaContabilOpt =
                  Optional.ofNullable(contasByCode.get(contaContabilCode));
              if (contaContabilOpt.isEmpty()) {
                errors.add(
                    ImportError.builder()
                        .lineNumber(lineNumber)
                        .error(formatError(lineNumber, 4, "contaContabilCode",
                            "código '" + contaContabilCode
                                + "' não encontrado no Plano de Contas para anoReferencia "
                                + anoReferencia))
                        .build());
                skippedLines++;
                continue;
              }
              contaContabilId = contaContabilOpt.get().getId();
              break;

            case CONTA_PARTE_B:
              if (contaParteBCode == null) {
                errors.add(
                    ImportError.builder()
                        .lineNumber(lineNumber)
                        .error(formatError(lineNumber, 5, "contaParteBCode",
                            "valor obrigatório quando tipoRelacionamento = CONTA_PARTE_B"))
                        .build());
                skippedLines++;
                continue;
              }
              Optional<ContaParteB> contaParteBOpt =
                  Optional.ofNullable(contasParteBByCode.get(contaParteBCode));
              if (contaParteBOpt.isEmpty()) {
                errors.add(
                    ImportError.builder()
                        .lineNumber(lineNumber)
                        .error(formatError(lineNumber, 5, "contaParteBCode",
                            "código '" + contaParteBCode
                                + "' não encontrado nas Contas Parte B para anoReferencia "
                                + anoReferencia))
                        .build());
                skippedLines++;
                continue;
              }
              contaParteBId = contaParteBOpt.get().getId();
              break;

            case AMBOS:
              if (contaContabilCode == null) {
                errors.add(
                    ImportError.builder()
                        .lineNumber(lineNumber)
                        .error(formatError(lineNumber, 4, "contaContabilCode",
                            "valor obrigatório quando tipoRelacionamento = AMBOS"))
                        .build());
                skippedLines++;
                continue;
              }
              if (contaParteBCode == null) {
                errors.add(
                    ImportError.builder()
                        .lineNumber(lineNumber)
                        .error(formatError(lineNumber, 5, "contaParteBCode",
                            "valor obrigatório quando tipoRelacionamento = AMBOS"))
                        .build());
                skippedLines++;
                continue;
              }
              Optional<PlanoDeContas> contaContabilAmbosOpt =
                  Optional.ofNullable(contasByCode.get(contaContabilCode));
              if (contaContabilAmbosOpt.isEmpty()) {
                errors.add(
                    ImportError.builder()
                        .lineNumber(lineNumber)
                        .error(formatError(lineNumber, 4, "contaContabilCode",
                            "código '" + contaContabilCode
                                + "' não encontrado no Plano de Contas para anoReferencia "
                                + anoReferencia))
                        .build());
                skippedLines++;
                continue;
              }
              Optional<ContaParteB> contaParteBambosOpt =
                  Optional.ofNullable(contasParteBByCode.get(contaParteBCode));
              if (contaParteBambosOpt.isEmpty()) {
                errors.add(
                    ImportError.builder()
                        .lineNumber(lineNumber)
                        .error(formatError(lineNumber, 5, "contaParteBCode",
                            "código '" + contaParteBCode
                                + "' não encontrado nas Contas Parte B para anoReferencia "
                                + anoReferencia))
                        .build());
                skippedLines++;
                continue;
              }
              contaContabilId = contaContabilAmbosOpt.get().getId();
              contaParteBId = contaParteBambosOpt.get().getId();
              break;

            default:
              errors.add(
                  ImportError.builder()
                      .lineNumber(lineNumber)
                      .error(formatError(lineNumber, 3, "tipoRelacionamento",
                          "valor '" + tipoRelacionamento
                              + "' inválido. Aceitos: CONTA_CONTABIL, CONTA_PARTE_B, AMBOS"))
                      .build());
              skippedLines++;
              continue;
          }

          // Montar domain object
          LancamentoParteB lancamento =
              LancamentoParteB.builder()
                  .companyId(companyId)
                  .mesReferencia(mesReferencia)
                  .anoReferencia(anoReferencia)
                  .tipoApuracao(tipoApuracao)
                  .tipoRelacionamento(tipoRelacionamento)
                  .contaContabilId(contaContabilId)
                  .contaParteBId(contaParteBId)
                  .parametroTributarioId(parametro.getId())
                  .tipoAjuste(tipoAjuste)
                  .descricao(descricao)
                  .valor(valor)
                  .status(Status.ACTIVE)
                  .build();

          if (dryRun) {
            previews.add(
                LancamentoParteBPreview.builder()
                    .mesReferencia(mesReferenciaStr)
                    .anoReferencia(String.valueOf(anoReferencia))
                    .tipoApuracao(tipoApuracaoStr)
                    .tipoRelacionamento(tipoRelacionamentoStr)
                    .contaContabilCode(contaContabilCode)
                    .contaParteBCode(contaParteBCode)
                    .parametroTributarioCodigo(parametroTributarioCodigo)
                    .tipoAjuste(tipoAjusteStr)
                    .descricao(descricao)
                    .valor(valorStr)
                    .build());
          } else {
            lancamentosToSave.add(lancamento);

            if (lancamentosToSave.size() >= CHUNK_SIZE) {
              lancamentoParteBRepository.saveAll(lancamentosToSave);
              log.info("Persisted chunk of {} lançamentos Parte B", lancamentosToSave.size());
              lancamentosToSave.clear();
            }
          }

          processedLines++;

        } catch (Exception e) {
          log.error("Error processing line {}: {}", lineNumber, e.getMessage(), e);
          errors.add(
              ImportError.builder()
                  .lineNumber(lineNumber)
                  .error("Linha " + lineNumber + ": erro inesperado — " + e.getMessage())
                  .build());
          skippedLines++;
        }
      }

      // Persistir chunk final se não for dry run
      if (!dryRun && !lancamentosToSave.isEmpty()) {
        lancamentoParteBRepository.saveAll(lancamentosToSave);
        log.info("Persisted final chunk of {} lançamentos Parte B", lancamentosToSave.size());
      }

      boolean success = skippedLines == 0;
      String message =
          success
              ? String.format("Successfully processed %d lines", processedLines)
              : String.format("Processed %d lines with %d errors", processedLines, skippedLines);

      return ImportLancamentoParteBResponse.builder()
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

  private String normalizeRequired(
      String value, String fieldName, int columnNumber, int lineNumber) {
    String normalized = normalizeField(value);
    if (normalized == null) {
      throw new IllegalArgumentException(formatError(
          lineNumber, columnNumber, fieldName, "valor obrigatório (vazio)"));
    }
    return normalized;
  }

  private static String formatError(
      int lineNumber, int columnNumber, String fieldName, String issue) {
    return "Linha " + lineNumber + ", campo '" + fieldName + "' (coluna " + columnNumber + "): "
        + issue;
  }

  /**
   * Cria CSVParser com auto-detecção de separador e header opcional.
   */
  private CSVParser createCsvParser(BufferedReader reader) throws Exception {
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
        .setSkipHeaderRecord(true)
        .setAllowMissingColumnNames(true);

    return new CSVParser(reader, builder.build());
  }
}
