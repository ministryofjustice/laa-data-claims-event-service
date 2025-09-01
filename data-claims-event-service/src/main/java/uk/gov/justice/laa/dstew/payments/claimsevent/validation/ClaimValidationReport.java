package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import java.util.ArrayList;
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
  private final List<ClaimValidationError> errors;

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
    this.errors = new ArrayList<>(errors);
    this.flaggedForRetry = false;
  }

  /**
   * Add an error to the claim validation report.
   *
   * @param error the error to add
   */
  public void addError(ClaimValidationError error) {
    log.debug("Adding error to claim {}: {}", this.claimId, error);
    this.errors.add(error);
  }

  /**
   * Add multiple errors to the claim validation report.
   *
   * @param errors the errors to add
   */
  public void addErrors(List<ClaimValidationError> errors) {
    log.debug("Adding errors to claim {}: {}", this.claimId, errors.toString());
    this.errors.addAll(errors);
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
