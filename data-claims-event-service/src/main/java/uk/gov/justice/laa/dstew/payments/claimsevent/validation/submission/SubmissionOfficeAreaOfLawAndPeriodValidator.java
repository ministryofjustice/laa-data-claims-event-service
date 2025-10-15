package uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionBase;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationError;

/** Check for duplicates based on the combination of Office × Area of Law × Submission Period. */
@Component
@Slf4j
@RequiredArgsConstructor
public class SubmissionOfficeAreaOfLawAndPeriodValidator implements SubmissionValidator {
  private final DataClaimsRestClient dataClaimsRestClient;

  @Override
  public void validate(SubmissionResponse submission, SubmissionValidationContext context) {

    if (isDuplicateSubmission(submission)) {
      context.addSubmissionValidationError(
          SubmissionValidationError.SUBMISSION_ALREADY_EXISTS,
          submission.getOfficeAccountNumber(),
          submission.getAreaOfLaw(),
          submission.getSubmissionPeriod());
    }
  }

  @Override
  public int priority() {
    return 100;
  }

  private Boolean isDuplicateSubmission(SubmissionResponse submission) {

    final List<SubmissionBase> submissionBases =
        dataClaimsRestClient
            .getSubmissions(
                List.of(submission.getOfficeAccountNumber()),
                null,
                null,
                null,
                submission.getAreaOfLaw(),
                submission.getSubmissionPeriod(),
                0,
                0,
                null)
            .getBody()
            .getContent()
            .stream()
            .filter(
                submissionBase ->
                    Objects.equals(
                        submissionBase.getStatus(), SubmissionStatus.VALIDATION_SUCCEEDED))
            .toList();
    log.debug("Found {} duplicates for submission {}", submissionBases.size(), submission);

    return !submissionBases.isEmpty();
  }
}
