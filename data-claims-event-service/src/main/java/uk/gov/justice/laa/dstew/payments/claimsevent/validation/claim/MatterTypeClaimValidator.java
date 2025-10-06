package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Checks the matter type code value is valid depending on the area of law.
 *
 * @author Jamie Briggs
 * @see ClaimResponse
 * @see SubmissionValidationContext
 * @see ClaimWithAreaOfLawValidator
 */
@Component
public class MatterTypeClaimValidator
    implements ClaimValidator, ClaimWithAreaOfLawValidator {

  private static final String MATTER_TYPE_CIVIL_PATTERN = "^[a-zA-Z0-9]{1,4}[-:][a-zA-Z0-9]{1,4}$";
  private static final String MATTER_TYPE_MEDIATION_PATTERN = "^[A-Z]{4}[-:][A-Z]{4}$";

  @Override
  public void validate(ClaimResponse claim, SubmissionValidationContext context, String areaOfLaw) {
    String regex =
        switch (areaOfLaw) {
          case "CIVIL" -> MATTER_TYPE_CIVIL_PATTERN;
          case "MEDIATION" -> MATTER_TYPE_MEDIATION_PATTERN;
          default -> null;
        };

    validateFieldWithRegex(
        claim, areaOfLaw, claim.getMatterTypeCode(), "matter_type_code", regex, context);
  }

  @Override
  public int priority() {
    return 100;
  }
}
