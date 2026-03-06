package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsevent.util.DisbursementClaimUtil.isDisbursementClaim;
import static uk.gov.justice.laa.dstew.payments.claimsevent.util.DisbursementClaimUtil.submissionPeriodCutoffDate;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationType;

@DisplayName("DisbursementClaimUtil")
class DisbursementClaimUtilTest {

  @Nested
  @DisplayName("isDisbursementClaim")
  class IsDisbursementClaim {

    @Test
    @DisplayName("Should return true when fee type is DISB_ONLY")
    void shouldReturnTrueForDisbursementFeeType() {
      assertThat(isDisbursementClaim(FeeCalculationType.DISB_ONLY.getValue())).isTrue();
    }

    @ParameterizedTest(name = "returns false for non-disbursement fee type: [{0}]")
    @ValueSource(strings = {"FIXED", "HOURLY", "ENHANCED"})
    @DisplayName("Should return false for any non-disbursement fee type")
    void shouldReturnFalseForNonDisbursementFeeTypes(String feeType) {
      assertThat(isDisbursementClaim(feeType)).isFalse();
    }

    @Test
    @DisplayName("Should return false when fee type is null")
    void shouldReturnFalseWhenNull() {
      assertThat(isDisbursementClaim(null)).isFalse();
    }

    @Test
    @DisplayName("Should return false when fee type is an empty string")
    void shouldReturnFalseWhenEmpty() {
      assertThat(isDisbursementClaim("")).isFalse();
    }
  }

  @Nested
  @DisplayName("submissionPeriodCutoffDate")
  class SubmissionPeriodCutoffDate {

    @Test
    @DisplayName("Should return the 20th of the following month for a standard period")
    void shouldReturnTwentiethOfFollowingMonth() {
      // MAY-2025 → 20 JUN-2025
      assertThat(submissionPeriodCutoffDate(YearMonth.of(2025, 5)))
          .isEqualTo(LocalDate.of(2025, 6, 20));
    }

    @Test
    @DisplayName("Should roll over correctly from December into January of the following year")
    void shouldRollOverFromDecemberToJanuary() {
      // DEC-2025 → 20 JAN-2026
      assertThat(submissionPeriodCutoffDate(YearMonth.of(2025, 12)))
          .isEqualTo(LocalDate.of(2026, 1, 20));
    }

    @Test
    @DisplayName("Should always produce the 20th regardless of the day count in the source month")
    void shouldAlwaysProduceDayTwenty() {
      // FEB-2024 (leap year, 29 days) → 20 MAR-2024
      assertThat(submissionPeriodCutoffDate(YearMonth.of(2024, 2)))
          .isEqualTo(LocalDate.of(2024, 3, 20));
      // JAN-2025 (31 days) → 20 FEB-2025
      assertThat(submissionPeriodCutoffDate(YearMonth.of(2025, 1)))
          .isEqualTo(LocalDate.of(2025, 2, 20));
    }

    @Test
    @DisplayName(
        "Should produce the correct cutoff for the example in the Javadoc (JAN-2026 → 20 FEB-2026)")
    void shouldMatchJavadocExample() {
      assertThat(submissionPeriodCutoffDate(YearMonth.of(2026, 1)))
          .isEqualTo(LocalDate.of(2026, 2, 20));
    }

    @Test
    @DisplayName(
        "Should produce the correct Rule B cutoff when applied to anchorPeriod minus 3 months")
    void shouldProduceCorrectRuleBCutoff() {
      // Anchor period MAY-2025: minus 3 = FEB-2025, cutoff = 20 MAR-2025
      YearMonth anchorMinusThree = YearMonth.of(2025, 5).minusMonths(3);
      assertThat(submissionPeriodCutoffDate(anchorMinusThree)).isEqualTo(LocalDate.of(2025, 3, 20));
    }

    @Test
    @DisplayName("Should produce the correct Rule B cutoff for anchor period JAN-2026")
    void shouldProduceCorrectRuleBCutoffForJanuary2026() {
      // Anchor period JAN-2026: minus 3 = OCT-2025, cutoff = 20 NOV-2025
      YearMonth anchorMinusThree = YearMonth.of(2026, 1).minusMonths(3);
      assertThat(submissionPeriodCutoffDate(anchorMinusThree))
          .isEqualTo(LocalDate.of(2025, 11, 20));
    }
  }
}
