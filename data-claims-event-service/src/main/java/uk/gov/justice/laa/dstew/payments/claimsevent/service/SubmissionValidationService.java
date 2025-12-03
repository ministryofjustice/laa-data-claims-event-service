package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionErrorCode;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionClaim;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission.SubmissionValidator;
import uk.gov.laa.springboot.metrics.aspect.annotations.CounterMetric;
import uk.gov.laa.springboot.metrics.aspect.annotations.SummaryMetric;

/**
 * A service responsible for validating claim submissions. Any errors found during validation will
 * be reported against the appropriate claims. Once validation is complete, the claim status and any
 * validation errors will be sent to the Data Claims API.
 */
@Slf4j
@Service
@AllArgsConstructor
public class SubmissionValidationService {

  private final ClaimValidationService claimValidationService;
  private final BulkClaimUpdater bulkClaimUpdater;
  private final DataClaimsRestClient dataClaimsRestClient;
  private final List<SubmissionValidator> submissionValidatorList;

  /**
   * Validates a claim submission inside the provided submissionResponse.
   *
   * @param submissionId the ID of the submission to validate
   */
  @SummaryMetric(
      metricName = "submission_validation_time",
      hintText = "Total time taken to validate claim (Include FSP validation time)")
  public SubmissionValidationContext validateSubmission(UUID submissionId) {
    log.debug("Validating submission {}", submissionId);

    SubmissionResponse submission = dataClaimsRestClient.getSubmission(submissionId).getBody();
    Assert.notNull(submission, "Submission not retrievable: " + submissionId.toString());
    SubmissionValidationContext context = initialiseValidationContext(submission);

    // Currently validating:
    // - Submission Status (Has highest priority to update the submission status if required)
    // - Submission Schema
    // - Nil submissions
    submissionValidatorList.stream()
        .sorted(Comparator.comparingInt(SubmissionValidator::priority))
        .forEach(validator -> validator.validate(submission, context));

    // Only validate claims if no submission level validation errors have been recorded.
    if (hasNoSubmissionErrors(context)) {
      claimValidationService.validateAndUpdateClaims(submission, context);
    }

    // Update submission and bulk submission status after completion
    var bulkSubmissionId = submission.getBulkSubmissionId();
    SubmissionPatch submissionPatch = new SubmissionPatch().submissionId(submissionId);
    BulkSubmissionPatch bulkSubmissionPatch =
        new BulkSubmissionPatch().bulkSubmissionId(bulkSubmissionId);
    if (context.hasErrors()) {
      updatePatchToValidationFailed(
          submissionId, context, submissionPatch, bulkSubmissionPatch, bulkSubmissionId);
    } else {
      updatePatchToValidationSucceeded(submissionId, submissionPatch, bulkSubmissionPatch);
    }

    return context;
  }

  @CounterMetric(
      metricName = "submissions_with_errors",
      hintText = "Total number submissions with validation errors",
      conditionalOnReturn = "false")
  private static boolean hasNoSubmissionErrors(SubmissionValidationContext context) {
    return !context.hasSubmissionLevelErrors();
  }

  @CounterMetric(
      metricName = "valid_submissions",
      hintText = "Total number submission which are valid")
  private void updatePatchToValidationSucceeded(
      UUID submissionId, SubmissionPatch submissionPatch, BulkSubmissionPatch bulkSubmissionPatch) {
    log.debug("Validation completed for submission {} with no errors", submissionId);
    submissionPatch.status(SubmissionStatus.VALIDATION_SUCCEEDED);
    bulkSubmissionPatch.status(BulkSubmissionStatus.VALIDATION_SUCCEEDED);
  }

  @CounterMetric(
      metricName = "invalid_submissions",
      hintText = "Total number submission which are invalid")
  private void updatePatchToValidationFailed(
      UUID submissionId,
      SubmissionValidationContext context,
      SubmissionPatch submissionPatch,
      BulkSubmissionPatch bulkSubmissionPatch,
      UUID bulkSubmissionId) {
    log.debug(
        "Validation completed for submission {} with errors: {}",
        submissionId,
        context.getSubmissionValidationErrors());
    submissionPatch
        .status(SubmissionStatus.VALIDATION_FAILED)
        .validationMessages(context.getSubmissionValidationErrors());
    bulkSubmissionPatch
        .status(BulkSubmissionStatus.VALIDATION_FAILED)
        .errorCode(BulkSubmissionErrorCode.V100)
        .errorDescription(
            "Validation completed for bulk submission %s with errors".formatted(bulkSubmissionId));
  }

  private SubmissionValidationContext initialiseValidationContext(SubmissionResponse submission) {
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    if (submission.getClaims() == null) {
      return submissionValidationContext;
    }
    List<ClaimValidationReport> claimReports =
        submission.getClaims().stream()
            .filter(claim -> ClaimStatus.READY_TO_PROCESS.equals(claim.getStatus()))
            .map(SubmissionClaim::getClaimId)
            .map(UUID::toString)
            .map(ClaimValidationReport::new)
            .toList();
    submissionValidationContext.addClaimReports(claimReports);
    return submissionValidationContext;
  }
}
