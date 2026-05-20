package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.model.ValidationIssue;
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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.EventServiceMetricService;
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

  private final ValidationService validationService;
  private final ClaimValidationService claimValidationService;
  private final BulkClaimUpdater bulkClaimUpdater;
  private final DataClaimsRestClient dataClaimsRestClient;
  private final List<SubmissionValidator> submissionValidatorList;
  private final EventServiceMetricService eventServiceMetricService;

  /**
   * Validates a claim submission inside the provided submissionResponse.
   *
   * @param submissionId the ID of the submission to validate
   */
  public SubmissionValidationContext validateSubmission(UUID submissionId) {
    log.debug("Validating submission {}", submissionId);
    eventServiceMetricService.startSubmissionValidationTimer(submissionId);

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

    compareSubmissionValidationResults(submissionId, submission, context);

    // Only validate claims if no submission level validation errors have been recorded.
    if (!context.hasSubmissionLevelErrors()) {
      claimValidationService.validateAndUpdateClaims(submission, context);
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
      eventServiceMetricService.incrementTotalInvalidSubmissions();
      bulkSubmissionPatch
          .status(BulkSubmissionStatus.VALIDATION_FAILED)
          .errorCode(BulkSubmissionErrorCode.V100)
          .errorDescription(
              "Validation completed for bulk submission %s with errors"
                  .formatted(bulkSubmissionId));
    } else {
      log.debug("Validation completed for submission {} with no errors", submissionId);
      submissionPatch.status(SubmissionStatus.VALIDATION_SUCCEEDED);
      eventServiceMetricService.incrementTotalValidSubmissions();
      bulkSubmissionPatch.status(BulkSubmissionStatus.VALIDATION_SUCCEEDED);
    }

    // Record what submission errors were found
    context
        .getSubmissionValidationErrors()
        .forEach(x -> eventServiceMetricService.recordValidationMessage(x, false));
    // Stop submission validation timer
    eventServiceMetricService.stopSubmissionValidationTimer(submissionId);

    dataClaimsRestClient.updateSubmission(submissionId.toString(), submissionPatch);
    dataClaimsRestClient.updateBulkSubmission(
        String.valueOf(bulkSubmissionId), bulkSubmissionPatch);
    return context;
  }

  /**
   * Dry-run comparison: runs the new submission-level validator and compares its issues against the
   * existing submission validation errors already recorded in the context. Any differences are
   * logged as WARN; if everything matches it logs at DEBUG and continues silently.
   */
  private void compareSubmissionValidationResults(
      UUID submissionId, SubmissionResponse submission, SubmissionValidationContext context) {

    ValidationResult validationResult = validationService.validateSubmission(submission);
    List<ValidationIssue> newIssues =
        validationResult != null && validationResult.getIssues() != null
            ? new ArrayList<>(validationResult.getIssues())
            : new ArrayList<>();

    List<ValidationMessagePatch> existingErrors =
        new ArrayList<>(context.getSubmissionValidationErrors());

    // remove exact matches
    Iterator<ValidationIssue> newIt = newIssues.iterator();
    while (newIt.hasNext()) {
      ValidationIssue ni = newIt.next();
      ValidationMessagePatch match = findExactSubmissionMatch(ni, existingErrors);
      if (match != null) {
        newIt.remove();
        existingErrors.remove(match);
      }
    }

    if (newIssues.isEmpty() && existingErrors.isEmpty()) {
      log.debug("Submission {} validators matched exactly at submission level", submissionId);
      return;
    }

    log.warn(
        "Submission {} mismatch at submission level [new={}, existing={}]",
        submissionId,
        newIssues.size(),
        existingErrors.size());
    newIssues.forEach(
        ni ->
            log.warn(
                "Submission {} only in new: code={} severity={} message={} technical={}",
                submissionId,
                ni.getCode(),
                ni.getSeverity(),
                ni.getMessage(),
                ni.getTechnicalMessage()));
    existingErrors.forEach(
        em ->
            log.warn(
                "Submission {} only in existing: source={} type={} display={} technical={}",
                submissionId,
                em.getSource(),
                em.getType(),
                em.getDisplayMessage(),
                em.getTechnicalMessage()));
  }

  private ValidationMessagePatch findExactSubmissionMatch(
      ValidationIssue ni, List<ValidationMessagePatch> existing) {
    String sev = ni.getSeverity() == null ? null : ni.getSeverity().name();
    return existing.stream()
        .filter(
            em -> {
              String type = em.getType() == null ? null : em.getType().name();
              return Objects.equals(
                      normaliseText(ni.getMessage()), normaliseText(em.getDisplayMessage()))
                  && Objects.equals(
                      normaliseText(ni.getTechnicalMessage()),
                      normaliseText(em.getTechnicalMessage()))
                  && Objects.equals(sev, type);
            })
        .findFirst()
        .orElse(null);
  }

  private String normaliseText(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().replaceAll("\\s+", " ");
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
