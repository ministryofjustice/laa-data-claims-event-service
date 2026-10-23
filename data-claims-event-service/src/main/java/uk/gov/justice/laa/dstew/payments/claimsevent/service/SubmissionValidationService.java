package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.model.ValidationResult;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.service.ValidationService;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionErrorCode;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionClaim;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.SubmissionEventProcessingException;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.EventServiceMetricService;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.ValidationResultComparator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission.SubmissionValidator;

/**
 * A service responsible for validating claim submissions. Any errors found during validation will
 * be reported against the appropriate claims. Once validation is complete, the claim status and any
 * validation errors will be sent to the Data Claims API.
 */
@Slf4j
@Service
@AllArgsConstructor
public class SubmissionValidationService {

  private static final Set<ClaimStatus> CLAIM_STATUSES_UNDER_VALIDATION =
      Set.of(ClaimStatus.READY_TO_PROCESS, ClaimStatus.VALIDATED_PENDING_APPROVAL);

  /**
   * Submission statuses that prove initial validation has already completed, so a redelivered
   * initial validation message must be a no-op.
   */
  private static final Set<SubmissionStatus> INITIAL_VALIDATION_COMPLETE_STATUSES =
      Set.of(
          SubmissionStatus.VALIDATED_PENDING_APPROVAL,
          SubmissionStatus.READY_FOR_FINAL_VALIDATION,
          SubmissionStatus.FINAL_VALIDATION_IN_PROGRESS,
          SubmissionStatus.VALIDATION_SUCCEEDED);

  private final ValidationService validationService;
  private final ClaimValidationService claimValidationService;
  private final DataClaimsRestClient dataClaimsRestClient;
  private final List<SubmissionValidator> submissionValidatorList;
  private final EventServiceMetricService eventServiceMetricService;

  /**
   * Validates a claim submission inside the provided submissionResponse.
   *
   * @param submissionId the ID of the submission to validate
   */
  public SubmissionValidationContext validateSubmission(UUID submissionId) {
    return validateSubmission(submissionId, false);
  }

  private SubmissionValidationContext validateSubmission(
      UUID submissionId, boolean finalValidation) {
    log.debug("Validating submission {}", submissionId);
    eventServiceMetricService.startSubmissionValidationTimer(submissionId);

    SubmissionResponse submission = dataClaimsRestClient.getSubmission(submissionId).getBody();
    Assert.notNull(submission, "Submission not retrievable: " + submissionId.toString());

    if (isValidationComplete(submission, finalValidation)) {
      // A previous attempt may have saved submission failure but failed to update the bulk status.
      // Retry that update without rerunning validation.
      if (finalValidation && submission.getStatus() == SubmissionStatus.VALIDATION_FAILED) {
        dataClaimsRestClient.updateBulkSubmission(
            submission.getBulkSubmissionId().toString(),
            failedBulkSubmissionPatch(submission.getBulkSubmissionId()));
      }
      log.debug(
          "Submission {} already completed {} validation in status {}; skipping re-validation "
              + "(no-op).",
          submissionId,
          finalValidation ? "final" : "initial",
          submission.getStatus());
      eventServiceMetricService.stopSubmissionValidationTimer(submissionId);
      return new SubmissionValidationContext();
    }

    if (finalValidation) {
      requireAwaitingFinalValidation(submission);
    }

    SubmissionValidationContext context = initialiseValidationContext(submission);

    // Currently validating:
    // - Submission Status (Has highest priority to update the submission status if required)
    // - Submission Schema
    // - Nil submissions
    submissionValidatorList.stream()
        .sorted(Comparator.comparingInt(SubmissionValidator::priority))
        .forEach(validator -> validator.validate(submission, context));

    compareSubmissionValidationResults(submissionId, submission, context);

    // Only validate claims if no submission level validation errors have been recorded.
    if (!context.hasSubmissionLevelErrors()) {
      claimValidationService.validateAndUpdateClaims(submission, context, finalValidation);
    } else {
      eventServiceMetricService.incrementTotalSubmissionsValidatedWithSubmissionErrors();
    }

    // Update submission and bulk submission status after completion
    var bulkSubmissionId = submission.getBulkSubmissionId();
    SubmissionPatch submissionPatch = new SubmissionPatch().submissionId(submissionId);
    BulkSubmissionPatch bulkSubmissionPatch =
        new BulkSubmissionPatch().bulkSubmissionId(bulkSubmissionId);
    if (context.hasErrors()) {
      log.debug(
          "Validation completed for submission {} with errors: {}",
          submissionId,
          context.getSubmissionValidationErrors());
      log.debug(
          "Validation completed for submission {} with no of claims errors: {}",
          submissionId,
          context.getClaimReports().size());
      submissionPatch
          .status(SubmissionStatus.VALIDATION_FAILED)
          .validationMessages(context.getSubmissionValidationErrors());
      if (finalValidation) {
        eventServiceMetricService.incrementTotalFinalValidationFailed();
      } else {
        eventServiceMetricService.incrementTotalInvalidSubmissions();
      }
      bulkSubmissionPatch = failedBulkSubmissionPatch(bulkSubmissionId);
    } else {
      log.debug("Validation completed for submission {} with no errors", submissionId);
      submissionPatch.status(
          finalValidation
              ? SubmissionStatus.VALIDATION_SUCCEEDED
              : SubmissionStatus.VALIDATED_PENDING_APPROVAL);
      if (finalValidation) {
        eventServiceMetricService.incrementTotalFinalValidationSucceeded();
      } else {
        eventServiceMetricService.incrementTotalValidSubmissions();
      }
      bulkSubmissionPatch.status(
          finalValidation
              ? BulkSubmissionStatus.VALIDATION_SUCCEEDED
              : BulkSubmissionStatus.VALIDATED_PENDING_APPROVAL);
    }

    // Record what submission errors were found
    context
        .getSubmissionValidationErrors()
        .forEach(x -> eventServiceMetricService.recordValidationMessage(x, false));
    // Stop submission validation timer
    eventServiceMetricService.stopSubmissionValidationTimer(submissionId);

    if (context.hasErrors()) {
      // Persist the submission failure first. If the bulk patch then fails, a redelivery sees the
      // terminal submission status and the status validator prevents claim re-validation, thereby
      // preserving the claim results already written by ClaimValidationService.
      dataClaimsRestClient.updateSubmission(submissionId.toString(), submissionPatch);
      dataClaimsRestClient.updateBulkSubmission(
          String.valueOf(bulkSubmissionId), bulkSubmissionPatch);
    } else {
      // Persist the bulk status first. The submission idempotency guard keys off the submission
      // status, so a failed bulk patch leaves the submission retryable and allows the redelivery to
      // recover the bulk status.
      dataClaimsRestClient.updateBulkSubmission(
          String.valueOf(bulkSubmissionId), bulkSubmissionPatch);
      dataClaimsRestClient.updateSubmission(submissionId.toString(), submissionPatch);
    }
    return context;
  }

