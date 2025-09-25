package uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError.SUBMISSION_STATE_IS_NULL;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Validates that a submission's status is valid for the given action. Also updates a submission
 * when validation has begun.
 *
 * @author Jamie Briggs
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionStatusValidator implements SubmissionValidator {

  final DataClaimsRestClient dataClaimsRestClient;

  @Override
  public void validate(final SubmissionResponse submission, SubmissionValidationContext context) {
    SubmissionStatus currentStatus = submission.getStatus();
    UUID submissionId = submission.getSubmissionId();
    switch (currentStatus) {
      case VALIDATION_IN_PROGRESS ->
          log.debug(
              "Submission {} already under validation. Attempting to complete validation.",
              submissionId);
      case READY_FOR_VALIDATION -> {
        log.debug(
            "Submission {} ready for validation. Updating status to VALIDATION_IN_PROGRESS.",
            submissionId);
        updateSubmissionStatus(submissionId, SubmissionStatus.VALIDATION_IN_PROGRESS);
      }
      case null -> {
        log.debug("Submission {} state is null", submissionId);
        context.addSubmissionValidationError(SUBMISSION_STATE_IS_NULL);
      }
      default -> {
        log.debug(
            "Submission {} cannot be validated in its current state: {}",
            submissionId,
            currentStatus);
        context.addSubmissionValidationError(
            "Submission cannot be validated in state " + currentStatus);
      }
    }
  }

  @Override
  public int priority() {
    return 1;
  }

  private void updateSubmissionStatus(UUID submissionId, SubmissionStatus submissionStatus) {
    SubmissionPatch submissionPatch =
        new SubmissionPatch().submissionId(submissionId).status(submissionStatus);
    dataClaimsRestClient.updateSubmission(submissionId.toString(), submissionPatch);
  }
}
