package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.DCES;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.FSP;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessageType;

/** Enum holding claim validation errors. */
@RequiredArgsConstructor
@Getter
public enum ClaimValidationError {
  SUBMISSION_STATE_IS_NULL("The submission state is null", null, DCES, ValidationMessageType.ERROR),
  INVALID_AREA_OF_LAW_FOR_PROVIDER(
      "A contract schedule with the provided area of law could not be found for this provider",
      null,
      DCES,
      ValidationMessageType.ERROR),
  INVALID_CATEGORY_OF_LAW_AND_FEE_CODE(
      "A category of law could not be found for the provided fee code",
      null,
      DCES,
      ValidationMessageType.ERROR),
  INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER(
      "The provider is not contracted for the category of law associated with the fee code",
      null,
      DCES,
      ValidationMessageType.ERROR),
  INVALID_NIL_SUBMISSION_CONTAINS_CLAIMS(
      "Submission is marked as nil submission, but contains claims",
      null,
  DCES,
  ValidationMessageType.ERROR),
  NON_NIL_SUBMISSION_CONTAINS_NO_CLAIMS(
      "Submission is marked as nil submission, but contains claims",null,
      DCES,
      ValidationMessageType.ERROR),
  INVALID_DATE_IN_UNIQUE_FILE_NUMBER(
      "Unique file ID must be in the format DDMMYY/NNN with a date in the past",null,
      DCES,
      ValidationMessageType.ERROR),
  INVALID_FEE_CALCULATION_VALIDATION_FAILED(
      "A validation error occurred when attempting to calculate the fee for this claim",
      null,
      FSP,
      ValidationMessageType.ERROR);

  final String displayMessage;
  final String technicalMessage;
  final String source;
  final ValidationMessageType type;

  /** Convert this enum into a ValidationMessagePatch. */
  public ValidationMessagePatch toPatch() {
    return new ValidationMessagePatch()
        .displayMessage(displayMessage)
        .technicalMessage(technicalMessage)
        .source(source)
        .type(type);
  }
}
