package br.com.lalurecf.domain.util;

import java.util.regex.Pattern;

/**
 * Utilitário para validação e manipulação da máscara de níveis do plano de contas.
 *
 * <p>A máscara é uma string de segmentos de dígitos '9' separados por ponto.
 * Exemplo: "99.999.99.999999" representa N1=2 dígitos, N2=3 dígitos, N3=2 dígitos, N4=6 dígitos.
 */
public final class MascaraNiveisUtils {

  private static final Pattern FORMATO_MASCARA =
      Pattern.compile("^9+(\\.9+){0,5}$");

  private MascaraNiveisUtils() {}

  /**
   * Valida que a máscara possui formato correto.
   *
   * <p>Regras: apenas dígitos '9' e pontos, sem ponto inicial/final, sem ponto duplo,
   * entre 1 e 6 segmentos.
   *
   * @param mascara string da máscara (ex: "99.999.99.999999")
   * @throws IllegalArgumentException se o formato for inválido
   */
  public static void validarFormato(String mascara) {
    if (mascara == null || mascara.isBlank()) {
      throw new IllegalArgumentException(
          "Máscara de níveis é obrigatória");
    }
    if (!FORMATO_MASCARA.matcher(mascara).matches()) {
      throw new IllegalArgumentException(
          "Máscara de níveis inválida: '" + mascara
              + "'. Use apenas '9' e '.', ex: '99.999.99.999999'");
    }
  }

  /**
   * Valida que o número de níveis informado corresponde à quantidade de segmentos da máscara.
   *
   * @param mascara string da máscara
   * @param numNiveis número de níveis informado pelo front
   * @throws IllegalArgumentException se não corresponderem
   */
  public static void validarNumNiveis(String mascara, int numNiveis) {
    int segmentos = mascara.split("\\.").length;
    if (segmentos != numNiveis) {
      throw new IllegalArgumentException(
          "numNiveis informado (" + numNiveis + ") não corresponde à quantidade de segmentos "
              + "da máscara (" + segmentos + "). Máscara: '" + mascara + "'");
    }
  }

  /**
   * Valida o código de uma conta contra a máscara da empresa.
   *
   * <p>O código pode estar em qualquer nível (1 a numNiveis). Cada segmento do código
   * deve ter exatamente o número de dígitos definido pela máscara para aquele nível.
   *
   * @param code código da conta (ex: "10.123.05")
   * @param mascara máscara da empresa (ex: "99.999.99.999999")
   * @throws IllegalArgumentException se o código não corresponder à máscara
   */
  public static void validarCodigoContraMascara(String code, String mascara) {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("Código da conta não pode ser vazio");
    }

    String[] mascaraSegmentos = mascara.split("\\.");
    String[] codeSegmentos = code.split("\\.");
    int nivelCode = codeSegmentos.length;

    if (nivelCode > mascaraSegmentos.length) {
      throw new IllegalArgumentException(
          "Código '" + code + "' possui " + nivelCode + " níveis, mas a máscara '"
              + mascara + "' define apenas " + mascaraSegmentos.length + " níveis");
    }

    StringBuilder regex = new StringBuilder("^");
    for (int i = 0; i < nivelCode; i++) {
      if (i > 0) {
        regex.append("\\.");
      }
      int digitos = mascaraSegmentos[i].length();
      regex.append("\\d{").append(digitos).append("}");
    }
    regex.append("$");

    if (!code.matches(regex.toString())) {
      throw new IllegalArgumentException(
          "Código '" + code + "' não corresponde ao formato definido pela máscara '"
              + mascara + "'. Cada segmento deve ter o número exato de dígitos da máscara.");
    }
  }

  /**
   * Deriva o nível hierárquico de uma conta a partir do seu código.
   *
   * <p>O nível é determinado pelo número de segmentos separados por ponto.
   * Exemplo: "10" → 1, "10.123" → 2, "10.123.05.001234" → 4.
   *
   * @param code código da conta
   * @return nível hierárquico (número de segmentos)
   */
  public static int derivarNivel(String code) {
    if (code == null || code.isBlank()) {
      return 1;
    }
    return code.split("\\.").length;
  }
}
