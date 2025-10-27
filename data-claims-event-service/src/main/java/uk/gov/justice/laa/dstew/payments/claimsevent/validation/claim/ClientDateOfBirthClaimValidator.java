package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.AbstractDateValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Validates that a claim's client's date of birth is valid and not too far in the past.
 *
 * @author Jamie Briggs
 * @see ClaimResponse
 * @see SubmissionValidationContext
 * @see AbstractDateValidator
 * @see BasicClaimValidator
 */
@Component
public class ClientDateOfBirthClaimValidator extends AbstractDateValidator
    implements BasicClaimValidator {

  public static final String MIN_BIRTH_DATE = "1900-01-01";

  @Override
  public void validate(ClaimResponse claim, SubmissionValidationContext context) {
    checkDateInPast(
        claim, "Client Date of Birth", claim.getClientDateOfBirth(), MIN_BIRTH_DATE, context);
    checkDateInPast(
        claim, "Client2 Date of Birth", claim.getClient2DateOfBirth(), MIN_BIRTH_DATE, context);
  }

  @Override
  public int priority() {
    return 100;
  }
}
