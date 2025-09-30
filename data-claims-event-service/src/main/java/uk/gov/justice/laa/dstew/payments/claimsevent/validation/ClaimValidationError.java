package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.FEE_SERVICE;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessageType;

/** Enum holding claim validation errors. */
@RequiredArgsConstructor
@Getter
public enum ClaimValidationError {
  INVALID_AREA_OF_LAW_FOR_PROVIDER(
      "A contract schedule with the provided area of law could not be found for this provider",
      null,
      EVENT_SERVICE,
      ValidationMessageType.ERROR),
  INVALID_CATEGORY_OF_LAW_AND_FEE_CODE(
      "A category of law could not be found for the provided fee code",
      null,
      EVENT_SERVICE,
      ValidationMessageType.ERROR),
  INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER(
      "The provider is not contracted for the category of law associated with the fee code",
      null,
      EVENT_SERVICE,
      ValidationMessageType.ERROR),
  INVALID_DATE_IN_UNIQUE_FILE_NUMBER(
      "Unique file ID must be in the format DDMMYY/NNN with a date in the past",
      null,
      EVENT_SERVICE,
      ValidationMessageType.ERROR),
  INVALID_FEE_CALCULATION_VALIDATION_FAILED(
      "A validation error occurred when attempting to calculate the fee for this claim",
      null,
      FEE_SERVICE,
      ValidationMessageType.ERROR),
  INVALID_CLAIM_HAS_DUPLICATE_IN_EXISTING_SUBMISSION(
      "A duplicate claim was found within the same submission",
      null,
      EVENT_SERVICE,
      ValidationMessageType.ERROR),
  INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION(
      "A duplicate claim was found in another submission",
      null,
      EVENT_SERVICE,
      ValidationMessageType.ERROR),
  INVALID_SUBMISSION_PERIOD_SAME_AS_CURRENT_MONTH(
      "A duplicate claim was found in another submission",
      null,
      EVENT_SERVICE,
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
