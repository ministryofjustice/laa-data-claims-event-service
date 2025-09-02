package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Class responsible for holding information about a claim under validation, including the claim ID
 * and validation errors.
 */
@Getter
@EqualsAndHashCode
public class ClaimValidationReport {

  private final String claimId;
  private final List<String> errors;

  public ClaimValidationReport(String claimId) {
    this.claimId = claimId;
    this.errors = new ArrayList<>();
  }

  public ClaimValidationReport(String claimId, List<ClaimValidationError> errors) {
    this.claimId = claimId;
    this.errors = new ArrayList<>();
    errors.forEach(e -> this.errors.add(e.getMessage()));
  }

  public ClaimValidationReport(String claimId, Collection<String> errors) {
    this.claimId = claimId;
    this.errors = new ArrayList<>(errors);
  }

  public void addError(ClaimValidationError error) {
    errors.add(error.getMessage());
  }

  public void addErrors(List<ClaimValidationError> errorList) {
    errorList.forEach(e -> errors.add(e.getMessage()));
  }

  public void addError(String error) {
    errors.add(error);
  }

  public void addErrorsStrings(List<String> errorList) {
    errors.addAll(errorList);
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }
}
