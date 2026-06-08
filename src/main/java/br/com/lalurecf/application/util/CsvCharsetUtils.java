package br.com.lalurecf.application.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Utilitários para leitura de arquivos CSV com detecção robusta de charset.
 *
 * <p>Suporta UTF-8 (com ou sem BOM) e faz fallback para ISO-8859-1 quando o conteúdo não é
 * UTF-8 válido. Isso evita o mojibake clássico (ex.: {@code ã} virar {@code Ã£}) ao receber
 * arquivos UTF-8 sem BOM produzidos por ferramentas modernas.
 */
public final class CsvCharsetUtils {

  private CsvCharsetUtils() {}

  /**
   * Cria um {@link BufferedReader} sobre os bytes do arquivo aplicando detecção de charset.
   *
   * <p>Regras de detecção:
   *
   * <ul>
   *   <li>BOM UTF-8 (EF BB BF) presente: remove o BOM e lê como UTF-8.
   *   <li>Sem BOM: tenta UTF-8 estrito; se decodificar sem erros, usa UTF-8.
   *   <li>Bytes inválidos para UTF-8: faz fallback para ISO-8859-1 (Latin-1).
   * </ul>
   *
   * @param rawBytes conteúdo bruto do arquivo
   * @return reader pronto para leitura
   */
  public static BufferedReader newReader(byte[] rawBytes) {
    int bomOffset = 0;
    if (rawBytes.length >= 3
        && (rawBytes[0] & 0xFF) == 0xEF
        && (rawBytes[1] & 0xFF) == 0xBB
        && (rawBytes[2] & 0xFF) == 0xBF) {
      bomOffset = 3;
    }

    Charset charset = detectCharset(rawBytes, bomOffset);
    return new BufferedReader(
        new InputStreamReader(
            new ByteArrayInputStream(rawBytes, bomOffset, rawBytes.length - bomOffset),
            charset));
  }

  private static Charset detectCharset(byte[] rawBytes, int bomOffset) {
    if (bomOffset > 0) {
      return StandardCharsets.UTF_8;
    }
    try {
      StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(rawBytes, bomOffset, rawBytes.length - bomOffset));
      return StandardCharsets.UTF_8;
    } catch (CharacterCodingException e) {
      return StandardCharsets.ISO_8859_1;
    }
  }
}
