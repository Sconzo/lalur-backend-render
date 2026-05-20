package br.com.lalurecf.infrastructure.dto.importschema;

import java.util.List;
import java.util.Map;

/**
 * Schema de um arquivo CSV de importação.
 *
 * <p>Renderizado como CSV (separador ";") via {@link #toCsv()} para o usuário visualizar
 * em Excel/LibreOffice.
 */
public record ImportSchemaResponse(List<ImportFieldSchema> fields) {

  /** BOM UTF-8 para que o Excel detecte a codificação ao abrir o CSV. */
  private static final String UTF8_BOM = "﻿";

  /**
   * Rótulos amigáveis em PT-BR para os tipos técnicos. ENUMs são preservados
   * (são mostrados como "Enum" + lista de valores permitidos).
   */
  private static final Map<String, String> FRIENDLY_TYPES = Map.of(
      "String", "Texto",
      "Integer", "Número inteiro",
      "Decimal", "Número decimal",
      "Boolean", "Sim/Não",
      "Date", "Data",
      "Enum", "Enum"
  );

  /**
   * Renderiza o schema como CSV (separador ";"). Inclui BOM UTF-8 para Excel reconhecer
   * acentuação corretamente.
   */
  public String toCsv() {
    StringBuilder sb = new StringBuilder();
    sb.append(UTF8_BOM);
    sb.append("Campo;Tipo;Obrigatório;Formato;Valores permitidos;")
        .append("Observação;Tamanho máximo;Exemplo\n");
    for (ImportFieldSchema f : fields) {
      String allowed = f.allowedValues() == null ? null : String.join(", ", f.allowedValues());
      String maxLen = f.maxLength() == null ? "" : f.maxLength().toString();
      sb.append(escape(f.name())).append(';')
          .append(escape(friendlyType(f.type()))).append(';')
          .append(f.required() ? "Sim" : "Não").append(';')
          .append(escape(f.format())).append(';')
          .append(escape(allowed)).append(';')
          .append(escape(f.observation())).append(';')
          .append(maxLen).append(';')
          .append(escape(f.example())).append('\n');
    }
    return sb.toString();
  }

  private static String friendlyType(String type) {
    return FRIENDLY_TYPES.getOrDefault(type, type);
  }

  private static String escape(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    if (value.indexOf(';') >= 0 || value.indexOf('"') >= 0
        || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
