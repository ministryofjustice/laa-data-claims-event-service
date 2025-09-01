package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Class responsible for holding the validation context for a submission within the scope of a
 * request. This will contain all claim errors reported during the request.
 */
@Getter
@RequestScope
@Component
public class SubmissionValidationContext {

  private final List<ClaimValidationReport> claimReports;

  /**
   * Construct a new {@code SubmissionValidationContext} with an initialised empty list of claim
   * reports.
   */
  public SubmissionValidationContext() {
    this.claimReports = new ArrayList<>();
  }

  /**
   * Add a claim error to the validation context. If a report has not yet been created for the
   * claim, creates a new claim report and adds the error. Otherwise, the error is added to the
   * existing claim report.
   *
   * @param claimId the ID of the claim for which to report an error
   * @param error the claim validation error
   */
  public void addClaimError(String claimId, ClaimValidationError error) {
    claimReports.stream()
        .filter(claimReport -> claimReport.getClaimId().equals(claimId))
        .findFirst()
        .ifPresentOrElse(
            claimReport -> claimReport.addError(error),
            () -> claimReports.add(new ClaimValidationReport(claimId, List.of(error))));
  }

  /**
   * Add a list of claim reports to the validation context.
   *
   * @param reports the list of {@link ClaimValidationReport}
   */
  public void addClaimReports(List<ClaimValidationReport> reports) {
    claimReports.addAll(reports);
  }

  /**
   * Add a claim validation error to all current claim reports.
   *
   * @param error the {@link ClaimValidationError} to add
   */
  public void addToAllClaimReports(ClaimValidationError error) {
    claimReports.forEach(claimReport -> claimReport.addError(error));
  }

  /**
   * Get the claim report for an individual claim.
   *
   * @param claimId the ID of the claim for which to retrieve a report
   * @return an optional {@link ClaimValidationReport}
   */
  public Optional<ClaimValidationReport> getClaimReport(String claimId) {
    return claimReports.stream()
        .filter(claimError -> claimError.getClaimId().equals(claimId))
        .findFirst();
  }

  /**
   * Flag a claim for retry in the case where it cannot be fully validated, e.g. error from third
   * party service.
   *
   * @param claimId the claim id to flag for retry
   */
  public void flagForRetry(String claimId) {
    claimReports.stream()
        .filter(claimReport -> claimReport.getClaimId().equals(claimId))
        .findFirst()
        .ifPresent(ClaimValidationReport::flagForRetry);
  }

  /**
   * Verify whether a claim is flagged for retry.
   *
   * @param claimId the ID of the claim
   * @return true if the claim is flagged for retry, false otherwise
   */
  public boolean isFlaggedForRetry(String claimId) {
    return getClaimReport(claimId).map(ClaimValidationReport::isFlaggedForRetry).orElse(false);
  }

  /**
   * Determine whether the validation context contains any claim errors for the given claim ID.
   *
   * @param claimId the ID of the claim
   * @return true of the claim report for the given claim has validation errors, false otherwise
   */
  public boolean hasErrors(String claimId) {
    if (claimId == null) {
      return false;
    }
    return claimReports.stream()
        .filter(claimReport -> claimReport.getClaimId().equals(claimId))
        .findFirst()
        .map(ClaimValidationReport::hasErrors)
        .orElse(false);
  }

  /**
   * Determine whether the validation context has any claim errors.
   *
   * @return true if any claim has a validation error, false otherwise.
   */
  public boolean hasErrors() {
    return claimReports.stream().anyMatch(ClaimValidationReport::hasErrors);
  }
}
