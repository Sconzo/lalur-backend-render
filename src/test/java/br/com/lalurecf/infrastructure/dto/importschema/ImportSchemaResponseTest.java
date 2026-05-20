package br.com.lalurecf.infrastructure.dto.importschema;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests para {@link ImportSchemaResponse#toCsv()}. */
@DisplayName("ImportSchemaResponse.toCsv()")
class ImportSchemaResponseTest {

  private static final String UTF8_BOM = "﻿";
  private static final String HEADER =
      "Campo;Tipo;Obrigatório;Formato;Valores permitidos;"
          + "Observação;Tamanho máximo;Exemplo";

  @Test
  @DisplayName("Inicia com BOM UTF-8 e header em PT-BR")
  void startsWithBomAndPortugueseHeader() {
    ImportSchemaResponse schema = new ImportSchemaResponse(List.of());

    String csv = schema.toCsv();

    assertThat(csv).startsWith(UTF8_BOM + HEADER + "\n");
  }

  @Test
  @DisplayName("Traduz tipos técnicos para rótulos PT-BR amigáveis")
  void translatesTechnicalTypesToFriendlyLabels() {
    ImportSchemaResponse schema =
        new ImportSchemaResponse(
            List.of(
                new ImportFieldSchema("a", "String", true, null, null, null, null, null),
                new ImportFieldSchema("b", "Integer", true, null, null, null, null, null),
                new ImportFieldSchema("c", "Decimal", true, null, null, null, null, null),
                new ImportFieldSchema("d", "Boolean", true, null, null, null, null, null),
                new ImportFieldSchema("e", "Date", true, null, null, null, null, null)));

    String csv = schema.toCsv();

    assertThat(csv).contains("a;Texto;");
    assertThat(csv).contains("b;Número inteiro;");
    assertThat(csv).contains("c;Número decimal;");
    assertThat(csv).contains("d;Sim/Não;");
    assertThat(csv).contains("e;Data;");
  }

  @Test
  @DisplayName("Preserva o tipo Enum (mantido por solicitação do usuário)")
  void preservesEnumType() {
    ImportSchemaResponse schema =
        new ImportSchemaResponse(
            List.of(
                new ImportFieldSchema(
                    "status", "Enum", true, null, List.of("ATIVO", "INATIVO"), null, null, null)));

    String csv = schema.toCsv();

    // Separator é ";", então vírgula em "ATIVO, INATIVO" não força escape com aspas.
    assertThat(csv).contains("status;Enum;Sim;;ATIVO, INATIVO;");
  }

  @Test
  @DisplayName("Tipo desconhecido é renderizado como veio (fallback identidade)")
  void unknownTypeFallsBackToOriginal() {
    ImportSchemaResponse schema =
        new ImportSchemaResponse(
            List.of(new ImportFieldSchema("x", "UUID", true, null, null, null, null, null)));

    String csv = schema.toCsv();

    assertThat(csv).contains("x;UUID;");
  }

  @Test
  @DisplayName("required=true vira \"Sim\" e required=false vira \"Não\"")
  void requiredFlagIsTranslated() {
    ImportSchemaResponse schema =
        new ImportSchemaResponse(
            List.of(
                new ImportFieldSchema("obrig", "String", true, null, null, null, null, null),
                new ImportFieldSchema("opcio", "String", false, null, null, null, null, null)));

    String csv = schema.toCsv();

    assertThat(csv).contains("obrig;Texto;Sim;");
    assertThat(csv).contains("opcio;Texto;Não;");
  }

  @Test
  @DisplayName("allowedValues são unidos com ', ' e escapados quando contêm separador")
  void allowedValuesAreJoinedAndEscaped() {
    ImportSchemaResponse schema =
        new ImportSchemaResponse(
            List.of(
                new ImportFieldSchema(
                    "f", "Enum", true, null, List.of("A", "B", "C"), null, null, null)));

    String csv = schema.toCsv();

    // Junta com ", " e envolve em aspas porque a string contém vírgulas (que viram
    // texto literal no Excel) — mas crucialmente NÃO contém o separador ";", então
    // só viraria entre aspas se tivesse ";". Verificamos o conteúdo joined.
    assertThat(csv).contains("A, B, C");
  }

  @Test
  @DisplayName("Valores com separador (;) ou aspas são escapados conforme RFC 4180")
  void escapesValuesWithSeparatorOrQuotes() {
    ImportSchemaResponse schema =
        new ImportSchemaResponse(
            List.of(
                new ImportFieldSchema(
                    "f",
                    "String",
                    true,
                    "tem ; aqui",
                    null,
                    "tem \"aspas\" aqui",
                    null,
                    "linha\ncom quebra")));

    String csv = schema.toCsv();

    // ";" → envolve em aspas
    assertThat(csv).contains("\"tem ; aqui\"");
    // aspas → duplica + envolve
    assertThat(csv).contains("\"tem \"\"aspas\"\" aqui\"");
    // newline → envolve em aspas
    assertThat(csv).contains("\"linha\ncom quebra\"");
  }

  @Test
  @DisplayName("Campos null viram célula vazia (sem 'null' literal)")
  void nullFieldsBecomeEmptyCells() {
    ImportSchemaResponse schema =
        new ImportSchemaResponse(
            List.of(new ImportFieldSchema("x", "String", true, null, null, null, null, null)));

    String csv = schema.toCsv();

    // Linha do campo: x;Texto;Sim;;;;;
    assertThat(csv).contains("\nx;Texto;Sim;;;;;\n");
    assertThat(csv).doesNotContain("null");
  }

  @Test
  @DisplayName("maxLength é renderizado como número (sem aspas, sem separador)")
  void maxLengthIsRenderedAsNumber() {
    ImportSchemaResponse schema =
        new ImportSchemaResponse(
            List.of(
                new ImportFieldSchema("descricao", "String", true, null, null, null, 1000, "ex")));

    String csv = schema.toCsv();

    assertThat(csv).contains("descricao;Texto;Sim;;;;1000;ex");
  }

  @Test
  @DisplayName("Gera exatamente 1 linha por field + 1 linha de header")
  void generatesOneRowPerFieldPlusHeader() {
    ImportSchemaResponse schema =
        new ImportSchemaResponse(
            List.of(
                new ImportFieldSchema("a", "String", true, null, null, null, null, null),
                new ImportFieldSchema("b", "String", true, null, null, null, null, null),
                new ImportFieldSchema("c", "String", true, null, null, null, null, null)));

    long lineCount = schema.toCsv().lines().count();

    assertThat(lineCount).isEqualTo(4);
  }
}
