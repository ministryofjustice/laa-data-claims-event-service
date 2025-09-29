package uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Validates that a submission's nil flag is set correctly.
 *
 * <p>Validation on this component includes:
 *
 * <ul>
 *   <li>If the submission is nil, it must not contain any claims
 *   <li>If the submission is not nil, it must contain at least one claim
 * </ul>
 *
 * @author Jamie Briggs
 */
@Component
@Slf4j
public class NilSubmissionValidator implements SubmissionValidator {

  @Override
  public void validate(final SubmissionResponse submission, SubmissionValidationContext context) {
    log.debug("Validating nil submission for submission {}", submission.getSubmissionId());
    if (Boolean.TRUE.equals(submission.getIsNilSubmission())) {
      if (submission.getClaims() != null && !submission.getClaims().isEmpty()) {
        context.addSubmissionValidationError(
            ClaimValidationError.INVALID_NIL_SUBMISSION_CONTAINS_CLAIMS);
      }
    } else if (Boolean.FALSE.equals(submission.getIsNilSubmission())
        && (submission.getClaims() == null || submission.getClaims().isEmpty())) {
      context.addSubmissionValidationError(
          ClaimValidationError.NON_NIL_SUBMISSION_CONTAINS_NO_CLAIMS);
    }
    log.debug("Nil submission completed for submission {}", submission.getSubmissionId());
  }

  @Override
  public int priority() {
    return 10;
  }
}
