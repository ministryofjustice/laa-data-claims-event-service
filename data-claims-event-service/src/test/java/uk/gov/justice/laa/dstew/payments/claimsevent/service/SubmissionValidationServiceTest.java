package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.model.ValidationResult;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.service.ValidationService;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionStatus;
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

  @Mock private DataClaimsRestClient dataClaimsRestClient;

  @Mock private SubmissionValidator submissionValidator;

  @Mock private EventServiceMetricService eventServiceMetricService;

  @Mock private ValidationService validationService;

  private SubmissionValidationService submissionValidationService;

  @BeforeEach
  void beforeEach() {
    submissionValidationService =
        new SubmissionValidationService(
            validationService,
            claimValidationService,
            dataClaimsRestClient,
            singletonList(submissionValidator),
            eventServiceMetricService);
    // Ensure ValidationService.validateSubmission returns a non-null ValidationResult so
    // SubmissionValidationService can proceed without NullPointerException during tests.
    // Lenient because the VALIDATED_PENDING_APPROVAL idempotency no-op path returns before this is
    // used.
    org.mockito.Mockito.lenient()
        .when(validationService.validateSubmission(any()))
        .thenReturn(ValidationResult.builder().isValid(true).issues(List.of()).build());
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
      verify(claimValidationService, times(0)).validateAndUpdateClaims(any(), any());
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

        // Passing INITIAL validation holds the submission and its bulk submission in
        // VALIDATED_PENDING_APPROVAL (awaiting the provider's final approval) rather than accepting
        // them.
        ArgumentCaptor<SubmissionPatch> submissionPatchCaptor =
            ArgumentCaptor.forClass(SubmissionPatch.class);
        verify(dataClaimsRestClient)
            .updateSubmission(eq(submissionId.toString()), submissionPatchCaptor.capture());
        assertThat(submissionPatchCaptor.getValue().getStatus())
            .isEqualTo(SubmissionStatus.VALIDATED_PENDING_APPROVAL);

        ArgumentCaptor<BulkSubmissionPatch> bulkSubmissionPatchCaptor =
            ArgumentCaptor.forClass(BulkSubmissionPatch.class);
        verify(dataClaimsRestClient)
            .updateBulkSubmission(any(), bulkSubmissionPatchCaptor.capture());
        assertThat(bulkSubmissionPatchCaptor.getValue().getStatus())
            .isEqualTo(BulkSubmissionStatus.VALIDATED_PENDING_APPROVAL);

        // The submission status is what the idempotency guard keys off, so it must be written
        // last. If the bulk submission patch were to fail after the submission had already moved
        // to VALIDATED_PENDING_APPROVAL, the redelivered message would short-circuit and leave the
        // bulk submission stranded in VALIDATION_IN_PROGRESS.
        InOrder inOrder = Mockito.inOrder(dataClaimsRestClient);
        inOrder.verify(dataClaimsRestClient).updateBulkSubmission(any(), any());
        inOrder.verify(dataClaimsRestClient).updateSubmission(any(), any());
      } else {
        // When
        result = submissionValidationService.validateSubmission(submission.getSubmissionId());
      }
    }

    @Test
    @DisplayName(
        "Should be a no-op when the submission is already held in VALIDATED_PENDING_APPROVAL (retry)")
    void shouldNotReprocessSubmissionAlreadyValidatedPendingApproval() {
      // Given a redelivered validation message for a submission already awaiting final approval
      UUID submissionId = new UUID(0, 0);
      SubmissionResponse submission =
          getSubmission(
              SubmissionStatus.VALIDATED_PENDING_APPROVAL,
              submissionId,
              AreaOfLaw.LEGAL_HELP,
              "officeAccountNumber",
              false,
              List.of());
      when(dataClaimsRestClient.getSubmission(submissionId))
          .thenReturn(ResponseEntity.of(Optional.of(submission)));

      // When
      SubmissionValidationContext result =
          submissionValidationService.validateSubmission(submissionId);

      // Then no re-validation, no status change and no further event/patch is produced.
      assertThat(result.hasErrors()).isFalse();
      verify(claimValidationService, never()).validateAndUpdateClaims(any(), any());
      verify(submissionValidator, never()).validate(any(), any());
      verify(dataClaimsRestClient, never()).updateSubmission(any(), any());
      verify(dataClaimsRestClient, never()).updateBulkSubmission(any(), any());
    }

    @Test
    @DisplayName(
        "Should be a no-op when the submission is already VALIDATION_SUCCEEDED (legacy retry)")
    void shouldNotReprocessLegacyValidatedSubmission() {
      // With legacy coercion enabled in the Claims API, the pending-approval patch is stored as
      // VALIDATION_SUCCEEDED. A redelivered validation message must remain idempotent.
      UUID submissionId = new UUID(0, 0);
      SubmissionResponse submission =
          getSubmission(
              SubmissionStatus.VALIDATION_SUCCEEDED,
              submissionId,
              AreaOfLaw.LEGAL_HELP,
              "officeAccountNumber",
              false,
              List.of());
      when(dataClaimsRestClient.getSubmission(submissionId))
          .thenReturn(ResponseEntity.of(Optional.of(submission)));

      SubmissionValidationContext result =
          submissionValidationService.validateSubmission(submissionId);

      assertThat(result.hasErrors()).isFalse();
      verify(claimValidationService, never()).validateAndUpdateClaims(any(), any());
      verify(submissionValidator, never()).validate(any(), any());
      verify(dataClaimsRestClient, never()).updateSubmission(any(), any());
      verify(dataClaimsRestClient, never()).updateBulkSubmission(any(), any());
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
      verify(claimValidationService, times(1))
          .validateAndUpdateClaims(eq(submissionResponse), any());
      verify(claimValidationService, times(1))
          .validateAndUpdateClaims(eq(submissionResponse), any(SubmissionValidationContext.class));
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
