package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.model.ValidationErrorMessage;

/**
 * Checks the matter type code value is valid depending on the area of law.
 *
 * @author Jamie Briggs
 * @see ClaimResponse
 * @see SubmissionValidationContext
 * @see ClaimWithAreaOfLawValidator
 */
@Component
public final class MatterTypeClaimValidator extends RegexClaimValidator
    implements ClaimWithAreaOfLawValidator {

  private static final String MATTER_TYPE_CIVIL_PATTERN = "^[a-zA-Z0-9]{1,4}[-:][a-zA-Z0-9]{1,4}$";
  private static final String MATTER_TYPE_MEDIATION_PATTERN = "^[A-Z]{4}[-:][A-Z]{4}$";

  public MatterTypeClaimValidator(
      Map<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages) {
    super(schemaValidationErrorMessages);
  }

  @Override
  public void validate(ClaimResponse claim, SubmissionValidationContext context, String areaOfLaw) {
    String regex =
        switch (areaOfLaw) {
          case "CIVIL", "LEGAL HELP" -> MATTER_TYPE_CIVIL_PATTERN;
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
