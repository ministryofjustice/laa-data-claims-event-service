package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static uk.gov.justice.laa.dstew.payments.claimsevent.util.DateUtil.DATE_FORMATTER_YYYY_MM_DD;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

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

  private static final String EARLIEST_DATE_ALLOWED_STRING = "2013-04-01";
  private static final String MIN_REP_ORDER_DATE_STRING = "2016-04-01";
  public static final LocalDate EARLIEST_DATE_ALLOWED =
      LocalDate.parse(EARLIEST_DATE_ALLOWED_STRING, DATE_FORMATTER_YYYY_MM_DD);
  public static final LocalDate MIN_REP_ORDER_DATE =
      LocalDate.parse(MIN_REP_ORDER_DATE_STRING, DATE_FORMATTER_YYYY_MM_DD);
  private static final String CASE_CONCLUDED_DATE_FIELD_NAME = "Case Concluded Date";

  @Override
  public void validate(
      ClaimResponse claim, SubmissionValidationContext context, AreaOfLaw areaOfLaw) {

    String caseStartDate = claim.getCaseStartDate();
    checkDateInPast(claim, "Case Start Date", caseStartDate, EARLIEST_DATE_ALLOWED, context);
    LocalDate earliestDateAllowedForCaseConcludedDate =
        AreaOfLaw.CRIME_LOWER.equals(areaOfLaw) ? MIN_REP_ORDER_DATE : EARLIEST_DATE_ALLOWED;

    if (claim.getCaseConcludedDate() != null) {
      LocalDate caseConcludedDateAllowed =
          LocalDate.parse(claim.getCaseConcludedDate(), DATE_FORMATTER_YYYY_MM_DD);
      if (caseConcludedDateAllowed.isAfter(LocalDate.now())) {
        context.addClaimError(
            claim.getId(),
            String.format("%s cannot be a future date", CASE_CONCLUDED_DATE_FIELD_NAME),
            EVENT_SERVICE);
      } else {
        checkDateInPastAndDoesNotExceedSubmissionPeriod(
            claim,
            CASE_CONCLUDED_DATE_FIELD_NAME,
            claim.getCaseConcludedDate(),
            earliestDateAllowedForCaseConcludedDate,
            context);
      }
    }
    checkDateInPast(
        claim, "Transfer Date", claim.getTransferDate(), EARLIEST_DATE_ALLOWED, context);
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
