package uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

  /**
   * Statuses that leave a submission "live" for duplicate-detection purposes. A submission counts
   * as a potential duplicate unless it has reached one of these terminal states. This mirrors the
   * partial unique index on the Data Claims API database, which enforces uniqueness of (office,
   * area of law, submission period) across every status except these.
   */
  private static final Set<SubmissionStatus> NON_BLOCKING_STATUSES =
      Set.of(SubmissionStatus.VALIDATION_FAILED, SubmissionStatus.REPLACED);

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

    final List<SubmissionBase> duplicates =
        dataClaimsRestClient
            .getSubmissions(
                List.of(submission.getOfficeAccountNumber()),
                submission.getAreaOfLaw(),
                submission.getSubmissionPeriod())
            .getBody()
            .getContent()
            .stream()
            .filter(candidate -> isDifferentSubmission(candidate, submission))
            .filter(this::isLiveSubmission)
            .filter(candidate -> isCreatedBefore(candidate, submission))
            .toList();
    log.debug("Found {} duplicates for submission {}", duplicates.size(), submission);

    return !duplicates.isEmpty();
  }

  private boolean isDifferentSubmission(SubmissionBase candidate, SubmissionResponse submission) {
    return !Objects.equals(candidate.getSubmissionId(), submission.getSubmissionId());
  }

  private boolean isLiveSubmission(SubmissionBase candidate) {
    return !NON_BLOCKING_STATUSES.contains(candidate.getStatus());
  }

  /**
   * Determines whether an existing submission was created before the one under validation.
   *
   * <p>The {@code submitted} timestamp is the value persisted as {@code created_on} on the
   * submission record. Only submissions created strictly earlier are treated as duplicates, which
   * lets the earliest of a set of concurrently submitted duplicates proceed while the later ones
   * are rejected.
   */
  private boolean isCreatedBefore(SubmissionBase candidate, SubmissionResponse submission) {
    OffsetDateTime candidateCreatedOn = candidate.getSubmitted();
    OffsetDateTime submissionCreatedOn = submission.getSubmitted();
    return candidateCreatedOn != null
        && submissionCreatedOn != null
        && candidateCreatedOn.isBefore(submissionCreatedOn);
  }
}
