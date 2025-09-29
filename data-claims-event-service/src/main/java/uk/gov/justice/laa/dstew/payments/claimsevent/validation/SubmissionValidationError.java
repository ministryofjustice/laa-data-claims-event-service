package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessageType;

/** Enum holding claim validation errors. */
@RequiredArgsConstructor
@Getter
public enum SubmissionValidationError {
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
