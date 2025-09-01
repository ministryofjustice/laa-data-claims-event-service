package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ClaimValidationReportTest {

  ClaimValidationReport claimValidationReport;

  @Nested
  @DisplayName("addError")
  class AddErrorTests {

    @Test
    @DisplayName("Correctly adds an error")
    void correctlyAddsError() {
      // Given
      claimValidationReport =
          new ClaimValidationReport(
              "claimId",
              List.of(ClaimValidationError.INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER));

      // When
      claimValidationReport.addError(ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER);

      // Then
      assertThat(claimValidationReport.getErrors().size()).isEqualTo(2);
      assertThat(claimValidationReport.getErrors())
          .contains(
              ClaimValidationError.INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER,
              ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER);
    }
  }

  @Nested
  @DisplayName("addErrors")
  class AddErrorsTests {

    @Test
    @DisplayName("Correctly adds errors")
    void addErrors() {
      // Given
      claimValidationReport =
          new ClaimValidationReport(
              "claimId",
              List.of(ClaimValidationError.INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER));

      // When
      claimValidationReport.addErrors(
          List.of(
              ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER,
              ClaimValidationError.INVALID_CATEGORY_OF_LAW_AND_FEE_CODE));

      // Then
      assertThat(claimValidationReport.getErrors().size()).isEqualTo(3);
      assertThat(claimValidationReport.getErrors())
          .contains(
              ClaimValidationError.INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER,
              ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER,
              ClaimValidationError.INVALID_CATEGORY_OF_LAW_AND_FEE_CODE);
    }
  }

  @Nested
  @DisplayName("hasErrors")
  class HasErrorsTests {

    @Test
    @DisplayName("Returns true when errors are present")
    void returnsTrueWhenErrorsPresent() {
      // Given
      claimValidationReport =
          new ClaimValidationReport(
              "claimId",
              List.of(ClaimValidationError.INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER));

      // Then
      assertThat(claimValidationReport.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("Returns false when no errors are present")
    void returnsFalseWhenNoErrorsPresent() {
      // Given
      claimValidationReport = new ClaimValidationReport("claimId");

      // Then
      assertThat(claimValidationReport.hasErrors()).isFalse();
    }
  }

  @Nested
  @DisplayName("flagForRetry")
  class FlagForRetryTests {

    @Test
    @DisplayName("Sets flagged for retry to true")
    void setsFlaggedForRetryToTrue() {
      // Given
      claimValidationReport = new ClaimValidationReport("claimId");
      assertThat(claimValidationReport.isFlaggedForRetry()).isFalse();

      // When
      claimValidationReport.flagForRetry();

      // Then
      assertThat(claimValidationReport.isFlaggedForRetry()).isTrue();
    }
  }
}
