package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SubmissionValidationContextTest {

  SubmissionValidationContext submissionValidationContext;

  @Nested
  @DisplayName("addClaimError")
  class AddClaimErrorTests {

    @Test
    @DisplayName("Handles errors for new claim reports")
    void handlesClaimErrorForNewClaim() {
      // Given
      submissionValidationContext = new SubmissionValidationContext();

      assertThat(submissionValidationContext.getClaimReports()).isEmpty();

      // When
      submissionValidationContext.addClaimError(
          "claimId", ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER);

      // Then
      ClaimValidationReport expected =
          new ClaimValidationReport(
              "claimId", List.of(ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER));

      assertThat(submissionValidationContext.getClaimReports()).hasSize(1);
      assertThat(submissionValidationContext.getClaimReports().getFirst()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Handles errors for existing claim reports")
    void handlesClaimErrorForExistingClaim() {
      // Given
      submissionValidationContext = new SubmissionValidationContext();

      assertThat(submissionValidationContext.getClaimReports()).isEmpty();

      submissionValidationContext.addClaimError(
          "claimId", ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER);

      assertThat(submissionValidationContext.getClaimReports()).hasSize(1);

      // When
      submissionValidationContext.addClaimError(
          "claimId", ClaimValidationError.INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER);

      // Then
      ClaimValidationReport expected =
          new ClaimValidationReport(
              "claimId",
              List.of(
                  ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER,
                  ClaimValidationError.INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER));

      assertThat(submissionValidationContext.getClaimReports()).hasSize(1);
      assertThat(submissionValidationContext.getClaimReports().getFirst()).isEqualTo(expected);
    }
  }

  @Nested
  @DisplayName("addClaimReports")
  class AddClaimReportsTest {

    @Test
    @DisplayName("Correctly adds a claim report")
    void correctlyAddsClaimReports() {
      // Given
      submissionValidationContext = new SubmissionValidationContext();

      assertThat(submissionValidationContext.getClaimReports()).isEmpty();

      // When
      ClaimValidationReport claimValidationReport1 = new ClaimValidationReport("claimId1");
      ClaimValidationReport claimValidationReport2 = new ClaimValidationReport("claimId2");
      submissionValidationContext.addClaimReports(
          List.of(claimValidationReport1, claimValidationReport2));

      // Then
      assertThat(submissionValidationContext.getClaimReports()).hasSize(2);
      assertThat(submissionValidationContext.getClaimReports())
          .isEqualTo(List.of(claimValidationReport1, claimValidationReport2));
    }
  }

  @Nested
  @DisplayName("addToAllClaimReports")
  class AddToAllClaimReportsTests {

    @Test
    @DisplayName("Correctly adds error to all existing claims")
    void correctlyAddsErrorToAllClaims() {
      // Given
      submissionValidationContext = new SubmissionValidationContext();

      assertThat(submissionValidationContext.getClaimReports()).isEmpty();

      ClaimValidationReport claimValidationReport1 = new ClaimValidationReport("claimId1");
      ClaimValidationReport claimValidationReport2 = new ClaimValidationReport("claimId2");
      submissionValidationContext.addClaimReports(
          List.of(claimValidationReport1, claimValidationReport2));

      assertThat(submissionValidationContext.getClaimReports()).hasSize(2);

      // When
      submissionValidationContext.addToAllClaimReports(
          ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER);

      // Then
      List<ClaimValidationReport> expected =
          List.of(
              new ClaimValidationReport(
                  "claimId1", List.of(ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER)),
              new ClaimValidationReport(
                  "claimId2", List.of(ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER)));
      assertThat(submissionValidationContext.getClaimReports()).hasSize(2);
      assertThat(submissionValidationContext.getClaimReports()).isEqualTo(expected);
    }
  }

  @Nested
  @DisplayName("getClaimReport")
  class GetClaimReportTests {

    @Test
    @DisplayName("Correctly gets the claim report for the given claim ID")
    void correctlyGetsReportForTheGivenClaimId() {
      // Given
      submissionValidationContext = new SubmissionValidationContext();

      ClaimValidationReport claimValidationReport1 =
          new ClaimValidationReport(
              "claimId1", List.of(ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER));
      ClaimValidationReport claimValidationReport2 =
          new ClaimValidationReport(
              "claimId2", List.of(ClaimValidationError.INVALID_CATEGORY_OF_LAW_AND_FEE_CODE));
      submissionValidationContext.addClaimReports(
          List.of(claimValidationReport1, claimValidationReport2));

      // When
      Optional<ClaimValidationReport> actual =
          submissionValidationContext.getClaimReport("claimId1");
      assertThat(actual.isPresent()).isTrue();
      assertThat(actual.get()).isEqualTo(claimValidationReport1);
    }
  }

  @Nested
  @DisplayName("hasErrors")
  class HasErrorsTests {

    @Test
    @DisplayName("Returns true if the claim report with the given claim ID has errors")
    void returnsTrueForClaimReportsWithErrors() {
      // Given
      submissionValidationContext = new SubmissionValidationContext();
      submissionValidationContext.addClaimReports(
          List.of(
              new ClaimValidationReport(
                  "claimId", List.of(ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER))));

      // Then
      assertThat(submissionValidationContext.getClaimReports()).hasSize(1);
      assertThat(submissionValidationContext.hasErrors("claimId")).isTrue();
    }

    @Test
    @DisplayName("Returns false if the claim report with the given claim ID does not have errors")
    void returnsFalseForClaimReportsWithoutErrors() {
      // Given
      submissionValidationContext = new SubmissionValidationContext();
      submissionValidationContext.addClaimReports(List.of(new ClaimValidationReport("claimId")));

      // Then
      assertThat(submissionValidationContext.getClaimReports()).hasSize(1);
      assertThat(submissionValidationContext.hasErrors("claimId")).isFalse();
    }

    @Test
    @DisplayName("Returns false no claim report exists for the given claim ID")
    void returnsFalseForMissingClaimReports() {
      // Given
      submissionValidationContext = new SubmissionValidationContext();

      // Then
      assertThat(submissionValidationContext.hasErrors("claimId")).isFalse();
    }

    @Test
    @DisplayName("Returns false when none of the claim reports in the context have errors")
    void returnsFalseWhenNoneOfTheClaimReportsHaveErrors() {
      // Given
      submissionValidationContext = new SubmissionValidationContext();

      ClaimValidationReport claimValidationReport1 = new ClaimValidationReport("claimId1");
      ClaimValidationReport claimValidationReport2 = new ClaimValidationReport("claimId2");
      submissionValidationContext.addClaimReports(
          List.of(claimValidationReport1, claimValidationReport2));

      // Then
      assertThat(submissionValidationContext.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Returns true when any of the claim reports in the context have errors")
    void returnsTrueWhenAnyOfTheClaimReportsHaveErrors() {
      // Given
      submissionValidationContext = new SubmissionValidationContext();

      ClaimValidationReport claimValidationReport1 =
          new ClaimValidationReport(
              "claimId1", List.of(ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER));
      ClaimValidationReport claimValidationReport2 = new ClaimValidationReport("claimId2");
      submissionValidationContext.addClaimReports(
          List.of(claimValidationReport1, claimValidationReport2));

      // Then
      assertThat(submissionValidationContext.hasErrors()).isTrue();
    }
  }
}
