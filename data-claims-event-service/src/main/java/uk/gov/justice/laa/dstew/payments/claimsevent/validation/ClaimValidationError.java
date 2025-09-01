package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

/** Enum holding claim validation errors. */
public enum ClaimValidationError {
  INVALID_AREA_OF_LAW_FOR_PROVIDER(
      "A contract schedule with the provided area of law could not be found for this provider"),
  INVALID_CATEGORY_OF_LAW_AND_FEE_CODE(
      "A category of law could not be found for the provided fee code"),
  INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER(
      "The provider is not contracted for the category of law associated with the fee code"),
  INVALID_NIL_SUBMISSION_CONTAINS_CLAIMS(
      "Submission is marked as nil submission, but contains claims"),
  INVALID_FEE_CALCULATION_VALIDATION_FAILED(
      "A validation error occurred when attempting to calculate the fee for this claim");

  final String message;

  ClaimValidationError(String message) {
    this.message = message;
  }
}
