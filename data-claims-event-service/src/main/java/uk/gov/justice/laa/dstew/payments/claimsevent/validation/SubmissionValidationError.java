package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessageType;

/** Enum holding submission validation errors. */
@RequiredArgsConstructor
@Getter
public enum SubmissionValidationError {
  INCORRECT_SUBMISSION_STATUS_FOR_VALIDATION(
      "Submission cannot be validated in state %s",
      null, EVENT_SERVICE, ValidationMessageType.ERROR),
  SUBMISSION_STATUS_IS_NULL(
      "The submission state is null", null, EVENT_SERVICE, ValidationMessageType.ERROR),
  INVALID_NIL_SUBMISSION_CONTAINS_CLAIMS(
      "Submission is marked as nil submission, but contains claims",
      null,
      EVENT_SERVICE,
      ValidationMessageType.ERROR),
  NON_NIL_SUBMISSION_CONTAINS_NO_CLAIMS(
      "Submission is marked as nil submission, but contains claims",
      null,
      EVENT_SERVICE,
      ValidationMessageType.ERROR),
  SUBMISSION_PERIOD_MISSING(
      "Submission period is required. Please provide a submission period in the format MMM-YYYY",
      null,
      EVENT_SERVICE,
      ValidationMessageType.ERROR),
  SUBMISSION_PERIOD_INVALID_FORMAT(
      "Submission period wrong format, should be in the format MMM-YYYY",
      null,
      EVENT_SERVICE,
      ValidationMessageType.ERROR),
  SUBMISSION_PERIOD_SAME_MONTH(
      "Submissions for the current month (%s) are not accepted. Please submit for a previous "
          + "month.",
      null, EVENT_SERVICE, ValidationMessageType.ERROR),
  SUBMISSION_PERIOD_FUTURE_MONTH(
      "Submissions for after the current month (%s) are not accepted. Please submit for a previous "
          + "month.",
      null, EVENT_SERVICE, ValidationMessageType.ERROR),
  SUBMISSION_VALIDATION_MINIMUM_PERIOD(
      "Submissions for periods before %s are not accepted. Please submit for a period on or after %s.",
      "null", EVENT_SERVICE, ValidationMessageType.ERROR),
  SUBMISSION_ALREADY_EXISTS(
      "Submission already exists for Office (%s), Area of Law (%s),  Period (%s)",
      null, EVENT_SERVICE, ValidationMessageType.ERROR),
  ;

  final String displayMessage;
  final String technicalMessage;
  final String source;
  final ValidationMessageType type;

  /** Convert this enum into a ValidationMessagePatch. */
  public ValidationMessagePatch toPatch(Object... params) {
    return new ValidationMessagePatch()
        .displayMessage(displayMessage.formatted(params))
        .technicalMessage(technicalMessage)
        .source(source)
        .type(type);
  }
}
