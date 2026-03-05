package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.EventServiceIllegalArgumentException;

@DisplayName("Claim effective date utility test")
class ClaimEffectiveDateUtilTest {

  // ---------------------------------------------------------------------------
  // Standard resolution path (non-PROD)
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("Standard claims (non-PROD fee code)")
  class StandardClaims {

    @Test
    @DisplayName("Returns case start date when present")
    void shouldReturnCaseStartDate() {
      // Given
      ClaimResponse claimResponse = ClaimResponse.builder().caseStartDate("2025-01-01").build();
      // When
      LocalDate result = ClaimEffectiveDateUtil.getEffectiveDate(claimResponse);
      // Then
      assertThat(result).isEqualTo(LocalDate.of(2025, 1, 1));
    }

    @ParameterizedTest(name = "Falls through when caseStartDate is [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Falls back to representation order date when case start date is absent or blank")
    void shouldFallBackToRepOrderDateWhenCaseStartDateNotPresent(String caseStartDate) {
      // Given
      ClaimResponse claimResponse =
          ClaimResponse.builder()
              .caseStartDate(caseStartDate)
              .representationOrderDate("2025-05-05")
              .build();
      // When
      LocalDate result = ClaimEffectiveDateUtil.getEffectiveDate(claimResponse);
      // Then
      assertThat(result).isEqualTo(LocalDate.of(2025, 5, 5));
    }

    @ParameterizedTest(name = "Falls through when representationOrderDate is [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Falls back to UFN when representation order date is absent or blank")
    void shouldFallBackToUFNWhenRepOrderDateNotPresent(String absentDate) {
      // Given
      ClaimResponse claimResponse =
          ClaimResponse.builder()
              .caseStartDate(absentDate)
              .representationOrderDate(absentDate)
              .uniqueFileNumber("251215/654")
              .build();
      // When
      LocalDate result = ClaimEffectiveDateUtil.getEffectiveDate(claimResponse);
      // Then
      assertThat(result).isEqualTo(LocalDate.of(2015, 12, 25));
    }

    @ParameterizedTest(name = "Throws when all fields are [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Throws when all date fields are absent or blank")
    void shouldThrowWhenNoFieldsAvailable(String absentDate) {
      // Given
      ClaimResponse claimResponse =
          ClaimResponse.builder()
              .caseStartDate(absentDate)
              .representationOrderDate(absentDate)
              .uniqueFileNumber(absentDate)
              .build();
      // When / Then
      assertThatThrownBy(() -> ClaimEffectiveDateUtil.getEffectiveDate(claimResponse))
          .isInstanceOf(EventServiceIllegalArgumentException.class)
          .hasMessageContaining("No fields available to determine effective date");
    }

    @Test
    @DisplayName("Throws when case start date is present but unparseable")
    void shouldThrowWhenCaseStartDateIsInvalid() {
      // Given
      ClaimResponse claimResponse = ClaimResponse.builder().caseStartDate("not-a-date").build();
      // When / Then
      assertThatThrownBy(() -> ClaimEffectiveDateUtil.getEffectiveDate(claimResponse))
          .isInstanceOf(EventServiceIllegalArgumentException.class)
          .hasMessageContaining("case start date");
    }

    @Test
    @DisplayName("Throws when representation order date is present but unparseable")
    void shouldThrowWhenRepOrderDateIsInvalid() {
      // Given
      ClaimResponse claimResponse =
          ClaimResponse.builder().representationOrderDate("not-a-date").build();
      // When / Then
      assertThatThrownBy(() -> ClaimEffectiveDateUtil.getEffectiveDate(claimResponse))
          .isInstanceOf(EventServiceIllegalArgumentException.class)
          .hasMessageContaining("representation order date");
    }
  }

  // ---------------------------------------------------------------------------
  // PROD fee code resolution path
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("PROD fee code claims")
  class ProdClaims {

    @Test
    @DisplayName("Returns case concluded date when present for PROD claim")
    void shouldReturnCaseConcludedDateForProdClaim() {
      // Given
      ClaimResponse claimResponse =
          ClaimResponse.builder().feeCode("PROD").caseConcludedDate("2025-03-15").build();
      // When
      LocalDate result = ClaimEffectiveDateUtil.getEffectiveDate(claimResponse);
      // Then
      assertThat(result).isEqualTo(LocalDate.of(2025, 3, 15));
    }

    @ParameterizedTest(name = "Falls through when caseConcludedDate is [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName(
        "Falls back to standard path when case concluded date is absent or blank for PROD claim")
    void shouldFallBackToStandardPathWhenCaseConcludedDateAbsent(String caseConcludedDate) {
      // Given — PROD claim with no concluded date but a valid case start date
      ClaimResponse claimResponse =
          ClaimResponse.builder()
              .feeCode("PROD")
              .caseConcludedDate(caseConcludedDate)
              .caseStartDate("2025-06-01")
              .build();
      // When
      LocalDate result = ClaimEffectiveDateUtil.getEffectiveDate(claimResponse);
      // Then
      assertThat(result).isEqualTo(LocalDate.of(2025, 6, 1));
    }

    @Test
    @DisplayName("Throws when case concluded date is present but unparseable for PROD claim")
    void shouldThrowWhenCaseConcludedDateIsInvalid() {
      // Given
      ClaimResponse claimResponse =
          ClaimResponse.builder().feeCode("PROD").caseConcludedDate("not-a-date").build();
      // When / Then
      assertThatThrownBy(() -> ClaimEffectiveDateUtil.getEffectiveDate(claimResponse))
          .isInstanceOf(EventServiceIllegalArgumentException.class)
          .hasMessageContaining("case concluded date");
    }

    @Test
    @DisplayName("Throws when all fields are absent for PROD claim")
    void shouldThrowWhenNoFieldsAvailableForProdClaim() {
      // Given
      ClaimResponse claimResponse = ClaimResponse.builder().feeCode("PROD").id("claim-123").build();
      // When / Then
      assertThatThrownBy(() -> ClaimEffectiveDateUtil.getEffectiveDate(claimResponse))
          .isInstanceOf(EventServiceIllegalArgumentException.class)
          .hasMessageContaining("No fields available to determine effective date");
    }
  }
}
