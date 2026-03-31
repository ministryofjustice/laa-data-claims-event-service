package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static uk.gov.justice.laa.dstew.payments.claimsevent.util.DateUtil.DATE_FORMATTER_YYYY_MM_DD;

import java.time.LocalDate;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.AbstractDateValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Validates that a claim's case dates are valid and not too far in the past.
 *
 * @author Jamie Briggs
 * @see ClaimResponse
 * @see SubmissionValidationContext
 * @see ClaimWithAreaOfLawValidator
 */
@Component
public final class CaseDatesClaimValidator extends AbstractDateValidator
    implements ClaimWithAreaOfLawValidator {

  private static final String OLDEST_DATE_ALLOWED_STRING = "1995-01-01";
  private static final String EARLIEST_CASE_CONCLUDED_DATE_ALLOWED_STRING = "2013-04-01";
  private static final String MIN_REP_ORDER_DATE_STRING = "2016-04-01";
  private static final LocalDate OLDEST_DATE_ALLOWED =
      LocalDate.parse(OLDEST_DATE_ALLOWED_STRING, DATE_FORMATTER_YYYY_MM_DD);
  private static final LocalDate EARLIEST_CASE_CONCLUDED_DATE_ALLOWED =
      LocalDate.parse(EARLIEST_CASE_CONCLUDED_DATE_ALLOWED_STRING, DATE_FORMATTER_YYYY_MM_DD);
  private static final LocalDate MIN_REP_ORDER_DATE =
      LocalDate.parse(MIN_REP_ORDER_DATE_STRING, DATE_FORMATTER_YYYY_MM_DD);
  private static final String CASE_CONCLUDED_DATE_FIELD_NAME = "Case Concluded Date";

  @Override
  public void validate(
      ClaimResponse claim, SubmissionValidationContext context, AreaOfLaw areaOfLaw) {

    String caseStartDate = claim.getCaseStartDate();
    checkDateInPast(claim, "Case Start Date", caseStartDate, OLDEST_DATE_ALLOWED, context);
    LocalDate earliestDateAllowedForCaseConcludedDate =
        AreaOfLaw.CRIME_LOWER.equals(areaOfLaw)
            ? MIN_REP_ORDER_DATE
            : EARLIEST_CASE_CONCLUDED_DATE_ALLOWED;

    checkDateNotInFutureAndWithinAllowedPeriod(
        claim,
        CASE_CONCLUDED_DATE_FIELD_NAME,
        claim.getCaseConcludedDate(),
        earliestDateAllowedForCaseConcludedDate,
        context);
    checkDateInPast(claim, "Transfer Date", claim.getTransferDate(), OLDEST_DATE_ALLOWED, context);
    checkDateInPast(
        claim,
        "Representation Order Date",
        claim.getRepresentationOrderDate(),
        MIN_REP_ORDER_DATE,
        context);
  }

  @Override
  public int priority() {
    return 100;
  }
}
