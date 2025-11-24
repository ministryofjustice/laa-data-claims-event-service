package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.assertContextClaimError;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionClaim;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.EventServiceMetricService;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission.SubmissionValidator;

@ExtendWith(MockitoExtension.class)
class SubmissionValidationServiceTest {

  @Mock private ClaimValidationService claimValidationService;

  @Mock private BulkClaimUpdater bulkClaimUpdater;

  @Mock private DataClaimsRestClient dataClaimsRestClient;

  @Mock private SubmissionValidator submissionValidator;

  @Mock private EventServiceMetricService eventServiceMetricService;

  private SubmissionValidationService submissionValidationService;

  @BeforeEach
  void beforeEach() {
    submissionValidationService =
        new SubmissionValidationService(
            claimValidationService,
            bulkClaimUpdater,
            dataClaimsRestClient,
            singletonList(submissionValidator),
            eventServiceMetricService);
  }

  @Nested
  @DisplayName("validateSubmission")
  class ValidateSubmissionTests {

    @Test
    @DisplayName("Should have submission validation errors and not validate claims")
    void shouldHaveSubmissionValidationErrorsAndNotValidateClaims() {
      // Given
      UUID submissionId = new UUID(0, 0);
      UUID claimId = new UUID(2, 0);
      SubmissionResponse submission = buildSubmission(submissionId, claimId, false);
      when(dataClaimsRestClient.getSubmission(submissionId))
          .thenReturn(ResponseEntity.ok(submission));
      doAnswer(
              invocation -> {
                SubmissionValidationContext context = invocation.getArgument(1);
                context.addSubmissionValidationError(
                    SubmissionValidationError.SUBMISSION_PERIOD_MISSING);
                return null;
              })
          .when(submissionValidator)
          .validate(any(), any());
      // When
      SubmissionValidationContext submissionValidationContext =
          submissionValidationService.validateSubmission(submissionId);
      // Then
      assertTrue(submissionValidationContext.hasErrors());
      assertContextClaimError(
          submissionValidationContext, SubmissionValidationError.SUBMISSION_PERIOD_MISSING);
      verify(claimValidationService, times(0)).validateClaims(any(), any());
      // we need to update and mark the claims as invalid when the submission is invalid.

    }

    @Test
    @DisplayName("Should have no validation errors")
    void testNoValidationErrors() {
      boolean isNilSubmission = false;
      ClaimStatus claimStatus = ClaimStatus.VALID;
      // Given
      UUID submissionId = new UUID(0, 0);
      UUID claimId =
          claimStatus != null ? new UUID(1, 1) : null; // only create claimId if there is a claim
      SubmissionResponse submission = buildSubmission(submissionId, claimId, isNilSubmission);

      when(dataClaimsRestClient.getSubmission(submissionId))
          .thenReturn(ResponseEntity.of(Optional.of(submission)));

      SubmissionValidationContext result;

      if (claimId != null) {
        ClaimPatch claimPatch = new ClaimPatch().id(claimId.toString()).status(claimStatus);

        // When
        result = submissionValidationService.validateSubmission(submissionId);

        // Then
        verifyCommonInteractions(submission, result);
      } else {
        // When
        result = submissionValidationService.validateSubmission(submission.getSubmissionId());
      }
    }

    private SubmissionResponse buildSubmission(
        UUID submissionId, UUID claimId, boolean isNilSubmission) {
      SubmissionClaim claim = new SubmissionClaim();
      claim.setClaimId(claimId);
      claim.setStatus(ClaimStatus.READY_TO_PROCESS);

      return getSubmission(
          SubmissionStatus.READY_FOR_VALIDATION,
          submissionId,
          AreaOfLaw.LEGAL_HELP,
          "officeAccountNumber",
          isNilSubmission,
          List.of(claim));
    }

    private SubmissionPatch buildSubmissionPatch(UUID submissionId) {
      return new SubmissionPatch()
          .submissionId(submissionId)
          .status(SubmissionStatus.VALIDATION_IN_PROGRESS);
    }

    private void verifyCommonInteractions(
        SubmissionResponse submissionResponse, SubmissionValidationContext context) {
      verify(claimValidationService, times(1)).validateClaims(eq(submissionResponse), any());
      verify(claimValidationService, times(1))
          .validateClaims(eq(submissionResponse), any(SubmissionValidationContext.class));
    }
  }

  private static SubmissionResponse getSubmission(
      SubmissionStatus submissionStatus,
      UUID submissionId,
      AreaOfLaw areaOfLaw,
      String officeAccountNumber,
      boolean isNilSubmission,
      List<SubmissionClaim> claims) {
    return SubmissionResponse.builder()
        .submissionId(submissionId)
        .areaOfLaw(areaOfLaw)
        .officeAccountNumber(officeAccountNumber)
        .status(submissionStatus)
        .isNilSubmission(isNilSubmission)
        .claims(claims)
        .build();
  }
}
