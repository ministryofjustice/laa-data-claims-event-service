package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.model.ValidationErrorMessage;

/**
 * Checks the outcome code claim value is valid depending on the area of law.
 *
 * @author Nagarjuna Rachakonda
 * @see ClaimResponse
 * @see SubmissionValidationContext
 * @see BasicClaimValidator
 */
@Component
public final class OutcomeCodeClaimValidator extends RegexClaimValidator
    implements ClaimWithAreaOfLawValidator {

  public static final String OUTCOME_CODE_CIVIL_PATTERN = "^[A-Za-z0-9-]{2}$";
  public static final String OUTCOME_CODE_CRIME_PATTERN =
      "(?i)^(CP(0[1-9]|1[0-9]|2[0-8])|CN(0[1-9]|1[0-3])|PL(0[1-9]|1[0-4]))?$";
  public static final String OUTCOME_CODE_MEDIATION_PATTERN = "(?i)^(A|B|S|C|P)?$";

  public OutcomeCodeClaimValidator(
      Map<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages) {
    super(schemaValidationErrorMessages);
  }

  @Override
  public void validate(ClaimResponse claim, SubmissionValidationContext context, String areaOfLaw) {
    String regex =
        switch (areaOfLaw) {
          case "CIVIL", "LEGAL HELP" -> OUTCOME_CODE_CIVIL_PATTERN;
          case "CRIME", "CRIME LOWER" -> OUTCOME_CODE_CRIME_PATTERN;
          case "MEDIATION" -> OUTCOME_CODE_MEDIATION_PATTERN;
          default -> null;
        };

    validateFieldWithRegex(
        claim, areaOfLaw, claim.getOutcomeCode(), "outcome_code", regex, context);
  }

  @Override
  public int priority() {
    return 100;
  }
}
