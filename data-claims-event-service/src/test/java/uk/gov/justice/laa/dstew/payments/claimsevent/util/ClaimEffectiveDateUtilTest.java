package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.EventServiceIllegalArgumentException;

@DisplayName("Claim effective date utility test")
class ClaimEffectiveDateUtilTest {

  ClaimEffectiveDateUtil claimEffectiveDateUtil = new ClaimEffectiveDateUtil();


  @Test
  @DisplayName("Should return case start date when case start date exists")
  void shouldReturnCaseStartDate() {
    // Given
    ClaimResponse claimResponse = ClaimResponse.builder().caseStartDate("2025-01-01").build();
    // When
    LocalDate result = claimEffectiveDateUtil.getEffectiveDate(claimResponse);
    // Then
    assertThat(result).isEqualTo(LocalDate.of(2025, 1, 1));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("Should fall back to rep order date when start date not present")
  void shouldFallBackToRepOrderDateWhenCaseStartDateNotPresent(String effectiveDate) {
    // Given
    ClaimResponse claimResponse =
        ClaimResponse.builder()
            .caseStartDate(effectiveDate)
            .representationOrderDate("2025-05-05")
            .build();
    // When
    LocalDate result = claimEffectiveDateUtil.getEffectiveDate(claimResponse);
    // Then
    assertThat(result).isEqualTo(LocalDate.of(2025, 5, 5));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("Should fall back to UFN when rep order date not present")
  void shouldFallBackToUFNWhenRepOrderDateNotPresent(String effectiveDate) {
    // Given
    ClaimResponse claimResponse =
        ClaimResponse.builder()
            .caseStartDate(effectiveDate)
            .representationOrderDate(effectiveDate)
            .uniqueFileNumber("251215/654")
            .build();
    // When
    LocalDate result = claimEffectiveDateUtil.getEffectiveDate(claimResponse);
    // Then
    assertThat(result).isEqualTo(LocalDate.of(2015, 12, 25));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("Should throw exception if no values present for start date")
  void shouldThrowExceptionIfNoValuesPresentForStartDate(String effectiveDate) {
    // Given
    ClaimResponse claimResponse =
        ClaimResponse.builder()
            .caseStartDate(effectiveDate)
            .representationOrderDate(effectiveDate)
            .uniqueFileNumber(effectiveDate)
            .build();
    // When / Then
    assertThatThrownBy(() -> claimEffectiveDateUtil.getEffectiveDate(claimResponse))
        .isInstanceOf(EventServiceIllegalArgumentException.class)
        .hasMessageContaining("No fields available to determine effective date");
  }
}
