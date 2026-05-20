package br.com.lalurecf.domain.model.valueobject;

import java.util.Objects;

/**
 * Value Object representing a Brazilian CNPJ (Cadastro Nacional da Pessoa Jurídica).
 * Immutable object with format and check digit validation.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public final class CNPJ {

  private final String value;

  private CNPJ(String value) {
    this.value = value;
  }

  /**
   * Creates a CNPJ from a database value, skipping check digit validation.
   * Use only when loading trusted data from the database.
   * For user input, use {@link #of(String)} which performs full validation.
   *
   * @param cnpj the raw CNPJ string from the database
   * @return a CNPJ instance, or null if the input is null or blank
   */
  public static CNPJ ofRaw(String cnpj) {
    if (cnpj == null || cnpj.isBlank()) {
      return null;
    }
    return new CNPJ(cnpj.replaceAll("[./-]", ""));
  }

  /**
   * Creates a CNPJ from a string value.
   * Accepts both formatted (00.000.000/0000-00) and unformatted (00000000000000) strings.
   *
   * @param cnpj the CNPJ string
   * @return a validated CNPJ instance
   * @throws IllegalArgumentException if CNPJ is invalid
   */
  public static CNPJ of(String cnpj) {
    if (cnpj == null || cnpj.isBlank()) {
      throw new IllegalArgumentException("CNPJ cannot be null or blank");
    }

    String cleanCnpj = cnpj.replaceAll("[./-]", "");

    validateFormat(cleanCnpj);
    validateCheckDigits(cleanCnpj);

    return new CNPJ(cleanCnpj);
  }

  /**
   * Validates CNPJ format (14 numeric digits).
   */
  private static void validateFormat(String cnpj) {
    if (!cnpj.matches("\\d{14}")) {
      throw new IllegalArgumentException("CNPJ must contain exactly 14 numeric digits");
    }

    // Check for known invalid CNPJs (all same digit)
    if (cnpj.matches("(\\d)\\1{13}")) {
      throw new IllegalArgumentException("CNPJ cannot have all same digits");
    }
  }

  /**
   * Validates CNPJ check digits using the official algorithm.
   */
  private static void validateCheckDigits(String cnpj) {
    int firstDigit = calculateCheckDigit(cnpj.substring(0, 12));
    int secondDigit = calculateCheckDigit(cnpj.substring(0, 12) + firstDigit);

    int providedFirstDigit = Character.getNumericValue(cnpj.charAt(12));
    int providedSecondDigit = Character.getNumericValue(cnpj.charAt(13));

    if (firstDigit != providedFirstDigit || secondDigit != providedSecondDigit) {
      throw new IllegalArgumentException("Invalid CNPJ check digits");
    }
  }

  /**
   * Calculates a single check digit for CNPJ.
   */
  private static int calculateCheckDigit(String partial) {
    int[] weights = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    int sum = 0;

    for (int i = 0; i < partial.length(); i++) {
      int digit = Character.getNumericValue(partial.charAt(i));
      sum += digit * weights[weights.length - partial.length() + i];
    }

    int remainder = sum % 11;
    if (remainder < 2) {
      return 0;
    }
    return 11 - remainder;
  }

  /**
   * Returns the unformatted CNPJ value (14 digits).
   */
  public String getValue() {
    return value;
  }

  /**
   * Returns the formatted CNPJ (00.000.000/0000-00).
   */
  public String format() {
    return String.format("%s.%s.%s/%s-%s",
        value.substring(0, 2),
        value.substring(2, 5),
        value.substring(5, 8),
        value.substring(8, 12),
        value.substring(12, 14)
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CNPJ cnpj = (CNPJ) o;
    return Objects.equals(value, cnpj.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return format();
  }
}
