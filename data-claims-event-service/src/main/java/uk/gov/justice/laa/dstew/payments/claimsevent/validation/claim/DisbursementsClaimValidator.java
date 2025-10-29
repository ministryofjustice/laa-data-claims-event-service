package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionAreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Validates that a claim's disbursements VAT amount is valid depending on the area of law.
 *
 * @author Jamie Briggs
 * @see ClaimResponse
 * @see SubmissionValidationContext
 * @see ClaimWithAreaOfLawValidator
 */
@Component
public class DisbursementsClaimValidator implements ClaimValidator, ClaimWithAreaOfLawValidator {

  @Override
  public void validate(
      ClaimResponse claim, SubmissionValidationContext context, BulkSubmissionAreaOfLaw areaOfLaw) {
    var disbursementsVatAmount = claim.getDisbursementsVatAmount();

    BigDecimal maxAllowed =
        switch (areaOfLaw) {
          case LEGAL_HELP -> BigDecimal.valueOf(99999.99);
          case CRIME_LOWER -> BigDecimal.valueOf(999999.99);
          case MEDIATION -> BigDecimal.valueOf(999999999.99);
        };

    if (disbursementsVatAmount != null && disbursementsVatAmount.compareTo(maxAllowed) > 0) {
      context.addClaimError(
          claim.getId(),
          String.format(
              "disbursementsVatAmount (%s): must have a maximum value of %s (provided value: %s)",
              areaOfLaw, maxAllowed, disbursementsVatAmount),
          EVENT_SERVICE);
    }
  }

  @Override
  public int priority() {
    return 100;
  }
}
