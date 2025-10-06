package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.AbstractDateValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Validates that a claim's case dates are valid and not too far in the past.
 *
 * @author Jamie Briggs
 * @see ClaimResponse
 * @see SubmissionValidationContext
 * @see BasicClaimValidator
 */
@Component
public class CaseDatesClaimValidator extends AbstractDateValidator
    implements ClaimValidator, BasicClaimValidator {

  public static final String OLDEST_DATE_ALLOWED_1 = "1995-01-01";
  public static final String MIN_REP_ORDER_DATE = "2016-04-01";

  @Override
  public void validate(ClaimResponse claim, SubmissionValidationContext context) {
    String caseStartDate = claim.getCaseStartDate();
    checkDateInPast(claim, "Case Start Date", caseStartDate, OLDEST_DATE_ALLOWED_1, context);
    checkDateInPast(
        claim, "Case Concluded Date", claim.getCaseConcludedDate(), OLDEST_DATE_ALLOWED_1, context);
    checkDateInPast(
        claim, "Transfer Date", claim.getTransferDate(), OLDEST_DATE_ALLOWED_1, context);
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
