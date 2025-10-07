package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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

  private static final String CIVIL_SCHEDULE_REFERENCE_PATTERN = "^[a-zA-Z0-9/.\\-]{1,20}$";

  @Override
  public void validate(ClaimResponse claim, SubmissionValidationContext context, String areaOfLaw) {
    String regex = null;
    if (areaOfLaw.equals("CIVIL")) {
      regex = CIVIL_SCHEDULE_REFERENCE_PATTERN;
    }
    validateFieldWithRegex(
        claim, areaOfLaw, claim.getScheduleReference(), "schedule_reference", regex, context);
  }

  @Override
  public int priority() {
    return 100;
  }
}
