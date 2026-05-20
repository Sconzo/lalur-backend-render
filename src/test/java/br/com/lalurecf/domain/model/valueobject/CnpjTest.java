package br.com.lalurecf.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("CNPJ Value Object Tests")
class CnpjTest {

    @Test
    @DisplayName("Should create CNPJ with valid unformatted string")
    void shouldCreateCnpjWithValidUnformattedString() {
        // Arrange & Act
        CNPJ cnpj = CNPJ.of("11222333000181");

        // Assert
        assertNotNull(cnpj);
        assertEquals("11222333000181", cnpj.getValue());
    }

    @Test
    @DisplayName("Should create CNPJ with valid formatted string")
    void shouldCreateCnpjWithValidFormattedString() {
        // Arrange & Act
        CNPJ cnpj = CNPJ.of("11.222.333/0001-81");

        // Assert
        assertNotNull(cnpj);
        assertEquals("11222333000181", cnpj.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "11222333000181",      // Valid CNPJ 1
        "00000000000191",      // Valid CNPJ 2 (Banco do Brasil)
        "11.222.333/0001-81",  // Valid formatted CNPJ
        "00.000.000/0001-91"   // Valid formatted CNPJ 2
    })
    @DisplayName("Should create CNPJ with various valid formats")
    void shouldCreateCnpjWithVariousValidFormats(String validCnpj) {
        // Act
        CNPJ cnpj = CNPJ.of(validCnpj);

        // Assert
        assertNotNull(cnpj);
    }

    @Test
    @DisplayName("Should format CNPJ correctly")
    void shouldFormatCnpjCorrectly() {
        // Arrange
        CNPJ cnpj = CNPJ.of("11222333000181");

        // Act
        String formatted = cnpj.format();

        // Assert
        assertEquals("11.222.333/0001-81", formatted);
    }

    @Test
    @DisplayName("Should throw exception for null CNPJ")
    void shouldThrowExceptionForNullCnpj() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> CNPJ.of(null)
        );
        assertEquals("CNPJ cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for blank CNPJ")
    void shouldThrowExceptionForBlankCnpj() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> CNPJ.of("   ")
        );
        assertEquals("CNPJ cannot be null or blank", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for CNPJ with less than 14 digits")
    void shouldThrowExceptionForShortCnpj() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> CNPJ.of("1122233300018")
        );
        assertEquals("CNPJ must contain exactly 14 numeric digits", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for CNPJ with more than 14 digits")
    void shouldThrowExceptionForLongCnpj() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> CNPJ.of("112223330001811")
        );
        assertEquals("CNPJ must contain exactly 14 numeric digits", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for CNPJ with non-numeric characters")
    void shouldThrowExceptionForNonNumericCnpj() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> CNPJ.of("1122233300018A")
        );
        assertEquals("CNPJ must contain exactly 14 numeric digits", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "00000000000000",
        "11111111111111",
        "22222222222222",
        "33333333333333",
        "44444444444444",
        "55555555555555",
        "66666666666666",
        "77777777777777",
        "88888888888888",
        "99999999999999"
    })
    @DisplayName("Should throw exception for CNPJ with all same digits")
    void shouldThrowExceptionForCnpjWithAllSameDigits(String invalidCnpj) {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> CNPJ.of(invalidCnpj)
        );
        assertEquals("CNPJ cannot have all same digits", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for invalid check digits")
    void shouldThrowExceptionForInvalidCheckDigits() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> CNPJ.of("11222333000180")  // Last digit should be 1, not 0
        );
        assertEquals("Invalid CNPJ check digits", exception.getMessage());
    }

    @Test
    @DisplayName("Should consider two CNPJs with same value as equal")
    void shouldConsiderTwoCnpjsWithSameValueAsEqual() {
        // Arrange
        CNPJ cnpj1 = CNPJ.of("11222333000181");
        CNPJ cnpj2 = CNPJ.of("11.222.333/0001-81");

        // Act & Assert
        assertEquals(cnpj1, cnpj2);
        assertEquals(cnpj1.hashCode(), cnpj2.hashCode());
    }

    @Test
    @DisplayName("Should return formatted string in toString")
    void shouldReturnFormattedStringInToString() {
        // Arrange
        CNPJ cnpj = CNPJ.of("11222333000181");

        // Act
        String result = cnpj.toString();

        // Assert
        assertEquals("11.222.333/0001-81", result);
    }
}
