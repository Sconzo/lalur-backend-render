package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.ecf.GenerateCompleteEcfUseCase;
import br.com.lalurecf.application.port.out.CompanyRepositoryPort;
import br.com.lalurecf.application.port.out.EcfFileRepositoryPort;
import br.com.lalurecf.domain.enums.EcfFileStatus;
import br.com.lalurecf.domain.enums.EcfFileType;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.Company;
import br.com.lalurecf.domain.model.EcfFile;
import br.com.lalurecf.infrastructure.dto.ecf.GenerateCompleteEcfResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por gerar o ECF Completo via merge por chave.
 *
 * <p>Algoritmo de merge (granularidade M300/M350):
 * <ol>
 *   <li>Carrega IMPORTED_ECF e ARQUIVO_PARCIAL do repositório</li>
 *   <li>Parseia o Parcial em mapa indexado por {codigoApuracao}|{tipo}|{codigoEnquadramento}</li>
 *   <li>Percorre Importado linha a linha; substitui M300/M350 cujo código existe no Parcial</li>
 *   <li>Adiciona M030 do Parcial ausentes no Importado e M400/M410/M405 do Parcial</li>
 *   <li>Recalcula M990</li>
 *   <li>Salva como COMPLETE_ECF</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EcfMergerService implements GenerateCompleteEcfUseCase {

  private final EcfFileRepositoryPort ecfFileRepositoryPort;
  private final CompanyRepositoryPort companyRepositoryPort;

  @Override
  @Transactional
  public GenerateCompleteEcfResponse generate(
      Integer fiscalYear, Long companyId, String generatedBy) {

    log.info("Gerando ECF Completo: companyId={}, fiscalYear={}", companyId, fiscalYear);

    // Passo 1: Carregar os dois arquivos-fonte
    EcfFile importedEcf = ecfFileRepositoryPort
        .findByCompanyAndFiscalYearAndType(companyId, fiscalYear, EcfFileType.IMPORTED_ECF)
        .orElseThrow(() -> new IllegalArgumentException(
            "ECF Importado não encontrado. "
                + "Faça upload do arquivo ECF antes de gerar o ECF Completo."));

    EcfFile parcialFile = ecfFileRepositoryPort
        .findByCompanyAndFiscalYearAndType(companyId, fiscalYear, EcfFileType.ARQUIVO_PARCIAL)
        .orElseThrow(() -> new IllegalArgumentException(
            "Arquivo Parcial não encontrado. "
                + "Gere o Arquivo Parcial antes de gerar o ECF Completo."));

    // Passo 2: Parsear o Parcial
    ParsedParcial parsed = parseParcial(parcialFile.getContent());

    // Passos 3, 4: Merge linha a linha
    List<String> resultLines = mergeContent(importedEcf.getContent(), parsed);

    // Passo 5: Recalcular M990 e os totalizadores do bloco 9 (9900/9990/9999)
    recalcularM990(resultLines);
    recalcularBloco9(resultLines);

    String resultContent = String.join("\n", resultLines) + "\n";

    // Passo 6: Salvar como COMPLETE_ECF
    Company company = companyRepositoryPort.findById(companyId)
        .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada: " + companyId));

    String cnpj = company.getCnpj() != null ? company.getCnpj().getValue() : companyId.toString();
    String fileName = String.format("ECF_Completo_%d_%s.txt", fiscalYear, cnpj);

    EcfFile completeEcf = EcfFile.builder()
        .fileType(EcfFileType.COMPLETE_ECF)
        .companyId(companyId)
        .fiscalYear(fiscalYear)
        .content(resultContent)
        .fileName(fileName)
        .fileStatus(EcfFileStatus.DRAFT)
        .generatedAt(LocalDateTime.now())
        .generatedBy(generatedBy)
        .sourceImportedEcfId(importedEcf.getId())
        .sourceParcialFileId(parcialFile.getId())
        .status(Status.ACTIVE)
        .build();

    EcfFile saved = ecfFileRepositoryPort.saveOrReplace(completeEcf);
    log.info("ECF Completo salvo: id={}, fileName={}", saved.getId(), saved.getFileName());

    int totalLinhas = extractM990Count(resultLines);

    return new GenerateCompleteEcfResponse(
        true,
        "ECF Completo gerado com sucesso",
        saved.getId(),
        saved.getFileName(),
        (long) resultContent.length(),
        importedEcf.getId(),
        parcialFile.getId(),
        totalLinhas);
  }

  /**
   * Parseia o Arquivo Parcial em estruturas indexadas para uso no merge.
   *
   * <p>Indexa M300/M350 com seus filhos (M305/M310/M355/M360) por chave
   * {codigoApuracao}|{tipo}|{codigoEnquadramento}. Coleta M030 completos e M400/M410/M405.
   */
  ParsedParcial parseParcial(String parcialContent) {
    Map<String, List<String>> blockIndex = new LinkedHashMap<>();
    Set<String> periodos = new LinkedHashSet<>();
    Map<String, List<String>> m030Blocks = new LinkedHashMap<>();
    List<String> m400Lines = new ArrayList<>();

    String periodoAtual = null;
    String chaveAtual = null;
    List<String> m030BlocoAtual = null;
    boolean inM400 = false;

    for (String line : parcialContent.split("\n")) {
      String tipo = extractTipo(line);
      if (tipo == null) {
        continue;
      }

      switch (tipo) {
        case "M030" -> {
          if (periodoAtual != null && m030BlocoAtual != null) {
            m030Blocks.put(periodoAtual, new ArrayList<>(m030BlocoAtual));
          }
          periodoAtual = extractField(line, 4);
          periodos.add(periodoAtual);
          m030BlocoAtual = new ArrayList<>();
          m030BlocoAtual.add(line);
          chaveAtual = null;
          inM400 = false;
        }
        case "M300", "M350" -> {
          String codigo = extractField(line, 2);
          chaveAtual = periodoAtual + "|" + tipo + "|" + codigo;
          blockIndex.put(chaveAtual, new ArrayList<>(List.of(line)));
          if (m030BlocoAtual != null) {
            m030BlocoAtual.add(line);
          }
          inM400 = false;
        }
        case "M305", "M310", "M355", "M360" -> {
          if (chaveAtual != null) {
            blockIndex.get(chaveAtual).add(line);
          }
          if (m030BlocoAtual != null) {
            m030BlocoAtual.add(line);
          }
        }
        case "M400" -> {
          m400Lines.add(line);
          inM400 = true;
          chaveAtual = null;
        }
        case "M410", "M405" -> {
          if (inM400) {
            m400Lines.add(line);
          }
        }
        default -> {
          // M001, M990 e outros: ignorar no parse do Parcial
        }
      }
    }

    if (periodoAtual != null && m030BlocoAtual != null) {
      m030Blocks.put(periodoAtual, new ArrayList<>(m030BlocoAtual));
    }

    return new ParsedParcial(blockIndex, periodos, m030Blocks, m400Lines);
  }

  /**
   * Executa o merge linha a linha do ECF Importado usando os dados do Parcial.
   */
  List<String> mergeContent(String importedContent, ParsedParcial parsed) {
    List<String> result = new ArrayList<>();
    String[] importedLines = importedContent.split("\n");

    boolean inBlocoM = false;
    String periodoAtual = "";
    boolean substituindo = false;
    Set<String> keysUsed = new LinkedHashSet<>();
    Set<String> periodosInImportado = new LinkedHashSet<>();
    List<String> blocoLinhas = new ArrayList<>();
    List<String> linhasAposM = new ArrayList<>();
    boolean afterM990 = false;

    for (String line : importedLines) {
      String tipo = extractTipo(line);

      // Linhas após M990: copiadas sem alteração
      if (afterM990) {
        linhasAposM.add(line);
        continue;
      }

      // Linhas antes de M001: copiadas sem alteração
      if (!inBlocoM && !"M001".equals(tipo)) {
        result.add(line);
        continue;
      }

      if ("M001".equals(tipo)) {
        inBlocoM = true;
        blocoLinhas.add(line);
        continue;
      }

      if ("M990".equals(tipo)) {
        // Inserir M300/M350 não usados do período atual antes do M990
        blocoLinhas.addAll(buildUnusedForPeriod(periodoAtual, parsed.blockIndex, keysUsed));
        // Adicionar M030 do Parcial não presentes no Importado
        for (String periodo : parsed.periodos) {
          if (!periodosInImportado.contains(periodo)) {
            List<String> m030Block = parsed.m030Blocks.get(periodo);
            if (m030Block != null) {
              blocoLinhas.addAll(m030Block);
            }
          }
        }
        // Adicionar M400/M410/M405 do Parcial
        blocoLinhas.addAll(parsed.m400Lines);
        // M990 placeholder — será recalculado
        blocoLinhas.add("|M990|0|");
        afterM990 = true;
        inBlocoM = false;
        continue;
      }

      if ("M030".equals(tipo)) {
        // Fechar período anterior
        if (!periodoAtual.isEmpty()) {
          blocoLinhas.addAll(buildUnusedForPeriod(periodoAtual, parsed.blockIndex, keysUsed));
        }
        periodoAtual = extractField(line, 4);
        periodosInImportado.add(periodoAtual);
        substituindo = false;
        blocoLinhas.add(line);
        continue;
      }

      if ("M300".equals(tipo) || "M350".equals(tipo)) {
        String codigo = extractField(line, 2);
        String chave = periodoAtual + "|" + tipo + "|" + codigo;
        if (parsed.blockIndex.containsKey(chave)) {
          blocoLinhas.addAll(parsed.blockIndex.get(chave));
          keysUsed.add(chave);
          substituindo = true;
        } else {
          blocoLinhas.add(line);
          substituindo = false;
        }
        continue;
      }

      if ("M305".equals(tipo) || "M310".equals(tipo)
          || "M355".equals(tipo) || "M360".equals(tipo)) {
        if (!substituindo) {
          blocoLinhas.add(line);
        }
        continue;
      }

      // M400/M410/M405 do Importado: ignorados (Parcial prevalece)
      if ("M400".equals(tipo) || "M410".equals(tipo) || "M405".equals(tipo)) {
        substituindo = false;
        continue;
      }

      // Outros registros do bloco M (M010, etc.)
      substituindo = false;
      blocoLinhas.add(line);
    }

    result.addAll(blocoLinhas);
    result.addAll(linhasAposM);
    return result;
  }

  private List<String> buildUnusedForPeriod(
      String periodo, Map<String, List<String>> blockIndex, Set<String> keysUsed) {
    List<String> lines = new ArrayList<>();
    if (periodo.isEmpty()) {
      return lines;
    }
    for (Map.Entry<String, List<String>> entry : blockIndex.entrySet()) {
      String key = entry.getKey();
      if (key.startsWith(periodo + "|") && !keysUsed.contains(key)) {
        lines.addAll(entry.getValue());
        keysUsed.add(key);
      }
    }
    return lines;
  }

  /**
   * Recalcula os totalizadores do bloco 9 (encerramento do arquivo).
   *
   * <ul>
   *   <li>|9900|TIPO_REG|QTD| — quantidade de registros de cada tipo no arquivo
   *   <li>|9990|QTD_LIN_9| — total de linhas do bloco 9 (inclui o próprio 9990)
   *   <li>|9999|QTD_LIN| — total geral de linhas do arquivo (inclui o próprio 9999)
   * </ul>
   *
   * <p>Os tipos listados nos registros 9900 são preservados; apenas o contador é atualizado
   * com a contagem atual do arquivo após o merge.
   */
  void recalcularBloco9(List<String> resultLines) {
    // Contagem de cada tipo de registro no arquivo final
    Map<String, Integer> countByTipo = new LinkedHashMap<>();
    for (String line : resultLines) {
      String tipo = extractTipo(line);
      if (tipo != null) {
        countByTipo.merge(tipo, 1, Integer::sum);
      }
    }

    // Atualizar cada |9900|TIPO|QTD| com a contagem correta;
    // coletar tipos que já têm registro 9900 (para depois identificar os faltantes).
    Set<String> tiposComRegistro9900 = new LinkedHashSet<>();
    int ultimoIndice9900 = -1;
    String template9900 = null;
    for (int i = 0; i < resultLines.size(); i++) {
      String line = resultLines.get(i);
      if (!"9900".equals(extractTipo(line))) {
        continue;
      }
      String[] parts = line.split("\\|", -1);
      if (parts.length < 4) {
        continue;
      }
      String tipoReg = parts[2];
      tiposComRegistro9900.add(tipoReg);
      int novoCount = countByTipo.getOrDefault(tipoReg, 0);
      parts[3] = String.valueOf(novoCount);
      resultLines.set(i, String.join("|", parts));
      ultimoIndice9900 = i;
      template9900 = line; // guarda última linha pra clonar formato (nº de campos)
    }

    // Adicionar |9900| para tipos que apareceram no arquivo após o merge
    // mas não estavam no bloco 9 original (ex: M305, M310 vindos do parcial).
    List<String> tiposFaltantes = new ArrayList<>();
    for (String tipo : countByTipo.keySet()) {
      if (tipo == null || tipo.isEmpty()) {
        continue;
      }
      // Ignora os totalizadores do bloco 9
      if ("9900".equals(tipo) || "9990".equals(tipo) || "9999".equals(tipo)
          || "9001".equals(tipo) || "9100".equals(tipo)) {
        continue;
      }
      if (!tiposComRegistro9900.contains(tipo)) {
        tiposFaltantes.add(tipo);
      }
    }

    if (!tiposFaltantes.isEmpty() && ultimoIndice9900 >= 0) {
      // Detectar formato do 9900 (número de campos) a partir de um existente
      int numCampos = template9900 != null ? template9900.split("\\|", -1).length : 6;

      // Inserir cada novo 9900 na posição alfabética correta dentro do bloco
      for (String tipo : tiposFaltantes) {
        int count = countByTipo.getOrDefault(tipo, 0);
        String novaLinha = formatRegistro9900(tipo, count, numCampos);
        int posicao = findInsertPosition9900(resultLines, tipo);
        resultLines.add(posicao, novaLinha);
      }

      // Atualizar countByTipo com os novos 9900 inseridos (afeta |9900|9900|)
      countByTipo.merge("9900", tiposFaltantes.size(), Integer::sum);

      // Re-percorrer os |9900| para atualizar o contador do tipo "9900"
      // (que agora cresceu por causa das novas linhas adicionadas).
      for (int i = 0; i < resultLines.size(); i++) {
        String line = resultLines.get(i);
        if (!"9900".equals(extractTipo(line))) {
          continue;
        }
        String[] parts = line.split("\\|", -1);
        if (parts.length < 4 || !"9900".equals(parts[2])) {
          continue;
        }
        parts[3] = String.valueOf(countByTipo.getOrDefault("9900", 0));
        resultLines.set(i, String.join("|", parts));
      }
    }

    // Recontar |9990| (total bloco 9) e |9999| (total geral)
    int total9 = 0;
    int totalGeral = 0;
    for (String line : resultLines) {
      String tipo = extractTipo(line);
      if (tipo == null) {
        continue;
      }
      totalGeral++;
      if (tipo.startsWith("9")) {
        total9++;
      }
    }

    for (int i = 0; i < resultLines.size(); i++) {
      String line = resultLines.get(i);
      String tipo = extractTipo(line);
      if ("9990".equals(tipo)) {
        resultLines.set(i, "|9990|" + total9 + "|");
      } else if ("9999".equals(tipo)) {
        resultLines.set(i, "|9999|" + totalGeral + "|");
      }
    }
  }

  /**
   * Encontra a posição correta para inserir um novo |9900|TIPO|...| mantendo
   * a ordem dos registros 9900 existentes (geralmente alfabética).
   *
   * <p>Procura o primeiro 9900 cujo tipo seja "maior" que o novo (comparação alfabética),
   * e insere antes dele. Se nenhum for maior, insere após o último 9900.
   */
  private int findInsertPosition9900(List<String> lines, String novoTipo) {
    int ultimoIndice9900 = -1;
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      if (!"9900".equals(extractTipo(line))) {
        continue;
      }
      String[] parts = line.split("\\|", -1);
      if (parts.length < 3) {
        continue;
      }
      String tipoAtual = parts[2];
      if (tipoAtual.compareTo(novoTipo) > 0) {
        return i;
      }
      ultimoIndice9900 = i;
    }
    return ultimoIndice9900 + 1;
  }

  /**
   * Constrói uma linha |9900| com o número de campos compatível com o formato existente.
   * Layout: |9900|TIPO_REG|QTD_REG|... campos extras vazios|
   */
  private String formatRegistro9900(String tipoReg, int qtd, int numCampos) {
    StringBuilder sb = new StringBuilder();
    sb.append("|9900|").append(tipoReg).append("|").append(qtd).append("|");
    // numCampos inclui os 2 vazios das pontas; já preenchemos 4 (vazio, 9900, tipo, qtd)
    // restam (numCampos - 5) campos vazios antes do "|" final
    int camposExtras = Math.max(0, numCampos - 5);
    for (int i = 0; i < camposExtras; i++) {
      sb.append("|");
    }
    return sb.toString();
  }

  void recalcularM990(List<String> resultLines) {
    int m990Idx = -1;
    int m001Idx = -1;
    for (int i = 0; i < resultLines.size(); i++) {
      String tipo = extractTipo(resultLines.get(i));
      if ("M001".equals(tipo)) {
        m001Idx = i;
      }
      if ("M990".equals(tipo)) {
        m990Idx = i;
        break;
      }
    }
    if (m990Idx >= 0 && m001Idx >= 0) {
      int totalLinhas = m990Idx - m001Idx + 1; // inclui M001 e M990
      resultLines.set(m990Idx, "|M990|" + totalLinhas + "|");
    }
  }

  private int extractM990Count(List<String> resultLines) {
    for (String line : resultLines) {
      String tipo = extractTipo(line);
      if ("M990".equals(tipo)) {
        String[] parts = line.split("\\|", -1);
        if (parts.length > 2) {
          try {
            return Integer.parseInt(parts[2]);
          } catch (NumberFormatException e) {
            return 0;
          }
        }
      }
    }
    return 0;
  }

  private String extractTipo(String line) {
    if (line == null || line.isBlank() || !line.startsWith("|")) {
      return null;
    }
    String[] parts = line.split("\\|", -1);
    return parts.length > 1 ? parts[1] : null;
  }

  private String extractField(String line, int fieldIndex) {
    String[] parts = line.split("\\|", -1);
    return parts.length > fieldIndex ? parts[fieldIndex] : "";
  }

  /**
   * Estrutura de dados para resultado do parse do Arquivo Parcial.
   *
   * @param blockIndex mapa {codigoApuracao}|{tipo}|{codigo} → linhas do bloco
   * @param periodos conjunto ordenado de codigosApuracao presentes no Parcial
   * @param m030Blocks mapa codigoApuracao → linhas completas do bloco M030
   * @param m400Lines linhas de M400/M410/M405 do Parcial
   */
  record ParsedParcial(
      Map<String, List<String>> blockIndex,
      Set<String> periodos,
      Map<String, List<String>> m030Blocks,
      List<String> m400Lines) {
  }
}
