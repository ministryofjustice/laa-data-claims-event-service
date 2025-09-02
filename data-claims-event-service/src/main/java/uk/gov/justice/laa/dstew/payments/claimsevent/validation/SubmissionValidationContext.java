package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Class responsible for holding the validation context for a submission within the scope of a
 * request. This will contain all claim errors reported during the request.
 */
@Getter
@RequestScope
@Component
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionValidationContext {

  private List<ClaimValidationReport> claimReports  = new ArrayList<>();
  private List<String> submissionValidationErrors = new ArrayList<>();

  /**
   * Adds a list of submission level validation error messages to the existing collection of errors.
   *
   * @param submissionValidationErrors the list of validation error messages to be added
   */
  public void addSubmissionValidationErrors(List<String> submissionValidationErrors) {
    this.submissionValidationErrors.addAll(submissionValidationErrors);
  }

  /**
   * Adds a single submission-level validation error message to the existing collection of errors.
   *
   * @param submissionValidationError the validation error message to be added
   */
  public void addSubmissionValidationError(String submissionValidationError) {
    this.submissionValidationErrors.add(submissionValidationError);
  }

  /**
   * Add an error to the report (Extracts the error message and passes it on to the overloaded method)
   *
   * @param claimId the ID of the claim for which to report an error
   * @param error the claim validation error
   */
  public void addClaimError(String claimId, ClaimValidationError error) {
    addClaimError(claimId, error.getMessage());
  }

  /**
   * Add a claim error to the validation context. If a report has not yet been created for the
   * claim, creates a new claim report and adds the error. Otherwise, the error is added to the
   * existing claim report.
   *
   * @param claimId the ID of the claim for which to report an error
   * @param error the claim validation error as a plain String
   */
  public void addClaimError(String claimId, String error) {
    claimReports.stream()
        .filter(claimReport -> claimReport.getClaimId().equals(claimId))
        .findFirst()
        .ifPresentOrElse(
            claimReport -> claimReport.addError(error),
            () -> claimReports.add(new ClaimValidationReport(claimId, List.of(error))));
  }

  /**
   * Add a list of claim errors to the validation context. If a report has not yet been created for the
   * claim, creates a new claim report and adds the errors. Otherwise, the errors are added to the
   * existing claim report.
   *
   * @param claimId the ID of the claim for which to report an error
   * @param errors the list of claim validation errors
   */
  public void addClaimErrors(String claimId, List<String> errors) {
    claimReports.stream()
        .filter(claimReport -> claimReport.getClaimId().equals(claimId))
        .findFirst()
        .ifPresentOrElse(
            claimReport -> claimReport.addErrorsStrings (errors),
            () -> claimReports.add(new ClaimValidationReport(claimId, errors)));
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
    return !submissionValidationErrors.isEmpty() || claimReports.stream().anyMatch(ClaimValidationReport::hasErrors);
  }
}
