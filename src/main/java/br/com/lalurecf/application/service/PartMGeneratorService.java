package br.com.lalurecf.application.service;

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
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável pela geração do conteúdo do bloco M do arquivo ECF.
 *
 * <p>Gera registros M030/M300/M305/M310 (IRPJ), M350/M355/M360 (CSLL) e
 * M400/M410/M405 (Contas da Parte B) a partir dos lançamentos cadastrados.
 *
 * <p>O método principal {@link #generateArquivoParcial} retorna o conteúdo do
 * bloco M (sem M001) incluindo M990 ao final, pronto para ser envolto pelo
 * montador do arquivo parcial.
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class PartMGeneratorService {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyyyy");

  private final LancamentoParteBRepositoryPort lancamentoRepo;
  private final ContaParteBRepositoryPort contaParteBRepo;
  private final PlanoDeContasRepositoryPort planoDeContasRepo;
  private final TaxParameterRepositoryPort taxParameterRepo;

  /**
   * Gera o conteúdo do bloco M para o arquivo parcial ECF.
   *
   * <p>Retorna todas as linhas M (Grupo1 IRPJ + Grupo2 CSLL + Grupo3 Parte B + M990)
   * como string com separador de linha. O chamador (montador do arquivo parcial) deve
   * adicionar a linha M001 antes deste conteúdo.
   *
   * @param companyId ID da empresa
   * @param fiscalYear ano fiscal de referência
   * @return conteúdo do bloco M terminando com M990
   * @throws IllegalArgumentException se não existirem lançamentos ACTIVE para o ano
   */
  @Transactional(readOnly = true)
  public String generateArquivoParcial(Long companyId, Integer fiscalYear) {
    List<LancamentoParteB> active =
        lancamentoRepo.findByCompanyIdAndAnoReferenciaAndStatus(
            companyId, fiscalYear, Status.ACTIVE);
    return generateArquivoParcial(active, fiscalYear);
  }

  /**
   * Variante que recebe a lista já filtrada, evitando nova consulta ao banco quando o chamador
   * já carregou os lançamentos.
   */
  public String generateArquivoParcial(List<LancamentoParteB> active, Integer fiscalYear) {
    if (active.isEmpty()) {
      throw new IllegalArgumentException(
          "Nenhum lançamento da Parte B ativo encontrado para o ano " + fiscalYear);
    }

    List<String> lines = new ArrayList<>();
    lines.addAll(generateGrupoIrpj(active, fiscalYear));
    lines.addAll(generateGrupoCsll(active, fiscalYear));

    lines.add(String.format("|M990|%d|", lines.size() + 1));

    return String.join("\n", lines) + "\n";
  }

  /**
   * Gera linhas do Grupo 1 — IRPJ (M030/M300/M305/M310).
   * Package-private para facilitar testes unitários.
   */
  List<String> generateGrupoIrpj(List<LancamentoParteB> active, Integer fiscalYear) {
    return generateGrupo(active, fiscalYear, TipoApuracao.IRPJ, "M300", "M305", "M310");
  }

  /**
   * Gera linhas do Grupo 2 — CSLL (M030/M350/M355/M360).
   * Package-private para facilitar testes unitários.
   */
  List<String> generateGrupoCsll(List<LancamentoParteB> active, Integer fiscalYear) {
    return generateGrupo(active, fiscalYear, TipoApuracao.CSLL, "M350", "M355", "M360");
  }

  private List<String> generateGrupo(
      List<LancamentoParteB> allActive, Integer fiscalYear,
      TipoApuracao tipoApuracao, String regPai, String regFilhoParteB, String regFilhoContabil) {

    List<LancamentoParteB> filtered = allActive.stream()
        .filter(l -> l.getTipoApuracao() == tipoApuracao)
        .collect(Collectors.toList());

    if (filtered.isEmpty()) {
      return new ArrayList<>();
    }

    // Batch-fetch entidades relacionadas (evita N+1 dentro dos loops)
    Map<Long, TaxParameter> parametrosById = batchFetchParametros(filtered);
    Map<Long, ContaParteB> contasParteBById = batchFetchContasParteB(filtered);
    Map<Long, PlanoDeContas> contasContabeisById = batchFetchPlanoDeContas(filtered);

    // Período cumulativo desde 01/01 do ano fiscal
    LocalDate inicioAno = LocalDate.of(fiscalYear, 1, 1);

    // Agrupar por mesReferencia (ordenado)
    Map<Integer, List<LancamentoParteB>> byMes = new TreeMap<>(
        filtered.stream().collect(Collectors.groupingBy(LancamentoParteB::getMesReferencia)));

    List<String> lines = new ArrayList<>();

    for (Map.Entry<Integer, List<LancamentoParteB>> mesEntry : byMes.entrySet()) {
      int mes = mesEntry.getKey();
      List<LancamentoParteB> lancamentosMes = mesEntry.getValue();

      // M030 — sempre 01/01 a último dia do mês corrente
      LocalDate fim = LocalDate.of(fiscalYear, mes, 1)
          .withDayOfMonth(LocalDate.of(fiscalYear, mes, 1).lengthOfMonth());
      String codigoApuracao = "A" + String.format("%02d", mes);
      lines.add(String.format("|M030|%s|%s|%s|",
          inicioAno.format(DATE_FORMAT), fim.format(DATE_FORMAT), codigoApuracao));

      // Agrupar por parametroTributarioId, ordenado pelo code (numérico crescente)
      // para seguir o padrão do ECF (ex: 6 → 8 → 8.65 → 8.75 → 95)
      Map<Long, List<LancamentoParteB>> byParametro = lancamentosMes.stream()
          .collect(Collectors.groupingBy(
              LancamentoParteB::getParametroTributarioId,
              LinkedHashMap::new,
              Collectors.toList()));

      List<Map.Entry<Long, List<LancamentoParteB>>> sortedByParametro =
          new ArrayList<>(byParametro.entrySet());
      sortedByParametro.sort((a, b) -> {
        TaxParameter pa = parametrosById.get(a.getKey());
        TaxParameter pb = parametrosById.get(b.getKey());
        return compareCodesNumerically(
            pa != null ? pa.getCode() : "",
            pb != null ? pb.getCode() : "");
      });

      for (Map.Entry<Long, List<LancamentoParteB>> paramEntry : sortedByParametro) {
        Long parametroId = paramEntry.getKey();
        List<LancamentoParteB> grupo = paramEntry.getValue();

        TaxParameter parametro = parametrosById.get(parametroId);
        if (parametro == null) {
          throw new IllegalArgumentException(
              "ParametroTributario não encontrado: " + parametroId);
        }

        TipoAjuste tipoAjuste = grupo.get(0).getTipoAjuste();
        String tipoAjusteStr = tipoAjuste == TipoAjuste.ADICAO ? "A" : "E";
        String dc = tipoAjuste == TipoAjuste.ADICAO ? "D" : "C";
        String indicador = determineIndicador(grupo);
        BigDecimal somaValores = grupo.stream()
            .map(LancamentoParteB::getValor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        String historico = grupo.get(0).getDescricao();

        // M300/M350 — registro pai
        lines.add(String.format("|%s|%s|%s|%s|%s|%s|%s|",
            regPai, parametro.getCode(), removeAccents(parametro.getDescription()),
            tipoAjusteStr, indicador, formatValor(somaValores), removeAccents(historico)));

        // M305/M355 — agrupado por contaParteBId, todos juntos primeiro
        Map<Long, BigDecimal> m305Totals = grupo.stream()
            .filter(l -> l.getTipoRelacionamento() == TipoRelacionamento.CONTA_PARTE_B
                || l.getTipoRelacionamento() == TipoRelacionamento.AMBOS)
            .filter(l -> l.getContaParteBId() != null)
            .collect(Collectors.groupingBy(
                LancamentoParteB::getContaParteBId,
                LinkedHashMap::new,
                Collectors.reducing(BigDecimal.ZERO,
                    LancamentoParteB::getValor, BigDecimal::add)));

        for (Map.Entry<Long, BigDecimal> e : m305Totals.entrySet()) {
          ContaParteB conta = contasParteBById.get(e.getKey());
          if (conta == null) {
            throw new IllegalArgumentException("ContaParteB não encontrada: " + e.getKey());
          }
          lines.add(String.format("|%s|%s|%s|%s|",
              regFilhoParteB, conta.getCodigoConta(), formatValor(e.getValue()), dc));
        }

        // M310/M360 — agrupado por contaContabilId, depois dos M305
        Map<Long, BigDecimal> m310Totals = grupo.stream()
            .filter(l -> l.getTipoRelacionamento() == TipoRelacionamento.CONTA_CONTABIL
                || l.getTipoRelacionamento() == TipoRelacionamento.AMBOS)
            .filter(l -> l.getContaContabilId() != null)
            .collect(Collectors.groupingBy(
                LancamentoParteB::getContaContabilId,
                LinkedHashMap::new,
                Collectors.reducing(BigDecimal.ZERO,
                    LancamentoParteB::getValor, BigDecimal::add)));

        for (Map.Entry<Long, BigDecimal> e : m310Totals.entrySet()) {
          PlanoDeContas plano = contasContabeisById.get(e.getKey());
          if (plano == null) {
            throw new IllegalArgumentException("PlanoDeContas não encontrado: " + e.getKey());
          }
          lines.add(String.format("|%s|%s||%s|%s|",
              regFilhoContabil, plano.getCode(), formatValor(e.getValue()), dc));
        }
      }
    }

    return lines;
  }

  private Map<Long, TaxParameter> batchFetchParametros(List<LancamentoParteB> lancamentos) {
    List<Long> ids = lancamentos.stream()
        .map(LancamentoParteB::getParametroTributarioId)
        .filter(java.util.Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());
    return ids.isEmpty()
        ? Map.of()
        : taxParameterRepo.findAllById(ids).stream()
            .collect(Collectors.toMap(TaxParameter::getId, p -> p));
  }

  private Map<Long, ContaParteB> batchFetchContasParteB(List<LancamentoParteB> lancamentos) {
    List<Long> ids = lancamentos.stream()
        .map(LancamentoParteB::getContaParteBId)
        .filter(java.util.Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());
    return ids.isEmpty()
        ? Map.of()
        : contaParteBRepo.findAllById(ids).stream()
            .collect(Collectors.toMap(ContaParteB::getId, c -> c));
  }

  private Map<Long, PlanoDeContas> batchFetchPlanoDeContas(List<LancamentoParteB> lancamentos) {
    List<Long> ids = lancamentos.stream()
        .map(LancamentoParteB::getContaContabilId)
        .filter(java.util.Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());
    return ids.isEmpty()
        ? Map.of()
        : planoDeContasRepo.findAllById(ids).stream()
            .collect(Collectors.toMap(PlanoDeContas::getId, p -> p));
  }

  /**
   * Gera linhas do Grupo 3 — Contas da Parte B (M400/M410/M405).
   *
   * <p>⚠️ Campos dos registros M400/M410/M405 são baseados em aproximação —
   * confirmar layout exato contra o manual oficial SPED ECF antes de homologar.
   *
   * <p>Package-private para facilitar testes unitários.
   */
  List<String> generateGrupo3ParteB(List<LancamentoParteB> active) {
    // Collect unique ContaParteB IDs referenced in lançamentos
    Set<Long> contaIdsUsadas = active.stream()
        .filter(l -> l.getContaParteBId() != null)
        .map(LancamentoParteB::getContaParteBId)
        .collect(Collectors.toSet());

    List<String> lines = new ArrayList<>();

    for (Long contaId : contaIdsUsadas) {
      ContaParteB conta = contaParteBRepo.findById(contaId)
          .orElseThrow(() -> new IllegalArgumentException(
              "ContaParteB não encontrada: " + contaId));

      List<LancamentoParteB> lancamentosDaConta = active.stream()
          .filter(l -> contaId.equals(l.getContaParteBId()))
          .collect(Collectors.toList());

      // M400 — natureza da conta (⚠️ confirmar campos no SPED ECF manual)
      lines.add(String.format("|M400|%s|%s|%s|",
          conta.getCodigoConta(), conta.getDescricao(), conta.getTipoTributo().name()));

      // M410 — lançamentos (⚠️ confirmar campos no SPED ECF manual)
      for (LancamentoParteB lanc : lancamentosDaConta) {
        String dc = lanc.getTipoAjuste() == TipoAjuste.ADICAO ? "D" : "C";
        lines.add(String.format("|M410|%s|%02d/%d|%s|%s|%s|%s|",
            conta.getCodigoConta(),
            lanc.getMesReferencia(), lanc.getAnoReferencia(),
            lanc.getTipoAjuste().name(),
            formatValor(lanc.getValor()), dc,
            lanc.getDescricao()));
      }

      // M405 — saldo da conta (⚠️ confirmar campos no SPED ECF manual)
      BigDecimal totalAdic = lancamentosDaConta.stream()
          .filter(l -> l.getTipoAjuste() == TipoAjuste.ADICAO)
          .map(LancamentoParteB::getValor)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal totalExcl = lancamentosDaConta.stream()
          .filter(l -> l.getTipoAjuste() == TipoAjuste.EXCLUSAO)
          .map(LancamentoParteB::getValor)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal saldoAnterior = conta.getSaldoInicial() != null
          ? conta.getSaldoInicial() : BigDecimal.ZERO;
      BigDecimal saldoAtual = saldoAnterior.add(totalAdic).subtract(totalExcl);

      lines.add(String.format("|M405|%s|%s|%s|%s|%s|",
          conta.getCodigoConta(),
          formatValor(saldoAnterior), formatValor(totalAdic),
          formatValor(totalExcl), formatValor(saldoAtual)));
    }

    return lines;
  }

  private String determineIndicador(List<LancamentoParteB> grupo) {
    boolean allContaParteB = grupo.stream()
        .allMatch(l -> l.getTipoRelacionamento() == TipoRelacionamento.CONTA_PARTE_B);
    boolean allContaContabil = grupo.stream()
        .allMatch(l -> l.getTipoRelacionamento() == TipoRelacionamento.CONTA_CONTABIL);

    if (allContaParteB) {
      return "1";
    }
    if (allContaContabil) {
      return "2";
    }
    return "3";
  }

  /**
   * Compara dois códigos de parâmetro como números (ex: "6" &lt; "8" &lt; "8.65" &lt; "95").
   * Aceita "," ou "." como separador decimal. Códigos não-numéricos são comparados
   * lexicograficamente como fallback.
   */
  private int compareCodesNumerically(String a, String b) {
    try {
      BigDecimal va = new BigDecimal(a.replace(",", "."));
      BigDecimal vb = new BigDecimal(b.replace(",", "."));
      return va.compareTo(vb);
    } catch (NumberFormatException e) {
      return a.compareTo(b);
    }
  }

  /**
   * Remove acentos e diacríticos de uma string (ex: "Provisões" → "Provisoes").
   * Mantém o texto legível em qualquer encoding e evita problemas de exibição.
   */
  private String removeAccents(String s) {
    if (s == null) {
      return null;
    }
    return Normalizer.normalize(s, Normalizer.Form.NFD)
        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
  }

  private String formatValor(BigDecimal valor) {
    if (valor == null) {
      return "0,00";
    }
    return String.format("%.2f", valor).replace(".", ",");
  }
}
