package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.ExportLancamentoContabilUseCase;
import br.com.lalurecf.application.port.out.LancamentoContabilRepositoryPort;
import br.com.lalurecf.domain.model.LancamentoContabil;
import br.com.lalurecf.infrastructure.exception.ResourceNotFoundException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service para exportação de lançamentos contábeis para arquivo CSV.
 *
 * <p>Gera arquivo CSV com partidas dobradas no formato compatível com importação.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExportLancamentoContabilService implements ExportLancamentoContabilUseCase {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final String[] CSV_HEADER = {
    "contaDebitoCode",
    "contaDebitoName",
    "contaCreditoCode",
    "contaCreditoName",
    "data",
    "valor",
    "historico",
    "numeroDocumento"
  };

  private final LancamentoContabilRepositoryPort lancamentoContabilRepository;

  @Override
  @Transactional(readOnly = true)
  public String exportLancamentos(
      Long companyId, Integer fiscalYear, LocalDate dataInicio, LocalDate dataFim) {

    log.info(
        "Exporting Lançamentos Contábeis for company {} and fiscalYear {} (dataInicio: {},"
            + " dataFim: {})",
        companyId,
        fiscalYear,
        dataInicio,
        dataFim);

    // Validar range de data
    if (dataInicio != null && dataFim == null) {
      throw new IllegalArgumentException("dataFim is required when dataInicio is provided");
    }

    if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio)) {
      throw new IllegalArgumentException("dataFim must be >= dataInicio");
    }

    // Buscar lançamentos já filtrados e ordenados no banco
    List<LancamentoContabil> lancamentos =
        lancamentoContabilRepository.findForExport(companyId, fiscalYear, dataInicio, dataFim);

    // Validar se há lançamentos
    if (lancamentos.isEmpty()) {
      throw new ResourceNotFoundException(
          "No lançamentos found for company "
              + companyId
              + " and fiscalYear "
              + fiscalYear);
    }

    try {
      // Gerar CSV
      StringWriter writer = new StringWriter();
      CSVFormat format =
          CSVFormat.DEFAULT
              .builder()
              .setDelimiter(';')
              .setHeader(CSV_HEADER)
              .build();

      try (CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {
        for (LancamentoContabil lancamento : lancamentos) {
          csvPrinter.printRecord(
              lancamento.getContaDebitoCode(),
              lancamento.getContaDebitoName(),
              lancamento.getContaCreditoCode(),
              lancamento.getContaCreditoName(),
              lancamento.getData().format(DATE_FORMATTER),
              String.format(Locale.US, "%.2f", lancamento.getValor()),
              lancamento.getHistorico(),
              lancamento.getNumeroDocumento() != null ? lancamento.getNumeroDocumento() : "");
        }
      }

      log.info("Exported {} lançamentos contábeis", lancamentos.size());
      return writer.toString();

    } catch (Exception e) {
      log.error("Error generating CSV: {}", e.getMessage(), e);
      throw new RuntimeException("Error generating CSV file: " + e.getMessage(), e);
    }
  }
}
