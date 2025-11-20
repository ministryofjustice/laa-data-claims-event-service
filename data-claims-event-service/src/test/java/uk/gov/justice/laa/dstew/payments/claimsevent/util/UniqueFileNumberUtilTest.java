package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.EventServiceIllegalArgumentException;

@DisplayName("Unique file number util test")
class UniqueFileNumberUtilTest {

  @Test
  @DisplayName("Should parse valid file number")
  void shouldParseValidFileNumber() {
    // Given
    String ufn = "010101/123";
    // When
    LocalDate result = UniqueFileNumberUtil.parse(ufn);
    // Then
    assertThat(result).isEqualTo(LocalDate.of(2001, 1, 1));
  }

  @Test
  @DisplayName("Should parse valid file number two")
  void shouldParseValidFileNumberTwo() {
    // Given
    String ufn = "251230/123";
    // When
    LocalDate result = UniqueFileNumberUtil.parse(ufn);
    // Then
    assertThat(result).isEqualTo(LocalDate.of(2030, 12, 25));
  }

  @Test
  @DisplayName("Should parse valid with year greater than 50 (1900)")
  void shouldParseValidWithYearGreaterThan50() {
    // Given
    String ufn = "010151/123";
    // When
    LocalDate result = UniqueFileNumberUtil.parse(ufn);
    // Then
    assertThat(result).isEqualTo(LocalDate.of(1951, 1, 1));
  }

  @Test
  @DisplayName("Should parse valid with year lower than 50 (2000)")
  void shouldParseValidWithYearLowerThan50() {
    // Given
    String ufn = "010150/123";
    // When
    LocalDate result = UniqueFileNumberUtil.parse(ufn);
    // Then
    assertThat(result).isEqualTo(LocalDate.of(2050, 1, 1));
  }

  @ParameterizedTest
  @ValueSource(strings = {"234", "010101", "ABC"})
  void shouldThrowExceptionWhenInvalidFormat(String invalidUfnNumbers) {
    // When
    Exception exception =
        assertThrows(
            EventServiceIllegalArgumentException.class,
            () -> UniqueFileNumberUtil.parse(invalidUfnNumbers));
    // Then
    assertThat(exception).isNotNull();
    assertThat(exception.getMessage())
        .isEqualTo(
            "Invalid format for unique file number: "
                + invalidUfnNumbers
                + ". Expected format: ddMMyy/NNN");
  }
}
