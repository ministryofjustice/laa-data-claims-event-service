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

  public void addSubmissionValidationErrors(List<ValidationMessagePatch> errors) {
    this.submissionValidationErrors.addAll(errors);
  }

  public void addSubmissionValidationError(String message) {
    submissionValidationErrors.add(
        new ValidationMessagePatch()
            .displayMessage(message)
            .technicalMessage(message)
            .source(DCES)
            .type(ValidationMessageType.ERROR));
  }

  public void addSubmissionValidationError(ClaimValidationError error) {
    submissionValidationErrors.add(error.toPatch());
  }

  public void addClaimError(String claimId, ClaimValidationError error) {
    addClaimMessages(claimId, List.of(error.toPatch()));
  }

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

  public void addClaimMessages(String claimId, List<ValidationMessagePatch> messages) {
    getClaimReport(claimId)
        .ifPresentOrElse(
            report -> report.addMessages(messages),
            () -> claimReports.add(new ClaimValidationReport(claimId, messages)));
  }

  public void addClaimReports(List<ClaimValidationReport> reports) {
    claimReports.addAll(reports);
  }

  public Optional<ClaimValidationReport> getClaimReport(String claimId) {
    return claimReports.stream().filter(r -> r.getClaimId().equals(claimId)).findFirst();
  }

  public void flagForRetry(String claimId) {
    getClaimReport(claimId).ifPresent(ClaimValidationReport::flagForRetry);
  }

  public boolean isFlaggedForRetry(String claimId) {
    return getClaimReport(claimId).map(ClaimValidationReport::isFlaggedForRetry).orElse(false);
  }

  public boolean hasErrors(String claimId) {
    return getClaimReport(claimId).map(ClaimValidationReport::hasErrors).orElse(false);
  }

  public boolean hasErrors() {
    return !submissionValidationErrors.isEmpty()
        || claimReports.stream().anyMatch(ClaimValidationReport::hasErrors);
  }
}
