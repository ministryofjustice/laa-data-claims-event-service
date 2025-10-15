package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.assertContextClaimError;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResultSet;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationType;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy.AbstractDuplicateClaimValidatorStrategy;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.fee.scheme.model.FeeDetailsResponse;

@ExtendWith(MockitoExtension.class)
class DuplicateClaimCivilDisbursementValidationStrategyTest
    extends AbstractDuplicateClaimValidatorStrategy {

  @Mock FeeSchemePlatformRestClient feeSchemePlatformRestClient;
  @Mock DataClaimsRestClient dataClaimsRestClient;

  @InjectMocks DuplicateClaimCivilDisbursementValidationStrategy duplicateClaimValidationService;

  void stubIsDisbursementClaim(boolean isDisbursement) {
    FeeCalculationType feeType =
        isDisbursement ? FeeCalculationType.DISBURSEMENT_ONLY : FeeCalculationType.FIXED;
    when(feeSchemePlatformRestClient.getFeeDetails(any()))
        .thenReturn(ResponseEntity.ok(new FeeDetailsResponse().feeType(feeType.toString())));
  }

  @Nested
  class ValidClaim {

    @Test
    @DisplayName("Should have no errors when current claim not disbursement")
    void shouldHaveNoErrorsWhenCurrentClaimNotDisbursement() {
      // Given
      var claimTobeProcessed =
          createClaim(
              "claimId1",
              "CIV123",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025");
      stubIsDisbursementClaim(false);
      SubmissionValidationContext context = new SubmissionValidationContext();
      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claimTobeProcessed, Collections.emptyList(), "1", context);
      // Then
      assertThat(context.hasErrors()).isFalse();
      verify(dataClaimsRestClient, times(0))
          .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName(
        "Should have no errors when no other previous claims and no other current "
            + "submission claims")
    void shouldHaveNoErrorsWhenNoOtherClaimsAndNoCurrentSubmissionClaims() {
      // Given
      var claimTobeProcessed =
          createClaim(
              "claimId1",
              "CIV123",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025");
      stubIsDisbursementClaim(true);
      SubmissionValidationContext context = new SubmissionValidationContext();

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(
                  Optional.of(new ClaimResultSet().content(Collections.emptyList()))));
      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claimTobeProcessed, Collections.emptyList(), "1", context);
      // Then
      assertThat(context.hasErrors()).isFalse();
      verify(dataClaimsRestClient, times(1))
          .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName(
        "Should have no errors when no other previous claims and exact claims on current "
            + "submission - Validated elsewhere")
    void shouldHaveNoErrorsWhenNoOtherClaimsAndExactClaimsOnCurrentSubmission() {
      // Given
      var claimTobeProcessed =
          createClaim(
              "claimId1",
              "CIV123",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025");
      var previousClaim =
          createClaim(
              "claimId1",
              "CIV123",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025");
      stubIsDisbursementClaim(true);
      SubmissionValidationContext context = new SubmissionValidationContext();

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(
                  Optional.of(new ClaimResultSet().content(Collections.emptyList()))));
      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claimTobeProcessed, List.of(previousClaim), "1", context);
      // Then
      assertThat(context.hasErrors()).isFalse();
      verify(dataClaimsRestClient, times(1))
          .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should have no errors when duplicate claim but older than three months")
    void shouldHaveNoErrorsWhenDuplicateClaimButOlderThanThreeMonths() {
      // Given
      var claimTobeProcessed =
          createClaim(
              "claimId1",
              "CIV123",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025");
      var duplicateClaimOnPreviousSubmission =
          createClaim(
              "claimId1",
              "CIV123",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS,
              "FEB-2025");
      stubIsDisbursementClaim(true);
      SubmissionValidationContext context = new SubmissionValidationContext();

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(
                  Optional.of(
                      new ClaimResultSet()
                          .content(singletonList(duplicateClaimOnPreviousSubmission)))));
      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claimTobeProcessed, emptyList(), "1", context);
      // Then
      assertThat(context.hasErrors()).isFalse();
      verify(dataClaimsRestClient, times(1))
          .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should have no errors when duplicate claim but exactly 1 year older")
    void shouldHaveNoErrorsWhenDuplicateClaimButOneYearOlder() {
      // Given
      var claimTobeProcessed =
          createClaim(
              "claimId1",
              "CIV123",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025");
      var duplicateClaimOnPreviousSubmission =
          createClaim(
              "claimId1",
              "CIV123",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2024");
      stubIsDisbursementClaim(true);
      SubmissionValidationContext context = new SubmissionValidationContext();

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(
                  Optional.of(
                      new ClaimResultSet()
                          .content(singletonList(duplicateClaimOnPreviousSubmission)))));
      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claimTobeProcessed, emptyList(), "1", context);
      // Then
      assertThat(context.hasErrors()).isFalse();
      verify(dataClaimsRestClient, times(1))
          .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
  }

  @Nested
  @DisplayName("Invalid claims")
  class InvalidClaims {

    @Test
    @DisplayName("Should have errors when duplicate claim but younger than three months")
    void shouldHaveNoErrorsWhenDuplicateClaimButOlderThanThreeMonths() {
      // Given
      var claimTobeProcessed =
          createClaim(
              "claimId1",
              "CIV123",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025");
      var duplicateClaimOnPreviousSubmission =
          createClaim(
              "claimId1",
              "CIV123",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS,
              "MAR-2025");
      stubIsDisbursementClaim(true);
      SubmissionValidationContext context = new SubmissionValidationContext();

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(
                  Optional.of(
                      new ClaimResultSet()
                          .content(singletonList(duplicateClaimOnPreviousSubmission)))));
      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claimTobeProcessed, emptyList(), "1", context);
      // Then
      assertThat(context.hasErrors()).isTrue();
      verify(dataClaimsRestClient, times(1))
          .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
      assertContextClaimError(
          context,
          "claimId1",
          ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
    }
  }
}
