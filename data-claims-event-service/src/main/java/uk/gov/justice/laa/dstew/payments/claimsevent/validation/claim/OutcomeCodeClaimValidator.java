package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Checks the outcome code claim value is valid depending on the area of law.
 *
 * @author Nagarjuna Rachakonda
 * @see ClaimResponse
 * @see SubmissionValidationContext
 * @see BasicClaimValidator
 */
@Component
@RequiredArgsConstructor
public class OutcomeCodeClaimValidator implements ClaimValidator, ClaimWithAreaOfLawValidator {

  public static final String OUTCOME_CODE_LEGAL_HELP_PATTERN = "^[A-Za-z0-9-]{2}$";
  public static final String OUTCOME_CODE_CRIME_LOWER_PATTERN =
      "(?i)^(CP(0[1-9]|1[0-9]|2[0-8])|CN(0[1-9]|1[0-3])|PL(0[1-9]|1[0-4]))?$";
  public static final String OUTCOME_CODE_MEDIATION_PATTERN = "(?i)^(A|B|S|C|P)?$";

  @Override
  public void validate(
      ClaimResponse claim, SubmissionValidationContext context, AreaOfLaw areaOfLaw) {
    String regex =
        switch (areaOfLaw) {
          case LEGAL_HELP -> OUTCOME_CODE_LEGAL_HELP_PATTERN;
          case CRIME_LOWER -> OUTCOME_CODE_CRIME_LOWER_PATTERN;
          case MEDIATION -> OUTCOME_CODE_MEDIATION_PATTERN;
        };

    validateFieldWithRegex(
        claim, areaOfLaw, claim.getOutcomeCode(), "outcome_code", regex, context);
  }

  @Override
  public int priority() {
    return 100;
  }
}
