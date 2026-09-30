package uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.assertContextClaimError;

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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResultSet;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationType;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate.DuplicateClaimCrimeLowerValidationServiceStrategy;

@ExtendWith(MockitoExtension.class)
class DuplicateClaimCrimeLowerValidationServiceStrategyTest {

  @Mock DataClaimsRestClient dataClaimsRestClient;

  @InjectMocks DuplicateClaimCrimeLowerValidationServiceStrategy duplicateClaimValidationService;

  @Nested
  @DisplayName("validateDuplicateClaims")
  class ValidateDuplicateClaimsTests {

    private void verifyGetClaimsCalledWith(ClaimResponse claim, String officeCode) {
      verify(dataClaimsRestClient)
          .getClaims(
              eq(officeCode),
              eq(null),
              eq(
                  List.of(
                      SubmissionStatus.CREATED,
                      SubmissionStatus.VALIDATION_IN_PROGRESS,
                      SubmissionStatus.READY_FOR_VALIDATION,
                      SubmissionStatus.VALIDATION_SUCCEEDED)),
              eq(claim.getFeeCode()),
              eq(claim.getUniqueFileNumber()),
              eq(null),
              eq(null),
              eq(List.of(ClaimStatus.READY_TO_PROCESS, ClaimStatus.VALID)),
              eq(null));
    }