  /**
   * Revalidates a submission after provider approval and completes the final validation lifecycle.
   *
   * @param submissionId the ID of the submission to validate
   */
  public SubmissionValidationContext finalValidateSubmission(UUID submissionId) {
    return validateSubmission(submissionId, true);
  }

  private BulkSubmissionPatch failedBulkSubmissionPatch(UUID bulkSubmissionId) {
    return new BulkSubmissionPatch()
        .bulkSubmissionId(bulkSubmissionId)
        .status(BulkSubmissionStatus.VALIDATION_FAILED)
        .errorCode(BulkSubmissionErrorCode.V100)
        .errorDescription(
            "Validation completed for bulk submission %s with errors".formatted(bulkSubmissionId));
  }

  private boolean isValidationComplete(SubmissionResponse submission, boolean finalValidation) {
    if (finalValidation) {
      return submission.getStatus() == SubmissionStatus.VALIDATION_SUCCEEDED
          || submission.getStatus() == SubmissionStatus.VALIDATION_FAILED;
    }
    return INITIAL_VALIDATION_COMPLETE_STATUSES.contains(submission.getStatus());
  }

  private void requireAwaitingFinalValidation(SubmissionResponse submission) {
    SubmissionStatus currentStatus = submission.getStatus();
    if (currentStatus != SubmissionStatus.READY_FOR_FINAL_VALIDATION
        && currentStatus != SubmissionStatus.FINAL_VALIDATION_IN_PROGRESS) {
      throw new SubmissionEventProcessingException(
          "Submission "
              + submission.getSubmissionId()
              + " is not awaiting final validation in status: "
              + currentStatus);
    }
  }

  /**
   * Runs the new submission-level validator and compares its issues against the existing submission
   * validation errors recorded in the context. Differences are logged as WARN via {@link
   * ValidationResultComparator}. Any unexpected error is caught and logged to prevent disruption to
   * the existing validation flow.
   */
  private void compareSubmissionValidationResults(
      UUID submissionId, SubmissionResponse submission, SubmissionValidationContext context) {

    try {
      ValidationResult validationResult = validationService.validateSubmission(submission);

      if (validationResult == null) {
        log.warn("Validation service returned null for submission {}", submissionId);
        return;
      }

      var newIssues =
          Optional.of(validationResult).map(ValidationResult::getIssues).orElseGet(List::of);

      ValidationResultComparator.compare(
          "Submission " + submissionId, newIssues, context.getSubmissionValidationErrors());
    } catch (Exception e) {
      log.error(
          "Error during dry-run submission validation comparison for submission {} - existing validation unaffected",
          submissionId,
          e);
    }
  }

  private SubmissionValidationContext initialiseValidationContext(SubmissionResponse submission) {
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    if (submission.getClaims() == null) {
      return submissionValidationContext;
    }
    List<ClaimValidationReport> claimReports =
        submission.getClaims().stream()
            .filter(claim -> CLAIM_STATUSES_UNDER_VALIDATION.contains(claim.getStatus()))
            .map(SubmissionClaim::getClaimId)
            .map(UUID::toString)
            .map(ClaimValidationReport::new)
            .toList();
    submissionValidationContext.addClaimReports(claimReports);
    return submissionValidationContext;
  }
}
