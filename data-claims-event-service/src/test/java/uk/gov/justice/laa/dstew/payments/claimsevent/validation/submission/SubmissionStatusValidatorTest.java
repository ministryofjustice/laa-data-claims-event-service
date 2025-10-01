package uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.assertContextClaimError;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationError;

@ExtendWith(MockitoExtension.class)
@DisplayName("Submission status validator test")
class SubmissionStatusValidatorTest {

  private SubmissionStatusValidator validator;

  @Mock DataClaimsRestClient dataClaimsRestClient;

  @BeforeEach
  void beforeEach() {
    validator = new SubmissionStatusValidator(dataClaimsRestClient);
  }

  @Test
  @DisplayName("Should do nothing if submission status is VALIDATION_IN_PROGRESS")
  void shouldDoNothingIfSubmissionStatusIsValidationInProgress() {
    // Given
    SubmissionStatus status = SubmissionStatus.VALIDATION_IN_PROGRESS;
    SubmissionResponse submissionResponse =
        SubmissionResponse.builder().submissionId(UUID.randomUUID()).status(status).build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    // When
    validator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertFalse(submissionValidationContext.hasErrors());
    verify(dataClaimsRestClient, times(0)).updateSubmission(any(), any());
  }

  @Test
  @DisplayName(
      "Should update the submission status to IN_PROGRESS when submission status is "
          + "READY_FOR_VALIDATION")
  void shouldUpdateTheSubmissionStatusToInProgressWhenSubmissionStatusIsReadyForValidation() {
    // Given
    SubmissionStatus status = SubmissionStatus.READY_FOR_VALIDATION;
    UUID submissionId = UUID.randomUUID();
    SubmissionResponse submissionResponse =
        SubmissionResponse.builder().submissionId(submissionId).status(status).build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    // When
    validator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertFalse(submissionValidationContext.hasErrors());
    SubmissionPatch submissionPatch =
        new SubmissionPatch()
            .submissionId(submissionId)
            .status(SubmissionStatus.VALIDATION_IN_PROGRESS);
    verify(dataClaimsRestClient, times(1)).updateSubmission(any(), eq(submissionPatch));
  }

  @Test
  @DisplayName("Should add errors if submission status is NULL")
  void shouldAddErrorIfSubmissionStatusIsNull() {
    // Given
    SubmissionResponse submissionResponse =
        SubmissionResponse.builder().submissionId(UUID.randomUUID()).status(null).build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    // When
    validator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertTrue(submissionValidationContext.hasErrors());
    assertContextClaimError(
        submissionValidationContext, SubmissionValidationError.SUBMISSION_STATUS_IS_NULL);
    verify(dataClaimsRestClient, times(0)).updateSubmission(any(), any());
  }

  @ParameterizedTest
  @EnumSource(
      value = SubmissionStatus.class,
      names = {"READY_FOR_VALIDATION", "VALIDATION_IN_PROGRESS"},
      mode = EnumSource.Mode.EXCLUDE)
  void shouldAddErrorIfSubmissionStatusUnknownValue(SubmissionStatus status) {
    // Given
    SubmissionResponse submissionResponse =
        SubmissionResponse.builder().submissionId(UUID.randomUUID()).status(status).build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    // When
    validator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertTrue(submissionValidationContext.hasErrors());
    assertContextClaimError(
        submissionValidationContext,
        "Submission cannot be validated in state " + submissionResponse.getStatus());
    verify(dataClaimsRestClient, times(0)).updateSubmission(any(), any());
  }
}
