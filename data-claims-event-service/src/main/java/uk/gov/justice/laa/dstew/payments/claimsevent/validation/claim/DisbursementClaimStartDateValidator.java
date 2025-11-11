package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationType;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Validator that checks if disbursement claims are submitted within the allowed timeframe relative
 * to the case start date. Validation is performed by comparing the case start date against the last
 * calendar day of the submission period month, with an inclusive boundary (exactly 3 months is
 * valid). Disbursement claims can only be submitted after a specific number of calendar months have
 * passed since the case start date.
 */
@Slf4j
@Component
public final class DisbursementClaimStartDateValidator implements ClaimValidator {

  private static final int MAXIMUM_MONTHS_DIFFERENCE = 3;
  public static final DateTimeFormatter CASE_START_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");
  public static final DateTimeFormatter FORMATTER_FOR_DISPLAY_MESSAGE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final DateTimeFormatter submissionPeriodFormatter =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .appendPattern("MMM-yyyy")
          .toFormatter(Locale.ENGLISH);

  /**
   * Validates that disbursement claims are submitted at least 3 calendar months after the case
   * start date. The validation compares the case start date against the last calendar day of the
   * submission period month, with an inclusive boundary (exactly 3 months is considered valid).
   *
   * @param currentClaim The claim to validate
   * @param context The validation context to store any validation errors
   * @param feeType The type of fee being claimed
   */
  public void validate(
      final ClaimResponse currentClaim,
      final SubmissionValidationContext context,
      final String feeType) {

    // Don't check if the current claim is not a disbursement, this validation only applies to
    // disbursement claims.
    if (!isDisbursementClaim(feeType)) {
      log.debug("Claim {} is not a disbursement claim", currentClaim.getId());
      return;
    }
    YearMonth submissionPeriod =
        YearMonth.parse(currentClaim.getSubmissionPeriod(), submissionPeriodFormatter);

    LocalDate submissionEndDate = submissionPeriod.atEndOfMonth();
    LocalDate caseStartDate =
        LocalDate.parse(currentClaim.getCaseStartDate(), CASE_START_DATE_FORMATTER);

    if (caseStartDate.plusMonths(3).isAfter(submissionEndDate)) {
      log.debug(
          "Disbursement claims can only be submitted at least {} calendar months after the Case Start Date {}",
          MAXIMUM_MONTHS_DIFFERENCE,
          caseStartDate.format(FORMATTER_FOR_DISPLAY_MESSAGE));

      context.addClaimError(
          currentClaim.getId(),
          String.format(
              "Disbursement claims can only be submitted at least %d calendar months after the Case Start Date %s",
              MAXIMUM_MONTHS_DIFFERENCE, caseStartDate.format(FORMATTER_FOR_DISPLAY_MESSAGE)),
          EVENT_SERVICE);
    }
  }

  /**
   * Checks if the given fee type represents a disbursement claim.
   *
   * @param feeType The fee type to check
   * @return true if the fee type is for disbursement claims, false otherwise
   */
  private Boolean isDisbursementClaim(String feeType) {
    return Objects.equals(feeType, FeeCalculationType.DISB_ONLY.getValue());
  }

  /**
   * Gets the priority of this validator in the validation chain.
   *
   * @return The priority value of 10 for this validator
   */
  @Override
  public int priority() {
    return 10;
  }
}
