package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.DCES;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessageType;

/**
 * Holds validation context for a submission during request scope. Contains submission-level and
 * claim-level messages.
 */
@Getter
@RequestScope
@Component
public class SubmissionValidationContext {

  private final List<ClaimValidationReport> claimReports = new ArrayList<>();
  private final List<ValidationMessagePatch> submissionValidationErrors = new ArrayList<>();

  /**
   * Adds a list of submission-level validation errors.
   *
   * @param errors list of validation message patches to add
   */
  public void addSubmissionValidationErrors(List<ValidationMessagePatch> errors) {
    this.submissionValidationErrors.addAll(errors);
  }

  /**
   * Adds a single submission-level validation error message.
   *
   * @param message the error message to add
   */
  public void addSubmissionValidationError(String message) {
    submissionValidationErrors.add(
        new ValidationMessagePatch()
            .displayMessage(message)
            .technicalMessage(message)
            .source(DCES)
            .type(ValidationMessageType.ERROR));
  }

  /**
   * Adds a submission-level validation error from a ClaimValidationError.
   *
   * @param error the validation error to convert and add
   */
  public void addSubmissionValidationError(ClaimValidationError error) {
    submissionValidationErrors.add(error.toPatch());
  }

  /**
   * Adds a claim-level validation error for a specific claim.
   *
   * @param claimId the ID of the claim
   * @param error the validation error to add
   */
  public void addClaimError(String claimId, ClaimValidationError error) {
    addClaimMessages(claimId, List.of(error.toPatch()));
  }

  /**
   * Adds a claim-level error message for a specific claim.
   *
   * @param claimId the ID of the claim
   * @param message the error message to add
   */
  public void addClaimError(String claimId, String message) {
    addClaimMessages(
        claimId,
        List.of(
            new ValidationMessagePatch()
                .displayMessage(message)
                .technicalMessage(message)
                .source(DCES)
                .type(ValidationMessageType.ERROR)));
  }

  /**
   * Adds validation messages for a specific claim.
   *
   * @param claimId the ID of the claim
   * @param messages list of validation message patches to add
   */
  public void addClaimMessages(String claimId, List<ValidationMessagePatch> messages) {
    getClaimReport(claimId)
        .ifPresentOrElse(
            report -> report.addMessages(messages),
            () -> claimReports.add(new ClaimValidationReport(claimId, messages)));
  }

  /**
   * Adds a list of claim validation reports.
   *
   * @param reports list of claim validation reports to add
   */
  public void addClaimReports(List<ClaimValidationReport> reports) {
    claimReports.addAll(reports);
  }

  /**
   * Retrieves the validation report for a specific claim.
   *
   * @param claimId the ID of the claim
   * @return an optional containing the report, if found
   */
  public Optional<ClaimValidationReport> getClaimReport(String claimId) {
    return claimReports.stream().filter(r -> r.getClaimId().equals(claimId)).findFirst();
  }

  /**
   * Flags a specific claim for retry.
   *
   * @param claimId the ID of the claim to flag
   */
  public void flagForRetry(String claimId) {
    getClaimReport(claimId).ifPresent(ClaimValidationReport::flagForRetry);
  }

  /**
   * Checks if a specific claim is flagged for retry.
   *
   * @param claimId the ID of the claim
   * @return true if flagged, false otherwise
   */
  public boolean isFlaggedForRetry(String claimId) {
    return getClaimReport(claimId).map(ClaimValidationReport::isFlaggedForRetry).orElse(false);
  }

  /**
   * Checks if a specific claim is flagged for retry.
   *
   * @param claimId the ID of the claim
   * @return true if flagged, false otherwise
   */
  public boolean hasErrors(String claimId) {
    return getClaimReport(claimId).map(ClaimValidationReport::hasErrors).orElse(false);
  }

  /**
   * Checks if there are any submission-level or claim-level errors.
   *
   * @return true if there are any errors, false otherwise
   */
  public boolean hasErrors() {
    return !submissionValidationErrors.isEmpty()
        || claimReports.stream().anyMatch(ClaimValidationReport::hasErrors);
  }
}
