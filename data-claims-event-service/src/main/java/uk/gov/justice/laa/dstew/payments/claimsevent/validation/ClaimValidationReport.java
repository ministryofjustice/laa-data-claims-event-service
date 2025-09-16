package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Class responsible for holding information about a claim under validation, including the claim ID
 * and validation errors.
 */
@Slf4j
@Getter
@EqualsAndHashCode
public class ClaimValidationReport {

  private final String claimId;
  private final List<String> errors;

  private boolean flaggedForRetry;

  /**
   * Construct a new {@code ClaimValidationReport} with an empty list of errors.
   *
   * @param claimId the ID of the claim
   */
  public ClaimValidationReport(String claimId) {
    this.claimId = claimId;
    this.errors = new ArrayList<>();
    this.flaggedForRetry = false;
  }

  /**
   * Construct a new {@code ClaimValidationReport} with an initial list of errors.
   *
   * @param claimId the ID of the claim
   */
  public ClaimValidationReport(String claimId, List<ClaimValidationError> errors) {
    this.claimId = claimId;
    this.errors = new ArrayList<>();
    errors.forEach(e -> this.errors.add(e.getDescription()));
  }

  /**
   * Constructs a new instance of {@code ClaimValidationReport} with the specified claim ID and a
   * collection of validation error messages.
   *
   * @param claimId the unique identifier for the claim
   * @param errors a collection of validation error messages associated with the claim
   */
  public ClaimValidationReport(String claimId, Collection<String> errors) {
    this.claimId = claimId;
    this.errors = new ArrayList<>(errors);
    this.flaggedForRetry = false;
  }

  /**
   * Add an error to the claim validation report.
   *
   * @param error the error to add
   */
  public void addError(ClaimValidationError error) {
    errors.add(error.getDescription());
  }

  public void addError(String error) {
    errors.add(error);
  }

  public void addErrors(List<ClaimValidationError> errorList) {
    errorList.forEach(e -> errors.add(e.getDescription()));
  }

  public void addErrorsStrings(List<String> errorList) {
    errors.addAll(errorList);
  }

  /**
   * Verify whether the claim validation report contains any errors.
   *
   * @return true if the report contains at least one error, false otherwise.
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /** Set the retry flag for this claim validation report to true. */
  public void flagForRetry() {
    log.debug("Flagging claim {} for retry", this.claimId);
    this.flaggedForRetry = true;
  }
}
