package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static uk.gov.justice.laa.dstew.payments.claimsevent.util.DateUtil.DATE_FORMATTER_FOR_DISPLAY_MESSAGE;
import static uk.gov.justice.laa.dstew.payments.claimsevent.util.DateUtil.DATE_FORMATTER_YYYY_MM_DD;
import static uk.gov.justice.laa.dstew.payments.claimsevent.util.DateUtil.SUBMISSION_PERIOD_FORMATTER;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
    if (StringUtils.hasText(currentClaim.getSubmissionPeriod())
        && StringUtils.hasText(currentClaim.getCaseStartDate())) {
      YearMonth submissionPeriod =
          YearMonth.parse(currentClaim.getSubmissionPeriod(), SUBMISSION_PERIOD_FORMATTER);

      LocalDate submissionEndDate = submissionPeriod.plusMonths(1).atDay(20);
      LocalDate caseStartDate =
          LocalDate.parse(currentClaim.getCaseStartDate(), DATE_FORMATTER_YYYY_MM_DD);

      if (caseStartDate.plusMonths(3).isAfter(submissionEndDate)) {
        log.debug(
            "Disbursement claims can only be submitted at least {} calendar months after the Case Start Date {}",
            MAXIMUM_MONTHS_DIFFERENCE,
            caseStartDate.format(DATE_FORMATTER_FOR_DISPLAY_MESSAGE));

        context.addClaimError(
            currentClaim.getId(),
            String.format(
                "Disbursement claims can only be submitted at least %d calendar months after the Case Start Date %s",
                MAXIMUM_MONTHS_DIFFERENCE,
                caseStartDate.format(DATE_FORMATTER_FOR_DISPLAY_MESSAGE)),
            EVENT_SERVICE);
      }
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
