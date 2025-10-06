package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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

  private static final String STAGE_REACHED_CIVIL_PATTERN = "^[a-zA-Z0-9]{2}$";
  private static final String STAGE_REACHED_CRIME_PATTERN = "^[A-Z]{4}$";

  @Override
  public void validate(ClaimResponse claim, SubmissionValidationContext context, String areaOfLaw) {
    String regex =
        switch (areaOfLaw) {
          case "CIVIL" -> STAGE_REACHED_CIVIL_PATTERN;
          case "CRIME" -> STAGE_REACHED_CRIME_PATTERN;
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
