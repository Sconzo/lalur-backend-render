package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.lancamentoparteb.ImportLancamentoParteBUseCase;
import br.com.lalurecf.application.port.out.ContaParteBRepositoryPort;
import br.com.lalurecf.application.port.out.LancamentoParteBRepositoryPort;
import br.com.lalurecf.application.port.out.PlanoDeContasRepositoryPort;
import br.com.lalurecf.application.port.out.TaxParameterRepositoryPort;
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
import br.com.lalurecf.infrastructure.security.FiscalYearContext;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
      MultipartFile file, Long companyId, boolean dryRun) {

    log.info(
        "Starting import of LancamentosParteB for company {} (dryRun: {})", companyId, dryRun);

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
        CSVParser csvParser = createCsvParser(reader)) {

      for (CSVRecord record : csvParser) {
        lineNumber++;

        try {
          // Extrair campos por posição (header opcional)
          if (record.size() < 9) {
            errors.add(ImportError.builder().lineNumber(lineNumber)
                .error("Linha com menos de 9 colunas (esperado 9)").build());
            skippedLines++;
            continue;
          }
          final String mesReferenciaStr = normalizeRequired(record.get(0),
              "mesReferencia", lineNumber);
          final String tipoApuracaoStr = normalizeRequired(record.get(1),
              "tipoApuracao", lineNumber);
          final String tipoRelacionamentoStr = normalizeRequired(record.get(2),
              "tipoRelacionamento", lineNumber);
          final String contaContabilCode = normalizeField(record.get(3));
          final String contaParteBCode = normalizeField(record.get(4));
          final String parametroTributarioCodigo = normalizeRequired(record.get(5),
              "parametroTributarioCodigo", lineNumber);
          final String tipoAjusteStr = normalizeRequired(record.get(6),
              "tipoAjuste", lineNumber);
          final String descricao = normalizeRequired(record.get(7),
              "descricao", lineNumber);
          final String valorStr = normalizeRequired(record.get(8),
              "valor", lineNumber);

          // Parse mesReferencia
          int mesReferencia;
          try {
            mesReferencia = Integer.parseInt(mesReferenciaStr);
            if (mesReferencia < 1 || mesReferencia > 12) {
              errors.add(
                  ImportError.builder()
                      .lineNumber(lineNumber)
                      .error("mesReferencia must be between 1 and 12, got: " + mesReferenciaStr)
                      .build());
              skippedLines++;
              continue;
            }
          } catch (NumberFormatException e) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error("Invalid mesReferencia format: " + mesReferenciaStr)
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
                    .error(
                        "Invalid tipoApuracao: '"
                            + tipoApuracaoStr
                            + "'. Valid values: IRPJ, CSLL")
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
                    .error(
                        "Invalid tipoRelacionamento: '"
                            + tipoRelacionamentoStr
                            + "'. Valid values: CONTA_CONTABIL, CONTA_PARTE_B, AMBOS")
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
                    .error(
                        "Invalid tipoAjuste: '"
                            + tipoAjusteStr
                            + "'. Valid values: ADICAO, EXCLUSAO")
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
                      .error("valor must be > 0")
                      .build());
              skippedLines++;
              continue;
            }
          } catch (NumberFormatException e) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error("Invalid valor format: " + valorStr)
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
                    .error(
                        "Parâmetro tributário não encontrado com código: '"
                            + parametroTributarioCodigo
                            + "'")
                    .build());
            skippedLines++;
            continue;
          }
          TaxParameter parametro = parametroOpt.get();
          if (parametro.getStatus() != Status.ACTIVE) {
            errors.add(
                ImportError.builder()
                    .lineNumber(lineNumber)
                    .error(
                        "Parâmetro tributário '"
                            + parametroTributarioCodigo
                            + "' não está ACTIVE. Status: "
                            + parametro.getStatus())
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
                        .error(
                            "contaContabilCode é obrigatório quando"
                                + " tipoRelacionamento = CONTA_CONTABIL")
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
                        .error(
                            "Conta contábil '"
                                + contaContabilCode
                                + "' não encontrada para empresa/anoReferencia "
                                + anoReferencia)
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
                        .error(
                            "contaParteBCode é obrigatório quando"
                                + " tipoRelacionamento = CONTA_PARTE_B")
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
                        .error(
                            "Conta Parte B '"
                                + contaParteBCode
                                + "' não encontrada para empresa/anoReferencia "
                                + anoReferencia)
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
                        .error(
                            "contaContabilCode é obrigatório quando tipoRelacionamento = AMBOS")
                        .build());
                skippedLines++;
                continue;
              }
              if (contaParteBCode == null) {
                errors.add(
                    ImportError.builder()
                        .lineNumber(lineNumber)
                        .error(
                            "contaParteBCode é obrigatório quando tipoRelacionamento = AMBOS")
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
                        .error(
                            "Conta contábil '"
                                + contaContabilCode
                                + "' não encontrada para empresa/anoReferencia "
                                + anoReferencia)
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
                        .error(
                            "Conta Parte B '"
                                + contaParteBCode
                                + "' não encontrada para empresa/anoReferencia "
                                + anoReferencia)
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
                      .error("tipoRelacionamento inválido: " + tipoRelacionamento)
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
                  .error("Unexpected error: " + e.getMessage())
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

  private String normalizeRequired(String value, String fieldName, int lineNumber) {
    String normalized = normalizeField(value);
    if (normalized == null) {
      throw new IllegalArgumentException(
          "Campo '" + fieldName + "' é obrigatório (coluna vazia na linha " + lineNumber + ")");
    }
    return normalized;
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
        .setSkipHeaderRecord(true);

    return new CSVParser(reader, builder.build());
  }
}
