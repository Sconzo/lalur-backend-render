package br.com.lalurecf.application.service;

import br.com.lalurecf.infrastructure.dto.ecf.ValidationResult;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Serviço de validação de conteúdo de arquivos ECF.
 *
 * <p>Valida campos obrigatórios dos registros M conforme layout SPED ECF.
 * Possui 3 métodos, um por tipo de arquivo (ARQUIVO_PARCIAL, IMPORTED_ECF, COMPLETE_ECF).
 */
@Service
public class EcfValidatorService {

  private static final Set<String> VALID_INDICADORES = Set.of("1", "2", "3");
  private static final Set<String> VALID_DC = Set.of("D", "C");

  /**
   * Valida o conteúdo de um ARQUIVO_PARCIAL.
   *
   * <p>Verifica presença de M030, campos obrigatórios de M300/M350 e filhos M305/M310/M355/M360,
   * consistência do indicador e compara totalValor com soma dos filhos.
   *
   * @param content conteúdo do arquivo como string
   * @return resultado da validação com erros e avisos
   */
  public ValidationResult validateArquivoParcial(String content) {
    ValidationResult result = new ValidationResult();
    String[] lines = content.split("\n");

    boolean hasM030 = false;
    String paiAtual = null;
    String indicadorAtual = null;
    String codigoAtual = null;
    BigDecimal totalValorDeclarado = BigDecimal.ZERO;
    BigDecimal somaFilhos = BigDecimal.ZERO;
    boolean hasM305Filho = false;
    boolean hasM310Filho = false;

    for (String line : lines) {
      String tipo = extractTipo(line);
      if (tipo == null) {
        continue;
      }

      switch (tipo) {
        case "M030" -> {
          // Validar consistência do pai anterior
          if (paiAtual != null) {
            validarConsistenciaPai(paiAtual, indicadorAtual, codigoAtual,
                totalValorDeclarado, somaFilhos, hasM305Filho, hasM310Filho, result);
          }
          hasM030 = true;
          paiAtual = null;
          indicadorAtual = null;
          hasM305Filho = false;
          hasM310Filho = false;
        }
        case "M300", "M350" -> {
          if (paiAtual != null) {
            validarConsistenciaPai(paiAtual, indicadorAtual, codigoAtual,
                totalValorDeclarado, somaFilhos, hasM305Filho, hasM310Filho, result);
          }
          paiAtual = tipo;
          codigoAtual = extractField(line, 2);
          indicadorAtual = extractField(line, 5);
          somaFilhos = BigDecimal.ZERO;
          hasM305Filho = false;
          hasM310Filho = false;

          if (codigoAtual.isBlank()) {
            result.addError(tipo + ": codigoEnquadramento vazio");
          }
          if (!VALID_INDICADORES.contains(indicadorAtual)) {
            result.addError(tipo + " código " + codigoAtual
                + ": indicador inválido '" + indicadorAtual + "' (esperado 1, 2 ou 3)");
          }
          final String totalValorStr = extractField(line, 6);
          totalValorDeclarado = parseBigDecimal(totalValorStr);
          if (totalValorDeclarado == null) {
            result.addError(tipo + " código " + codigoAtual
                + ": totalValor inválido '" + totalValorStr + "'");
            totalValorDeclarado = BigDecimal.ZERO;
          }
        }
        case "M305", "M355" -> {
          hasM305Filho = true;
          String codigo = extractField(line, 2);
          String valorStr = extractField(line, 3);
          String dc = extractField(line, 4);
          if (codigo.isBlank()) {
            result.addError(tipo + ": codigoContaParteB vazio");
          }
          BigDecimal valor = parseBigDecimal(valorStr);
          if (valor == null) {
            result.addError(tipo + " conta " + codigo + ": valor inválido '" + valorStr + "'");
          } else {
            somaFilhos = somaFilhos.add(valor);
          }
          if (!VALID_DC.contains(dc)) {
            result.addError(tipo + " conta " + codigo + ": D/C inválido '" + dc + "'");
          }
        }
        case "M310", "M360" -> {
          hasM310Filho = true;
          String codigo = extractField(line, 2);
          String valorStr = extractField(line, 4);
          String dc = extractField(line, 5);
          if (codigo.isBlank()) {
            result.addError(tipo + ": codigoContabil vazio");
          }
          BigDecimal valor = parseBigDecimal(valorStr);
          if (valor == null) {
            result.addError(tipo + " conta " + codigo + ": valor inválido '" + valorStr + "'");
          } else if ("2".equals(indicadorAtual)) {
            // indicador=2: apenas filhos M310/M360 — acumular para comparar com totalValor
            somaFilhos = somaFilhos.add(valor);
          }
          if (!VALID_DC.contains(dc)) {
            result.addError(tipo + " conta " + codigo + ": D/C inválido '" + dc + "'");
          }
        }
        default -> {
          // outros registros não validados aqui
        }
      }
    }

    // Validar o último pai
    if (paiAtual != null) {
      validarConsistenciaPai(paiAtual, indicadorAtual, codigoAtual,
          totalValorDeclarado, somaFilhos, hasM305Filho, hasM310Filho, result);
    }

    if (!hasM030) {
      result.addError("Nenhum registro M030 encontrado no arquivo");
    }

    return result;
  }

