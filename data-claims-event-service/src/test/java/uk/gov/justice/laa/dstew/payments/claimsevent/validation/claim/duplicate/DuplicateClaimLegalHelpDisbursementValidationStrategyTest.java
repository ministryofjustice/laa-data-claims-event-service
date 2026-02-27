package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.assertContextClaimError;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResultSet;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationType;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy.AbstractDuplicateClaimValidatorStrategy;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Tests for {@link DuplicateClaimLegalHelpDisbursementValidationStrategy}.
 *
 * <p>Organised into two groups:
 *
 * <ul>
 *   <li><b>Integration-level scenario tests</b> — {@link ValidClaim}, {@link InvalidClaims}, {@link
 *       TrueDuplicates}, {@link Example1InOrderDuplicate}, {@link Example2NotInOrderDuplicate},
 *       {@link Example3ProductionScenario} — exercise the full {@code validateDuplicateClaims}
 *       pipeline end-to-end.
 *   <li><b>Unit-level method tests</b> — {@link ParseConcludedDate}, {@link GetCutoffPeriod},
 *       {@link IsDuplicateClaimNullGuards}, {@link IsDuplicateClaimBoundary}, {@link
 *       SelectComparativeClaim}, {@link FindEligibleDuplicateClaims}, {@link
 *       ValidateDuplicateClaimsEarlyExits} — target each protected method individually to maximise
 *       branch and null-path coverage.
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class DuplicateClaimLegalHelpDisbursementValidationStrategyTest
    extends AbstractDuplicateClaimValidatorStrategy {

  @Mock FeeSchemePlatformRestClient feeSchemePlatformRestClient;
  @Mock DataClaimsRestClient dataClaimsRestClient;

  @InjectMocks
  DuplicateClaimLegalHelpDisbursementValidationStrategy duplicateClaimValidationService;

  private static final String OFFICE_CODE = "1";
  private static final String FEE_CODE = "CIV123";
  private static final String UFN = "070722/001";
  private static final String UCN = "CLI001";

  // ===========================================================================
  // Integration-level scenario tests
  // ===========================================================================

  @Nested
  class ValidClaim {

    @Test
    @DisplayName("Should have no errors when current claim not disbursement")
    void shouldHaveNoErrorsWhenCurrentClaimNotDisbursement() {
      var claimTobeProcessed =
          createClaim(
              "claimId1",
              "submissionId1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null);
      SubmissionValidationContext context = new SubmissionValidationContext();

      duplicateClaimValidationService.validateDuplicateClaims(
          claimTobeProcessed,
          List.of(claimTobeProcessed),
          OFFICE_CODE,
          context,
          FeeCalculationType.FIXED.toString());

      assertThat(context.hasErrors()).isFalse();
      verify(dataClaimsRestClient, times(0))
          .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName(
        "Should have no errors when no other previous claims and no other current submission claims")
    void shouldHaveNoErrorsWhenNoOtherClaimsAndNoCurrentSubmissionClaims() {
      var claimTobeProcessed =
          createClaim(
              "claimId1",
              "submissionId1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null);
      SubmissionValidationContext context = new SubmissionValidationContext();
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(
                  Optional.of(new ClaimResultSet().content(Collections.emptyList()))));

      duplicateClaimValidationService.validateDuplicateClaims(
          claimTobeProcessed,
          List.of(claimTobeProcessed),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.toString());

      assertThat(context.hasErrors()).isFalse();
      verify(dataClaimsRestClient, times(1))
          .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName(
        "Should have no errors when no other previous claims and exact claims on current submission — validated elsewhere")
    void shouldHaveNoErrorsWhenNoOtherClaimsAndExactClaimsOnCurrentSubmission() {
      var claimTobeProcessed =
          createClaim(
              "claimId1",
              "submissionId1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null);
      var previousClaim =
          createClaim(
              "claimId1",
              "submissionId2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null);
      SubmissionValidationContext context = new SubmissionValidationContext();
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(
                  Optional.of(new ClaimResultSet().content(Collections.emptyList()))));

      duplicateClaimValidationService.validateDuplicateClaims(
          claimTobeProcessed,
          List.of(previousClaim),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.toString());

      assertThat(context.hasErrors()).isFalse();
      verify(dataClaimsRestClient, times(1))
          .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should have no errors when duplicate claim but older than three months")
    void shouldHaveNoErrorsWhenDuplicateClaimButOlderThanThreeMonths() {
      // Anchor = MAY-2025, cutoff = 20 MAR-2025
      // Earlier concluded = 15 Jan 2025 — well before cutoff → Accept
      var claimTobeProcessed =
          createClaim(
              "claimId1",
              "submissionId1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-04-15");
      var duplicateClaimOnPreviousSubmission =
          createClaim(
              "claimId2",
              "submissionId2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "FEB-2025",
              null,
              "2025-01-15");
      SubmissionValidationContext context = new SubmissionValidationContext();
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(
                  Optional.of(
                      new ClaimResultSet()
                          .content(singletonList(duplicateClaimOnPreviousSubmission)))));

      duplicateClaimValidationService.validateDuplicateClaims(
          claimTobeProcessed,
          List.of(claimTobeProcessed),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.toString());

      assertThat(context.hasErrors()).isFalse();
      verify(dataClaimsRestClient, times(1))
          .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should have no errors when duplicate claim but exactly 1 year older")
    void shouldHaveNoErrorsWhenDuplicateClaimButOneYearOlder() {
      // Anchor = MAY-2025, cutoff = 20 MAR-2025
      // Earlier concluded = 15 May 2024 — over a year before cutoff → Accept
      var claimTobeProcessed =
          createClaim(
              "claimId1",
              "submissionId1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-04-15");
      var duplicateClaimOnPreviousSubmission =
          createClaim(
              "claimId2",
              "submissionId2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2024",
              null,
              "2024-05-15");
      SubmissionValidationContext context = new SubmissionValidationContext();
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(
                  Optional.of(
                      new ClaimResultSet()
                          .content(singletonList(duplicateClaimOnPreviousSubmission)))));

      duplicateClaimValidationService.validateDuplicateClaims(
          claimTobeProcessed,
          List.of(claimTobeProcessed),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.toString());

      assertThat(context.hasErrors()).isFalse();
      verify(dataClaimsRestClient, times(1))
          .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
  }

  @Nested
  @DisplayName("Invalid claims")
  class InvalidClaims {

    @Test
    @DisplayName("Should have errors when duplicate claim within three months")
    void shouldHaveErrorsWhenDuplicateClaimWithinThreeMonths() {
      // Anchor = MAY-2025, cutoff = 20 MAR-2025
      // Earlier concluded = 15 Apr 2025 > 20 Mar 2025 → Reject
      var claimTobeProcessed =
          createClaim(
              "claimId1",
              "submissionId1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-04-15");
      var duplicateClaimOnPreviousSubmission =
          createClaim(
              "claimId2",
              "submissionId2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAR-2025",
              null,
              "2025-04-20");
      SubmissionValidationContext context = new SubmissionValidationContext();
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(
                  Optional.of(
                      new ClaimResultSet()
                          .content(singletonList(duplicateClaimOnPreviousSubmission)))));

      duplicateClaimValidationService.validateDuplicateClaims(
          claimTobeProcessed,
          List.of(claimTobeProcessed),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.toString());

      assertThat(context.hasErrors()).isTrue();
      verify(dataClaimsRestClient, times(1))
          .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
      assertContextClaimError(
          context,
          "claimId1",
          ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
    }
  }

  @Nested
  @DisplayName("True Duplicates — same claim submitted twice in the same or adjacent period")
  class TrueDuplicates {

    @Test
    @DisplayName(
        "Should reject when identical claim exists in a previous submission of the same period")
    void shouldRejectIdenticalClaimInSamePeriod() {
      // Both MAY-2025, concluded 15 Apr 2025. Cutoff = 20 MAR-2025 → Reject
      var incoming =
          createClaim(
              "claimId1",
              "submissionId1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-04-15");
      var matched =
          createClaim(
              "claimId2",
              "submissionId2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-04-15");
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(Optional.of(new ClaimResultSet().content(singletonList(matched)))));

      SubmissionValidationContext context = new SubmissionValidationContext();
      duplicateClaimValidationService.validateDuplicateClaims(
          incoming,
          List.of(incoming),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.getValue());

      assertThat(context.hasErrors()).isTrue();
      assertContextClaimError(
          context,
          "claimId1",
          ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
    }

    @Test
    @DisplayName(
        "Should reject when identical claim exists in the immediately preceding submission period")
    void shouldRejectIdenticalClaimInPrecedingPeriod() {
      // Anchor = JUN-2025, cutoff = 20 APR-2025. Concluded 20 May 2025 > cutoff → Reject
      var incoming =
          createClaim(
              "claimId1",
              "submissionId1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "JUN-2025",
              null,
              "2025-05-20");
      var matched =
          createClaim(
              "claimId2",
              "submissionId2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-05-20");
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(Optional.of(new ClaimResultSet().content(singletonList(matched)))));

      SubmissionValidationContext context = new SubmissionValidationContext();
      duplicateClaimValidationService.validateDuplicateClaims(
          incoming,
          List.of(incoming),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.getValue());

      assertThat(context.hasErrors()).isTrue();
      assertContextClaimError(
          context,
          "claimId1",
          ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
    }

    @Test
    @DisplayName(
        "Should reject when claim with nearly identical concluded date exists within the 3-month window")
    void shouldRejectWhenConcludedDatesAreOneWeekApart() {
      // Anchor = MAY-2025, cutoff = 20 MAR-2025. Earlier concluded = 18 Apr 2025 > cutoff → Reject
      var incoming =
          createClaim(
              "claimId1",
              "submissionId1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-04-25");
      var matched =
          createClaim(
              "claimId2",
              "submissionId2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "APR-2025",
              null,
              "2025-04-18");
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(Optional.of(new ClaimResultSet().content(singletonList(matched)))));

      SubmissionValidationContext context = new SubmissionValidationContext();
      duplicateClaimValidationService.validateDuplicateClaims(
          incoming,
          List.of(incoming),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.getValue());

      assertThat(context.hasErrors()).isTrue();
      assertContextClaimError(
          context,
          "claimId1",
          ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
    }
  }

  @Nested
  @DisplayName("Example 1 — In-Order Duplicate (JAN-2026 submission)")
  class Example1InOrderDuplicate {

    // Incoming: JAN-2026, concluded 01 Dec 2025
    // Matched:  DEC-2025, concluded date varies
    // Anchor = JAN-2026, cutoff = 20 NOV-2025

    private static final String INCOMING_SUBMISSION_PERIOD = "JAN-2026";
    private static final String MATCHED_SUBMISSION_PERIOD = "DEC-2025";
    private static final String INCOMING_CONCLUDED_DATE = "2025-12-01";

    private void stubMatchedClaim(String matchedConcludedDate) {
      var matched =
          createClaim(
              "matchedClaimId",
              "submissionId2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              MATCHED_SUBMISSION_PERIOD,
              null,
              matchedConcludedDate);
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(Optional.of(new ClaimResultSet().content(singletonList(matched)))));
    }

    @Test
    @DisplayName("Should accept when matched concluded date is 10 Nov 2025 — 10 days before cutoff")
    void shouldAcceptWhenMatchedConcludedDateIs10DaysBeforeCutoff() {
      var incoming =
          createClaim(
              "incomingClaimId",
              "submissionId1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              INCOMING_SUBMISSION_PERIOD,
              null,
              INCOMING_CONCLUDED_DATE);
      stubMatchedClaim("2025-11-10");

      SubmissionValidationContext context = new SubmissionValidationContext();
      duplicateClaimValidationService.validateDuplicateClaims(
          incoming,
          List.of(incoming),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.getValue());

      assertThat(context.hasErrors()).isFalse();
    }

    @Test
    @DisplayName(
        "Should accept when matched concluded date is exactly 20 Nov 2025 — on the cutoff (inclusive boundary)")
    void shouldAcceptWhenMatchedConcludedDateIsExactlyOnCutoff() {
      var incoming =
          createClaim(
              "incomingClaimId",
              "submissionId1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              INCOMING_SUBMISSION_PERIOD,
              null,
              INCOMING_CONCLUDED_DATE);
      stubMatchedClaim("2025-11-20");

      SubmissionValidationContext context = new SubmissionValidationContext();
      duplicateClaimValidationService.validateDuplicateClaims(
          incoming,
          List.of(incoming),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.getValue());

      assertThat(context.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Should reject when matched concluded date is 21 Nov 2025 — 1 day after cutoff")
    void shouldRejectWhenMatchedConcludedDateIsOneDayAfterCutoff() {
      var incoming =
          createClaim(
              "incomingClaimId",
              "submissionId1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              INCOMING_SUBMISSION_PERIOD,
              null,
              INCOMING_CONCLUDED_DATE);
      stubMatchedClaim("2025-11-21");

      SubmissionValidationContext context = new SubmissionValidationContext();
      duplicateClaimValidationService.validateDuplicateClaims(
          incoming,
          List.of(incoming),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.getValue());

      assertThat(context.hasErrors()).isTrue();
      assertContextClaimError(
          context,
          "incomingClaimId",
          ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
    }
  }

  @Nested
  @DisplayName("Example 2 — Not-In-Order Duplicate (OCT-2025 anchor)")
  class Example2NotInOrderDuplicate {

    // Matched:  OCT-2025, concluded 10 Nov 2025
    // Incoming: SEP-2025, concluded date varies
    // Anchor = OCT-2025, cutoff = 20 AUG-2025

    private static final String INCOMING_SUBMISSION_PERIOD = "SEP-2025";
    private static final String MATCHED_SUBMISSION_PERIOD = "OCT-2025";
    private static final String MATCHED_CONCLUDED_DATE = "2025-11-10";

    private void stubMatchedClaim() {
      var matched =
          createClaim(
              "matchedClaimId",
              "submissionId2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              MATCHED_SUBMISSION_PERIOD,
              null,
              MATCHED_CONCLUDED_DATE);
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(Optional.of(new ClaimResultSet().content(singletonList(matched)))));
    }

    @Test
    @DisplayName("Should accept when incoming concluded date is 15 Aug 2025 — 5 days before cutoff")
    void shouldAcceptWhenIncomingConcludedDateIs5DaysBeforeCutoff() {
      var incoming =
          createClaim(
              "incomingClaimId",
              "submissionId1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              INCOMING_SUBMISSION_PERIOD,
              null,
              "2025-08-15");
      stubMatchedClaim();

      SubmissionValidationContext context = new SubmissionValidationContext();
      duplicateClaimValidationService.validateDuplicateClaims(
          incoming,
          List.of(incoming),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.getValue());

      assertThat(context.hasErrors()).isFalse();
    }

    @Test
    @DisplayName(
        "Should accept when incoming concluded date is exactly 20 Aug 2025 — on the cutoff (inclusive boundary)")
    void shouldAcceptWhenIncomingConcludedDateIsExactlyOnCutoff() {
      var incoming =
          createClaim(
              "incomingClaimId",
              "submissionId1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              INCOMING_SUBMISSION_PERIOD,
              null,
              "2025-08-20");
      stubMatchedClaim();

      SubmissionValidationContext context = new SubmissionValidationContext();
      duplicateClaimValidationService.validateDuplicateClaims(
          incoming,
          List.of(incoming),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.getValue());

      assertThat(context.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Should reject when incoming concluded date is 21 Aug 2025 — 1 day after cutoff")
    void shouldRejectWhenIncomingConcludedDateIsOneDayAfterCutoff() {
      var incoming =
          createClaim(
              "incomingClaimId",
              "submissionId1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              INCOMING_SUBMISSION_PERIOD,
              null,
              "2025-08-21");
      stubMatchedClaim();

      SubmissionValidationContext context = new SubmissionValidationContext();
      duplicateClaimValidationService.validateDuplicateClaims(
          incoming,
          List.of(incoming),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.getValue());

      assertThat(context.hasErrors()).isTrue();
      assertContextClaimError(
          context,
          "incomingClaimId",
          ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
    }
  }

  @Nested
  @DisplayName("Example 3 — Production Scenario (MAY-2025 incoming vs DEC-2025 in DB)")
  class Example3ProductionScenario {

    // Anchor = DEC-2025, cutoff = 20 OCT-2025
    // Earlier concluded = 31 May 2025 — nearly 5 months before cutoff → Accept

    @Test
    @DisplayName(
        "Should accept MAY-2025 claim when DEC-2025 matched claim exists — concluded dates 7 months apart, well outside the 3-month window")
    void shouldAcceptWhenConcludedDatesAre7MonthsApart() {
      var incoming =
          createClaim(
              "incomingClaimId",
              "submissionId1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-05-31");
      var matched =
          createClaim(
              "matchedClaimId",
              "submissionId2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "DEC-2025",
              null,
              "2025-12-31");
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(Optional.of(new ClaimResultSet().content(singletonList(matched)))));

      SubmissionValidationContext context = new SubmissionValidationContext();
      duplicateClaimValidationService.validateDuplicateClaims(
          incoming,
          List.of(incoming),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.getValue());

      assertThat(context.hasErrors()).isFalse();
    }
  }

  // ===========================================================================
  // Unit-level method tests
  // ===========================================================================

  @Nested
  @DisplayName("parseConcludedDate")
  class ParseConcludedDate {

    @ParameterizedTest(name = "Returns null when caseConcludedDate is [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Returns null when caseConcludedDate is null, empty, or blank")
    void returnsNullWhenNullOrBlank(String value) {
      var claim = new ClaimResponse().caseConcludedDate(value);
      assertThat(DuplicateClaimLegalHelpDisbursementValidationStrategy.parseConcludedDate(claim))
          .isNull();
    }

    @Test
    @DisplayName("Returns null and logs debug when caseConcludedDate cannot be parsed")
    void returnsNullWhenUnparseable() {
      var claim = new ClaimResponse().id("c1").caseConcludedDate("not-a-date");
      assertThat(DuplicateClaimLegalHelpDisbursementValidationStrategy.parseConcludedDate(claim))
          .isNull();
    }

    @Test
    @DisplayName("Returns correct LocalDate when caseConcludedDate is valid yyyy-MM-dd")
    void returnsLocalDateWhenValid() {
      var claim = new ClaimResponse().caseConcludedDate("2025-11-20");
      assertThat(DuplicateClaimLegalHelpDisbursementValidationStrategy.parseConcludedDate(claim))
          .isEqualTo(LocalDate.of(2025, 11, 20));
    }
  }

  @Nested
  @DisplayName("getCutoffPeriod")
  class GetCutoffPeriod {

    static Stream<Arguments> cutoffPeriodCases() {
      return Stream.of(
          Arguments.of(
              YearMonth.of(2025, 6),
              YearMonth.of(2025, 3),
              YearMonth.of(2025, 3),
              "incoming later (JUN-2025) → anchor JUN-2025, cutoff MAR-2025"),
          Arguments.of(
              YearMonth.of(2025, 3),
              YearMonth.of(2025, 6),
              YearMonth.of(2025, 3),
              "matched later (JUN-2025) → anchor JUN-2025, cutoff MAR-2025"),
          Arguments.of(
              YearMonth.of(2025, 5),
              YearMonth.of(2025, 5),
              YearMonth.of(2025, 2),
              "both equal (MAY-2025) → cutoff FEB-2025"),
          Arguments.of(
              YearMonth.of(2026, 1),
              YearMonth.of(2025, 6),
              YearMonth.of(2025, 10),
              "JAN-2026 anchor wraps year boundary → cutoff OCT-2025"));
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("cutoffPeriodCases")
    @DisplayName("Returns anchor minus 3 months")
    @SuppressWarnings("unused")
    void returnsCutoffPeriod(
        YearMonth incoming, YearMonth anchor, YearMonth expected, String description) {
      assertThat(duplicateClaimValidationService.getCutoffPeriod(incoming, anchor))
          .isEqualTo(expected);
    }
  }

  @Nested
  @DisplayName("isDuplicateClaim — null guards")
  class IsDuplicateClaimNullGuards {

    static Stream<Arguments> nullGuardCases() {
      return Stream.of(
          Arguments.of(
              null, "2025-04-15", "MAY-2025", "2025-04-20", "incoming submission period is null"),
          Arguments.of(
              "BADPERIOD",
              "2025-04-15",
              "MAY-2025",
              "2025-04-20",
              "incoming submission period is unparseable"),
          Arguments.of(
              "MAY-2025", "2025-04-15", null, "2025-04-20", "anchor submission period is null"),
          Arguments.of(
              "MAY-2025",
              "2025-04-15",
              "BADPERIOD",
              "2025-04-20",
              "anchor submission period is unparseable"),
          Arguments.of(
              "MAY-2025", null, "APR-2025", "2025-04-20", "incoming concluded date is null"),
          Arguments.of(
              "MAY-2025", "2025-04-15", "APR-2025", null, "anchor concluded date is null"));
    }

    @ParameterizedTest(name = "Returns false when {4}")
    @MethodSource("nullGuardCases")
    @DisplayName("Returns false when any required field is null or unparseable")
    @SuppressWarnings("unused")
    void returnsFalse(
        String incomingPeriod,
        String incomingDate,
        String anchorPeriod,
        String anchorDate,
        String description) {
      var incoming =
          createClaim(
              "c1",
              "s1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              incomingPeriod,
              null,
              incomingDate);
      var anchor =
          createClaim(
              "c2",
              "s2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              anchorPeriod,
              null,
              anchorDate);

      assertThat(duplicateClaimValidationService.isDuplicateClaim(incoming, anchor)).isFalse();
    }
  }

  @Nested
  @DisplayName("isDuplicateClaim — boundary decisions")
  class IsDuplicateClaimBoundary {

    static Stream<Arguments> boundaryCases() {
      return Stream.of(
          Arguments.of(
              "MAY-2025",
              "2025-03-20",
              "APR-2025",
              "2025-04-15",
              false,
              "earlier concluded exactly on cutoff (20 Mar 2025) → accept"),
          Arguments.of(
              "MAY-2025",
              "2025-01-15",
              "APR-2025",
              "2025-04-15",
              false,
              "earlier concluded well before cutoff (15 Jan 2025) → accept"),
          Arguments.of(
              "MAY-2025",
              "2025-03-21",
              "APR-2025",
              "2025-04-15",
              true,
              "earlier concluded 1 day after cutoff (21 Mar 2025) → reject"),
          Arguments.of(
              "SEP-2025",
              "2025-11-10",
              "OCT-2025",
              "2025-08-21",
              true,
              "anchor date earlier and after cutoff — anchor period OCT-2025 → reject"),
          Arguments.of(
              "JUN-2025",
              "2025-05-25",
              "MAY-2025",
              "2025-05-20",
              true,
              "incoming period later (JUN-2025 anchor, cutoff 20 APR-2025) → reject"));
    }

    @ParameterizedTest(name = "{5}")
    @MethodSource("boundaryCases")
    @DisplayName("Returns correct result for boundary cases")
    @SuppressWarnings("unused")
    void returnsCorrectResult(
        String incomingPeriod,
        String incomingDate,
        String anchorPeriod,
        String anchorDate,
        boolean expected,
        String description) {
      var incoming =
          createClaim(
              "c1",
              "s1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              incomingPeriod,
              null,
              incomingDate);
      var anchor =
          createClaim(
              "c2",
              "s2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              anchorPeriod,
              null,
              anchorDate);

      assertThat(duplicateClaimValidationService.isDuplicateClaim(incoming, anchor))
          .isEqualTo(expected);
    }
  }

  @Nested
  @DisplayName("selectComparativeClaim")
  class SelectComparativeClaim {

    @Test
    @DisplayName("Returns null when candidate list is empty")
    void returnsNullWhenEmpty() {
      assertThat(
              duplicateClaimValidationService.selectComparativeClaim(
                  emptyList(), LocalDate.of(2025, 4, 15)))
          .isNull();
    }

    @Test
    @DisplayName("Returns the single candidate when only one exists")
    void returnsSingleCandidate() {
      var claim =
          createClaim(
              "c1",
              "s1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-04-15");

      assertThat(
              duplicateClaimValidationService.selectComparativeClaim(
                  List.of(claim), LocalDate.of(2025, 4, 20)))
          .isEqualTo(claim);
    }

    @Test
    @DisplayName("Returns the candidate whose concluded date is closest to the incoming date")
    void returnsClosestCandidateByDistance() {
      // close: 10 Apr → 5 days away; far: 20 Jan → 84 days away
      var close =
          createClaim(
              "c1",
              "s1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-04-10");
      var far =
          createClaim(
              "c2",
              "s2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAR-2025",
              null,
              "2025-01-20");

      assertThat(
              duplicateClaimValidationService.selectComparativeClaim(
                  List.of(far, close), LocalDate.of(2025, 4, 15)))
          .isEqualTo(close);
    }

    @Test
    @DisplayName("Selects the candidate from the later submission period on a distance tie")
    void prefersLaterSubmissionPeriodOnTie() {
      // Both 5 days from 15 Apr 2025
      var earlierPeriod =
          createClaim(
              "c1",
              "s1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAR-2025",
              null,
              "2025-04-10");
      var laterPeriod =
          createClaim(
              "c2",
              "s2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "APR-2025",
              null,
              "2025-04-20");

      assertThat(
              duplicateClaimValidationService.selectComparativeClaim(
                  List.of(earlierPeriod, laterPeriod), LocalDate.of(2025, 4, 15)))
          .isEqualTo(laterPeriod);
    }

    @Test
    @DisplayName("Sorts candidates with null concluded date last")
    void sortsNullConcludedDateLast() {
      var withDate =
          createClaim(
              "c1",
              "s1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-04-10");
      var withoutDate =
          createClaim(
              "c2", "s2", FEE_CODE, UFN, UCN, ClaimStatus.READY_TO_PROCESS, "MAY-2025", null, null);

      assertThat(
              duplicateClaimValidationService.selectComparativeClaim(
                  List.of(withoutDate, withDate), LocalDate.of(2025, 4, 15)))
          .isEqualTo(withDate);
    }

    @Test
    @DisplayName("Sorts candidates with null submission period last in tie-break")
    void sortsNullSubmissionPeriodLastInTieBreak() {
      // Both 5 days away — null period sorts last
      var nullPeriod =
          createClaim(
              "c1",
              "s1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              null,
              null,
              "2025-04-10");
      var withPeriod =
          createClaim(
              "c2",
              "s2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "APR-2025",
              null,
              "2025-04-20");

      assertThat(
              duplicateClaimValidationService.selectComparativeClaim(
                  List.of(nullPeriod, withPeriod), LocalDate.of(2025, 4, 15)))
          .isEqualTo(withPeriod);
    }
  }

  @Nested
  @DisplayName("findEligibleDuplicateClaims")
  class FindEligibleDuplicateClaims {

    @Test
    @DisplayName("Returns empty list when no previous claims exist")
    void returnsEmptyWhenNoPreviousClaimsExist() {
      var incoming =
          createClaim(
              "c1",
              "s1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-04-15");
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(new ClaimResultSet().content(emptyList()))));

      assertThat(
              duplicateClaimValidationService.findEligibleDuplicateClaims(
                  incoming, List.of(incoming), OFFICE_CODE))
          .isEmpty();
    }

    @Test
    @DisplayName("Includes candidates with a valid concluded date")
    void includesCandidatesWithValidConcludedDate() {
      var incoming =
          createClaim(
              "c1",
              "s1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-04-15");
      var matched =
          createClaim(
              "c2",
              "s2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "APR-2025",
              null,
              "2025-04-10");
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(Optional.of(new ClaimResultSet().content(singletonList(matched)))));

      assertThat(
              duplicateClaimValidationService.findEligibleDuplicateClaims(
                  incoming, List.of(incoming), OFFICE_CODE))
          .containsExactly(matched);
    }

    static Stream<Arguments> ineligibleConcludedDateCases() {
      return Stream.of(
          Arguments.of(null, "null concluded date"),
          Arguments.of("", "empty concluded date"),
          Arguments.of("   ", "blank concluded date"),
          Arguments.of("not-a-date", "unparseable concluded date"));
    }

    @ParameterizedTest(name = "Excludes candidate with {1}")
    @MethodSource("ineligibleConcludedDateCases")
    @DisplayName("Excludes candidates whose concluded date cannot be parsed")
    @SuppressWarnings("unused")
    void excludesCandidatesWithInvalidConcludedDate(String concludedDate, String description) {
      var incoming =
          createClaim(
              "c1",
              "s1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-04-15");
      var ineligible =
          createClaim(
              "c2",
              "s2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "APR-2025",
              null,
              concludedDate);
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(
                  Optional.of(new ClaimResultSet().content(singletonList(ineligible)))));

      assertThat(
              duplicateClaimValidationService.findEligibleDuplicateClaims(
                  incoming, List.of(incoming), OFFICE_CODE))
          .isEmpty();
    }

    @Test
    @DisplayName("Includes only candidates with a valid concluded date when list is mixed")
    void includesOnlyValidCandidatesFromMixedList() {
      var incoming =
          createClaim(
              "c1",
              "s1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-04-15");
      var valid =
          createClaim(
              "c2",
              "s2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "APR-2025",
              null,
              "2025-04-10");
      var noDate =
          createClaim(
              "c3", "s3", FEE_CODE, UFN, UCN, ClaimStatus.READY_TO_PROCESS, "APR-2025", null, null);
      var badDate =
          createClaim(
              "c4",
              "s4",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAR-2025",
              null,
              "bad");
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(
                  Optional.of(new ClaimResultSet().content(List.of(valid, noDate, badDate)))));

      assertThat(
              duplicateClaimValidationService.findEligibleDuplicateClaims(
                  incoming, List.of(incoming), OFFICE_CODE))
          .containsExactly(valid);
    }
  }

  @Nested
  @DisplayName("validateDuplicateClaims — early-exit paths")
  class ValidateDuplicateClaimsEarlyExits {

    @Test
    @DisplayName("Skips all processing when feeType is not a disbursement claim")
    void skipsWhenNotDisbursementClaim() {
      var incoming =
          createClaim(
              "c1",
              "s1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-04-15");

      SubmissionValidationContext context = new SubmissionValidationContext();
      duplicateClaimValidationService.validateDuplicateClaims(
          incoming, List.of(incoming), OFFICE_CODE, context, FeeCalculationType.FIXED.getValue());

      assertThat(context.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Skips Rule B when no eligible candidates exist (all have null concluded dates)")
    void skipsRuleBWhenAllCandidatesHaveNullConcludedDate() {
      var incoming =
          createClaim(
              "c1",
              "s1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-04-15");
      var noDate =
          createClaim(
              "c2", "s2", FEE_CODE, UFN, UCN, ClaimStatus.READY_TO_PROCESS, "APR-2025", null, null);
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(Optional.of(new ClaimResultSet().content(singletonList(noDate)))));

      SubmissionValidationContext context = new SubmissionValidationContext();
      duplicateClaimValidationService.validateDuplicateClaims(
          incoming,
          List.of(incoming),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.getValue());

      assertThat(context.hasErrors()).isFalse();
    }

    static Stream<Arguments> skippableConcludedDateCases() {
      return Stream.of(
          Arguments.of(null, "null concluded date"),
          Arguments.of("not-a-date", "unparseable concluded date"));
    }

    @ParameterizedTest(name = "Skips Rule B when incoming claim has {1}")
    @MethodSource("skippableConcludedDateCases")
    @DisplayName("Skips Rule B when incoming claim has a null or unparseable concluded date")
    @SuppressWarnings("unused")
    void skipsRuleBWhenIncomingConcludedDateInvalid(String concludedDate, String description) {
      var incoming =
          createClaim(
              "c1",
              "s1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              concludedDate);
      var matched =
          createClaim(
              "c2",
              "s2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "APR-2025",
              null,
              "2025-04-20");
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(Optional.of(new ClaimResultSet().content(singletonList(matched)))));

      SubmissionValidationContext context = new SubmissionValidationContext();
      duplicateClaimValidationService.validateDuplicateClaims(
          incoming,
          List.of(incoming),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.getValue());

      assertThat(context.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Raises duplicate error when Rule B boundary is exceeded")
    void raisesDuplicateErrorWhenRuleBBoundaryExceeded() {
      // Anchor = MAY-2025, cutoff = 20 MAR-2025
      // Earlier concluded = 21 Mar 2025 > cutoff → Reject
      var incoming =
          createClaim(
              "c1",
              "s1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-03-21");
      var matched =
          createClaim(
              "c2",
              "s2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "APR-2025",
              null,
              "2025-04-20");
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(Optional.of(new ClaimResultSet().content(singletonList(matched)))));

      SubmissionValidationContext context = new SubmissionValidationContext();
      duplicateClaimValidationService.validateDuplicateClaims(
          incoming,
          List.of(incoming),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.getValue());

      assertThat(context.hasErrors()).isTrue();
      assertContextClaimError(
          context, "c1", ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
    }

    @Test
    @DisplayName("Does not raise error when Rule B boundary is not exceeded")
    void doesNotRaiseErrorWhenRuleBBoundaryNotExceeded() {
      // Anchor = MAY-2025, cutoff = 20 MAR-2025
      // Earlier concluded = 20 Mar 2025 — exactly on cutoff → Accept
      var incoming =
          createClaim(
              "c1",
              "s1",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              null,
              "2025-03-20");
      var matched =
          createClaim(
              "c2",
              "s2",
              FEE_CODE,
              UFN,
              UCN,
              ClaimStatus.READY_TO_PROCESS,
              "APR-2025",
              null,
              "2025-04-20");
      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(Optional.of(new ClaimResultSet().content(singletonList(matched)))));

      SubmissionValidationContext context = new SubmissionValidationContext();
      duplicateClaimValidationService.validateDuplicateClaims(
          incoming,
          List.of(incoming),
          OFFICE_CODE,
          context,
          FeeCalculationType.DISB_ONLY.getValue());

      assertThat(context.hasErrors()).isFalse();
    }
  }
}
