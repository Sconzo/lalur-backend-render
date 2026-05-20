package br.com.lalurecf.infrastructure.dto.importschema;

import java.util.List;

/**
 * Descreve um campo de um arquivo CSV de importação.
 *
 * <p>Usado nos endpoints de schema para informar ao front-end o tipo,
 * formato, restrições e valores permitidos de cada coluna.
 */
public record ImportFieldSchema(
    String name,
    String type,
    boolean required,
    String format,
    List<String> allowedValues,
    String observation,
    Integer maxLength,
    String example
) {

  /** Valores aceitos para campos booleanos em todos os imports. */
  public static final List<String> BOOLEAN_ALLOWED_VALUES =
      List.of("true", "false", "sim", "não", "nao", "yes", "no", "1", "0");
}
