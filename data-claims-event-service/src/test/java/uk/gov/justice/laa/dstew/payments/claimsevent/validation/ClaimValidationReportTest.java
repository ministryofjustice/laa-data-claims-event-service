package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessageType;

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
              "claimId", List.of(INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER.toPatch()));

      // When
      claimValidationReport.addError(INVALID_AREA_OF_LAW_FOR_PROVIDER);

      // Then
      assertThat(claimValidationReport.getMessages()).hasSize(2);
      assertThat(claimValidationReport.getMessages())
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .containsExactlyInAnyOrder(
              INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER.getDisplayMessage(),
              INVALID_AREA_OF_LAW_FOR_PROVIDER.getDisplayMessage());
    }
  }

  @Nested
  @DisplayName("addErrors")
  class AddErrorsTests {

    @Test
    @DisplayName("Correctly adds multiple errors")
    void addErrors() {
      // Given
      claimValidationReport =
          new ClaimValidationReport(
              "claimId", List.of(INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER.toPatch()));

      // When
      claimValidationReport.addErrors(
          List.of(INVALID_AREA_OF_LAW_FOR_PROVIDER, INVALID_CATEGORY_OF_LAW_AND_FEE_CODE));

      // Then
      assertThat(claimValidationReport.getMessages()).hasSize(3);
      assertThat(claimValidationReport.getMessages())
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .containsExactlyInAnyOrder(
              INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER.getDisplayMessage(),
              INVALID_AREA_OF_LAW_FOR_PROVIDER.getDisplayMessage(),
              INVALID_CATEGORY_OF_LAW_AND_FEE_CODE.getDisplayMessage());
    }
  }

  @Nested
  @DisplayName("addMessages")
  class AddMessagesTests {

    @Test
    @DisplayName("Correctly adds prebuilt message patches")
    void addMessages() {
      // Given
      final ValidationMessagePatch patch1 = INVALID_DATE_IN_UNIQUE_FILE_NUMBER.toPatch();
      final ValidationMessagePatch patch2 = INVALID_FEE_CALCULATION_VALIDATION_FAILED.toPatch();
      claimValidationReport = new ClaimValidationReport("claimId");

      // When
      claimValidationReport.addMessages(List.of(patch1, patch2));

      // Then
      assertThat(claimValidationReport.getMessages()).hasSize(2);
      assertThat(claimValidationReport.getMessages())
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .containsExactlyInAnyOrder(
              INVALID_DATE_IN_UNIQUE_FILE_NUMBER.getDisplayMessage(),
              INVALID_FEE_CALCULATION_VALIDATION_FAILED.getDisplayMessage());
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
              "claimId", List.of(INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER.toPatch()));

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

    @Test
    @DisplayName("Ignores non-error messages when checking for errors")
    void ignoresNonErrorMessages() {
      // Given
      final ValidationMessagePatch nonErrorPatch =
          new ValidationMessagePatch()
              .displayMessage("Info message")
              .technicalMessage("technical")
              .source("source")
              .type(ValidationMessageType.WARNING);

      claimValidationReport = new ClaimValidationReport("claimId", List.of(nonErrorPatch));

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
