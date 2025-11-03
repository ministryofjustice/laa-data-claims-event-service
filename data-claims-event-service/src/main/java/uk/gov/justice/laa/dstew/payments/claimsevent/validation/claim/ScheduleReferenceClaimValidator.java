package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Checks the schedule reference value is valid.
 *
 * @author Jamie Briggs
 * @see ClaimResponse
 * @see SubmissionValidationContext
 * @see BasicClaimValidator
 */
@Component
@RequiredArgsConstructor
public class ScheduleReferenceClaimValidator
    implements ClaimWithAreaOfLawValidator, ClaimValidator {

  private static final String LEGAL_HELP_SCHEDULE_REFERENCE_PATTERN = "^[a-zA-Z0-9/.\\-]{1,20}$";

  @Override
  public void validate(
      ClaimResponse claim, SubmissionValidationContext context, AreaOfLaw areaOfLaw) {
    String regex = null;
    if (AreaOfLaw.LEGAL_HELP.equals(areaOfLaw)) {
      regex = LEGAL_HELP_SCHEDULE_REFERENCE_PATTERN;
    }
    validateFieldWithRegex(
        claim, areaOfLaw, claim.getScheduleReference(), "schedule_reference", regex, context);
  }

  @Override
  public int priority() {
    return 100;
  }
}