  /**
   * Valida o conteúdo de um IMPORTED_ECF.
   *
   * <p>Verifica presença de M001 e M990, e que todas as linhas estão no formato SPED.
   *
   * @param content conteúdo do arquivo como string
   * @return resultado da validação
   */
  public ValidationResult validateImportedEcf(String content) {
    ValidationResult result = new ValidationResult();

    boolean hasM001 = content.contains("|M001|");
    boolean hasM990 = content.contains("|M990|");

    if (!hasM001) {
      result.addError("Registro |M001| não encontrado");
    }
    if (!hasM990) {
      result.addError("Registro |M990| não encontrado");
    }

    long invalidLines = content.lines()
        .filter(l -> !l.isBlank())
        .filter(l -> !l.startsWith("|") || !l.endsWith("|"))
        .count();

    if (invalidLines > 0) {
      result.addError(invalidLines
          + " linha(s) não estão no formato SPED (devem iniciar e terminar com |)");
    }

    return result;
  }

  /**
   * Valida o conteúdo de um COMPLETE_ECF.
   *
   * <p>Aplica todas as validações do ARQUIVO_PARCIAL para o bloco M, além de validar
   * presença de M001, M990 e a contagem de linhas do M990.
   *
   * @param content conteúdo do arquivo como string
   * @return resultado da validação
   */
  public ValidationResult validateCompleteEcf(String content) {
    ValidationResult result = validateImportedEcf(content);

    // Validar conteúdo do bloco M (mesma lógica do Parcial)
    ValidationResult parcialResult = validateArquivoParcial(content);
    parcialResult.getErrors().forEach(result::addError);
    parcialResult.getWarnings().forEach(result::addWarning);

    // Validar contagem do M990
    validateM990Count(content, result);

    return result;
  }

  private void validarConsistenciaPai(
      String tipo, String indicador, String codigo,
      BigDecimal totalDeclarado, BigDecimal somaFilhos,
      boolean hasM305, boolean hasM310, ValidationResult result) {

    if ("1".equals(indicador) && hasM310) {
      result.addError(tipo + " código " + codigo
          + ": indicador=1 (só Parte B) mas existem filhos M310/M360");
    }
    if ("2".equals(indicador) && hasM305) {
      result.addError(tipo + " código " + codigo
          + ": indicador=2 (só Contábil) mas existem filhos M305/M355");
    }
    if (totalDeclarado.compareTo(somaFilhos) != 0) {
      result.addWarning(tipo + " código " + codigo
          + ": totalValor=" + formatBr(totalDeclarado)
          + " difere da soma dos filhos=" + formatBr(somaFilhos));
    }
  }

  private void validateM990Count(String content, ValidationResult result) {
    String[] lines = content.split("\n");
    int m001Idx = -1;
    int m990Idx = -1;
    int m990Declared = 0;

    for (int i = 0; i < lines.length; i++) {
      String tipo = extractTipo(lines[i]);
      if ("M001".equals(tipo)) {
        m001Idx = i;
      }
      if ("M990".equals(tipo)) {
        m990Idx = i;
        String countStr = extractField(lines[i], 2);
        try {
          m990Declared = Integer.parseInt(countStr);
        } catch (NumberFormatException e) {
          result.addError("M990: contagem inválida '" + countStr + "'");
          return;
        }
        break;
      }
    }

    if (m001Idx >= 0 && m990Idx >= 0) {
      int actualCount = m990Idx - m001Idx + 1;
      if (actualCount != m990Declared) {
        result.addError("M990 declara " + m990Declared
            + " linhas mas o bloco M tem " + actualCount + " linhas");
      }
    }
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

  private BigDecimal parseBigDecimal(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(value.replace(",", "."));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private String formatBr(BigDecimal value) {
    return String.format("%.2f", value).replace(".", ",");
  }
}
