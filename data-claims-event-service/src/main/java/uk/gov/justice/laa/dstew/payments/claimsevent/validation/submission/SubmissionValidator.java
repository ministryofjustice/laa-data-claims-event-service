package uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission;

import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

public interface SubmissionValidator {
  void validate(final SubmissionResponse submission, SubmissionValidationContext context);
  int priority();
}
