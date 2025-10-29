package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionAreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Checks the stage reached claim value is valid depending on the area of law.
 *
 * @author Jamie Briggs
 * @see ClaimResponse
 * @see SubmissionValidationContext
 * @see BasicClaimValidator
 */
@Component
@RequiredArgsConstructor
public class StageReachedClaimValidator implements ClaimValidator, ClaimWithAreaOfLawValidator {

  public static final String STAGE_REACHED_LEGAL_HELP_PATTERN = "^[a-zA-Z0-9]{2}$";
  public static final String STAGE_REACHED_CRIME_LOWER_PATTERN =
      "^(INV[A-M]|PRI[A-E]|PRO[C-FH-LP-TUVW]|APP[ABC]|AS(MS|PL|AS)|YOU[EFKLXY]|VOID)$";

  @Override
  public void validate(
      ClaimResponse claim, SubmissionValidationContext context, BulkSubmissionAreaOfLaw areaOfLaw) {
    String regex =
        switch (areaOfLaw) {
          case LEGAL_HELP -> STAGE_REACHED_LEGAL_HELP_PATTERN;
          case CRIME_LOWER -> STAGE_REACHED_CRIME_LOWER_PATTERN;
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