    @Test
    @DisplayName("Crime Lower claims - successful validation does not update context")
    void crimeLowerClaimSuccessfulValidation() {
      // Given
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claimId1")
              .feeCode("feeCode1")
              .uniqueFileNumber("ufn1")
              .status(ClaimStatus.READY_TO_PROCESS);
      ClaimResponse claim2 =
          new ClaimResponse()
              .id("claimId2")
              .feeCode("feeCode2")
              .uniqueFileNumber("ufn2")
              .status(ClaimStatus.READY_TO_PROCESS);

      List<ClaimResponse> submissionClaims = List.of(claim1, claim2);

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(new ClaimResultSet())));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "officeCode", context, FeeCalculationType.FIXED.toString());

      // Then
      assertThat(context.hasErrors()).isFalse();
      verifyGetClaimsCalledWith(claim1, "officeCode");
    }

    @Test
    @DisplayName(
        "Crime Lower claims - different fee code but the same unique file number passes validation")
    void crimeLowerClaimDifferentFeeCodeButSameUfnPassesValidation() {
      // Given
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claimId1")
              .feeCode("feeCode1")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);
      ClaimResponse claim2 =
          new ClaimResponse()
              .id("claimId2")
              .feeCode("feeCode2")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);

      List<ClaimResponse> submissionClaims = List.of(claim1, claim2);

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(new ClaimResultSet())));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "officeCode", context, FeeCalculationType.FIXED.toString());

      // Then
      assertThat(context.hasErrors()).isFalse();
      verifyGetClaimsCalledWith(claim1, "officeCode");
    }

    @Test
    @DisplayName(
        "Crime Lower claims - different unique file number but the same fee code passes validation")
    void crimeLowerClaimDifferentUfnButSameFeeCodePassesValidation() {
      // Given
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claimId1")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn1")
              .status(ClaimStatus.READY_TO_PROCESS);
      ClaimResponse claim2 =
          new ClaimResponse()
              .id("claimId2")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn2")
              .status(ClaimStatus.READY_TO_PROCESS);

      List<ClaimResponse> submissionClaims = List.of(claim1, claim2);

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(new ClaimResultSet())));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "officeCode", context, FeeCalculationType.FIXED.toString());

      // Then
      assertThat(context.hasErrors()).isFalse();
      verifyGetClaimsCalledWith(claim1, "officeCode");
    }

    @Test
    @DisplayName(
        "Crime Lower claims - duplicate claims in submission results in claim error added to "
            + "validation "
            + "context")
    void crimeLowerClaimDuplicateInSubmissionResultsInClaimErrorAddedToContext() {
      // Given
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claimId1")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);
      ClaimResponse claim2 =
          new ClaimResponse()
              .id("claimId2")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);

      List<ClaimResponse> submissionClaims = List.of(claim1, claim2);

      ClaimResultSet claimResultSet = new ClaimResultSet();
      // The API is expected to filter out invalid claims; return only non-invalid claims
      claimResultSet.content(submissionClaims);

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(claimResultSet)));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "officeCode", context, FeeCalculationType.FIXED.toString());

      // Then
      assertThat(context.hasErrors(claim1.getId())).isTrue();
      assertContextClaimError(
          context,
          claim1.getId(),
          ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_EXISTING_SUBMISSION);
      verifyGetClaimsCalledWith(claim1, "officeCode");
    }

    @Test
    @DisplayName("Crime Lower claims - duplicate validation ignores invalid claims")
    void crimeLowerClaimDuplicateValidationIgnoresInvalidClaims() {
      // Given
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claimId1")
              .submissionId("submissionId")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);
      ClaimResponse claim2 =
          new ClaimResponse()
              .id("claimId2")
              .submissionId("submissionId")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.INVALID);

      List<ClaimResponse> submissionClaims = List.of(claim1, claim2);

      ClaimResultSet claimResultSet = new ClaimResultSet();
      // API is expected to filter out invalid claims; return only non-invalid claims
      claimResultSet.content(List.of(claim1));

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(claimResultSet)));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "officeCode", context, FeeCalculationType.FIXED.toString());

      // Then
      assertThat(context.hasErrors()).isFalse();
    }

    @Test
    @DisplayName(
        "Crime Lower claims - duplicate claims in another submission results in claim error added"
            + " to "
            + "validation context")
    void crimeLowerClaimDuplicateInAnotherSubmissionResultsInClaimErrorAddedToContext() {
      // Given
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claimId1")
              .submissionId("submissionId")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);

      ClaimResponse otherClaim =
          new ClaimResponse()
              .id("claimId2")
              .submissionId("submissionId2")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.VALID);

      List<ClaimResponse> submissionClaims = List.of(claim1);

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(List.of(otherClaim));

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(claimResultSet)));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim1.getId())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "officeCode", context, FeeCalculationType.FIXED.toString());

      // Then
      assertThat(context.hasErrors(claim1.getId())).isTrue();
      assertContextClaimError(
          context,
          claim1.getId(),
          ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
      verifyGetClaimsCalledWith(claim1, "officeCode");
    }

    @Test
    @DisplayName("Crime Lower claims - does not reprocess submission claims")
    void crimeLowerClaimDuplicateDoesNotReprocessSubmissionClaims() {
      // Given
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claimId1")
              .submissionId("submissionId")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);
      ClaimResponse claim2 =
          new ClaimResponse()
              .id("claimId2")
              .submissionId("submissionId")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);
      ClaimResponse otherClaim =
          new ClaimResponse()
              .id("claimId2")
              .submissionId("submissionId")
              .feeCode("feeCode")
              .uniqueFileNumber("ufn")
              .status(ClaimStatus.READY_TO_PROCESS);

      List<ClaimResponse> submissionClaims = List.of(claim1, claim2);

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(List.of(otherClaim));

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(claimResultSet)));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      // When
      duplicateClaimValidationService.validateDuplicateClaims(
          claim1, submissionClaims, "officeCode", context, FeeCalculationType.FIXED.toString());

      // Then
      assertContextClaimError(
          context,
          claim1.getId(),
          ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_EXISTING_SUBMISSION);
      assertThat(context.hasErrors(claim2.getId())).isFalse();
      verifyGetClaimsCalledWith(claim1, "officeCode");
    }

    @Nested
    @DisplayName("Ignore PROD Fee Code")
    class IgnoreProdFeeCode {

      @Test
      @DisplayName("Crime lower claims - Fee Code PROD success validation")
      void crimeLowerClaimDuplicateWithProdFeeCodeSuccess() {
        // Given
        ClaimResponse claim1 =
            new ClaimResponse()
                .id("claimId1")
                .feeCode("PROD")
                .caseConcludedDate("caseConcludedDate1")
                .status(ClaimStatus.READY_TO_PROCESS);
        ClaimResponse claim2 =
            new ClaimResponse()
                .id("claimId2")
                .feeCode("PROD")
                .caseConcludedDate("caseConcludedDate2")
                .status(ClaimStatus.READY_TO_PROCESS);

        List<ClaimResponse> submissionClaims = List.of(claim1, claim2);

        SubmissionValidationContext context = new SubmissionValidationContext();

        // When
        duplicateClaimValidationService.validateDuplicateClaims(
            claim1, submissionClaims, "officeCode", context, FeeCalculationType.FIXED.toString());

        // Then
        assertThat(context.hasErrors()).isFalse();
        verify(dataClaimsRestClient, times(0))
            .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
      }

      @Test
      @DisplayName(
          "Crime lower claims - Fee Code PROD passes validation, duplicate in same submission")
      void crimeLowerClaimDuplicateWithProdFeeCodeDuplicateInSameSubmission() {
        // Given
        ClaimResponse claim1 =
            new ClaimResponse()
                .id("claimId1")
                .feeCode("PROD")
                .caseConcludedDate("caseConcludedDate1")
                .status(ClaimStatus.READY_TO_PROCESS);
        ClaimResponse claim2 =
            new ClaimResponse()
                .id("claimId2")
                .feeCode("PROD")
                .caseConcludedDate("caseConcludedDate1")
                .status(ClaimStatus.READY_TO_PROCESS);

        List<ClaimResponse> submissionClaims = List.of(claim1, claim2);

        SubmissionValidationContext context = new SubmissionValidationContext();

        // When
        duplicateClaimValidationService.validateDuplicateClaims(
            claim1, submissionClaims, "officeCode", context, FeeCalculationType.FIXED.toString());

        // Then
        assertThat(context.hasErrors()).isFalse();
        verify(dataClaimsRestClient, times(0))
            .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
      }

      @Test
      @DisplayName(
          "Crime lower claims - Fee Code PROD passes validation, duplicate in another submission")
      void crimeLowerClaimDuplicateWithProdFeeCodeDuplicateInAnotherSubmission() {
        // Given
        ClaimResponse claim1 =
            new ClaimResponse()
                .id("claimId1")
                .submissionId("submissionId")
                .feeCode("PROD")
                .caseConcludedDate("caseConcludedDate1")
                .status(ClaimStatus.READY_TO_PROCESS);

        ClaimResponse otherClaim =
            new ClaimResponse()
                .id("claimId2")
                .submissionId("submissionId2")
                .feeCode("PROD")
                .caseConcludedDate("caseConcludedDate1")
                .status(ClaimStatus.VALID);

        List<ClaimResponse> submissionClaims = List.of(claim1);

        ClaimResultSet claimResultSet = new ClaimResultSet();
        claimResultSet.content(List.of(otherClaim));

        SubmissionValidationContext context = new SubmissionValidationContext();
        context.addClaimReports(
            List.of(
                new ClaimValidationReport(claim1.getId()),
                new ClaimValidationReport(claim1.getId())));

        // When
        duplicateClaimValidationService.validateDuplicateClaims(
            claim1, submissionClaims, "officeCode", context, FeeCalculationType.FIXED.toString());

        // Then
        assertThat(context.hasErrors(claim1.getId())).isFalse();
        verify(dataClaimsRestClient, times(0))
            .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
      }
    }

    @Nested
    @DisplayName("Flagged for retry short-circuit")
    class FlaggedForRetry {

      @Test
      @DisplayName(
          "Crime Lower claims - skips duplicate check entirely and makes no API call when claim "
              + "is flagged for retry")
      void skipsDuplicateCheckWhenFlaggedForRetry() {
        // Given
        ClaimResponse claim1 =
            new ClaimResponse()
                .id("claimId1")
                .submissionId("submissionId")
                .feeCode("feeCode")
                .uniqueFileNumber("ufn")
                .status(ClaimStatus.READY_TO_PROCESS);

        SubmissionValidationContext context = new SubmissionValidationContext();
        context.addClaimReports(List.of(new ClaimValidationReport(claim1.getId())));
        // Simulates an earlier validator (EffectiveCategoryOfLawClaimValidator, via
        // CategoryOfLawValidationService) flagging the claim for retry due to a transient Fee
        // Scheme Platform error, before the duplicate validator runs.
        context.flagForRetry(claim1.getId());

        // When
        duplicateClaimValidationService.validateDuplicateClaims(
            claim1, List.of(claim1), "officeCode", context, FeeCalculationType.FIXED.toString());

        // Then
        assertThat(context.hasErrors(claim1.getId())).isFalse();
        verify(dataClaimsRestClient, times(0))
            .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
      }

      @Test
      @DisplayName("Crime Lower claims - still performs duplicate check when not flagged for retry")
      void stillPerformsDuplicateCheckWhenNotFlaggedForRetry() {
        // Given
        ClaimResponse claim1 =
            new ClaimResponse()
                .id("claimId1")
                .submissionId("submissionId")
                .feeCode("feeCode")
                .uniqueFileNumber("ufn")
                .status(ClaimStatus.READY_TO_PROCESS);

        SubmissionValidationContext context = new SubmissionValidationContext();
        context.addClaimReports(List.of(new ClaimValidationReport(claim1.getId())));

        when(dataClaimsRestClient.getClaims(
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(ResponseEntity.of(Optional.of(new ClaimResultSet())));

        // When
        duplicateClaimValidationService.validateDuplicateClaims(
            claim1, List.of(claim1), "officeCode", context, FeeCalculationType.FIXED.toString());

        // Then
        verify(dataClaimsRestClient, times(1))
            .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
      }
    }

    @Nested
    @DisplayName(
        "Regression: same-submission duplicate detection is independent of pagination "
            + "(DSTEW-1717)")
    class PaginationIndependence {

      @Test
      @DisplayName(
          "Detects a same-submission duplicate even when the duplicate is NOT present in the "
              + "page-local submissionClaims list passed by ClaimValidationService — proving the "
              + "check no longer depends on which page of the submission is currently being "
              + "processed")
      void detectsSameSubmissionDuplicateNotPresentInLocalPage() {
        // Given: two claims in the SAME submission share feeCode + UFN (a genuine duplicate),
        // but claim2 is simulated as living on a *different page* of ClaimValidationService's
        // paginated fetch than claim1 - i.e. it is absent from the submissionClaims list passed
        // in for claim1's validation. Before this fix, main's implementation compared the
        // current claim only against this local list and so could never detect this duplicate.
        ClaimResponse claim1 =
            new ClaimResponse()
                .id("claimId1")
                .submissionId("submissionId")
                .feeCode("feeCode")
                .uniqueFileNumber("ufn")
                .status(ClaimStatus.READY_TO_PROCESS);
        ClaimResponse claim2OnAnotherPage =
            new ClaimResponse()
                .id("claimId2")
                .submissionId("submissionId")
                .feeCode("feeCode")
                .uniqueFileNumber("ufn")
                .status(ClaimStatus.READY_TO_PROCESS);

        // The page-local list visible to ClaimValidationService for this page contains only
        // claim1 - claim2 is on a different page and therefore not present here.
        List<ClaimResponse> pageLocalSubmissionClaims = List.of(claim1);

        // The Data Claims API is unpaginated for this targeted, filtered lookup, so it returns
        // both matching claims regardless of which page ClaimValidationService is currently on.
        ClaimResultSet claimResultSet = new ClaimResultSet();
        claimResultSet.content(List.of(claim2OnAnotherPage));
        when(dataClaimsRestClient.getClaims(
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(ResponseEntity.of(Optional.of(claimResultSet)));

        SubmissionValidationContext context = new SubmissionValidationContext();
        context.addClaimReports(List.of(new ClaimValidationReport(claim1.getId())));

        // When
        duplicateClaimValidationService.validateDuplicateClaims(
            claim1,
            pageLocalSubmissionClaims,
            "officeCode",
            context,
            FeeCalculationType.FIXED.toString());

        // Then
        assertThat(context.hasErrors(claim1.getId())).isTrue();
        assertContextClaimError(
            context,
            claim1.getId(),
            ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_EXISTING_SUBMISSION);
      }
    }
  }
}
