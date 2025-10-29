package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.model.ValidationErrorMessage;

/**
 * Checks the stage reached claim value is valid depending on the area of law.
 *
 * @author Jamie Briggs
 * @see ClaimResponse
 * @see SubmissionValidationContext
 * @see BasicClaimValidator
 */
@Component
public final class StageReachedClaimValidator extends RegexClaimValidator
    implements ClaimWithAreaOfLawValidator {

  public static final String STAGE_REACHED_LEGAL_HELP_PATTERN = "^[a-zA-Z0-9]{2}$";
  public static final String STAGE_REACHED_CRIME_LOWER_PATTERN =
      "^(INV[A-M]|PRI[A-E]|PRO[C-FH-LP-TUVW]|APP[ABC]|AS(MS|PL|AS)|YOU[EFKLXY]|VOID)$";

  public StageReachedClaimValidator(
      Map<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages) {
    super(schemaValidationErrorMessages);
  }

  @Override
  public void validate(ClaimResponse claim, SubmissionValidationContext context, String areaOfLaw) {
    String regex =
        switch (AreaOfLaw.fromValue(areaOfLaw)) {
          case AreaOfLaw.LEGAL_HELP -> STAGE_REACHED_LEGAL_HELP_PATTERN;
          case AreaOfLaw.CRIME_LOWER -> STAGE_REACHED_CRIME_LOWER_PATTERN;
          default -> null;
        };

    validateFieldWithRegex(
        claim, areaOfLaw, claim.getStageReachedCode(), "stage_reached_code", regex, context);
  }

  @Override
  public int priority() {
    return 100;
  }
}
