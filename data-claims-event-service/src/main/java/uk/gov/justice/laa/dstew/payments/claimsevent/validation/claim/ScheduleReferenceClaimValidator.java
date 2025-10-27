package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate.StrategyTypes;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.model.ValidationErrorMessage;

/**
 * Checks the schedule reference value is valid.
 *
 * @author Jamie Briggs
 * @see ClaimResponse
 * @see SubmissionValidationContext
 * @see BasicClaimValidator
 */
@Component
public final class ScheduleReferenceClaimValidator extends RegexClaimValidator
    implements ClaimWithAreaOfLawValidator {

  private static final String CIVIL_SCHEDULE_REFERENCE_PATTERN = "^[a-zA-Z0-9/.\\-]{1,20}$";

  public ScheduleReferenceClaimValidator(
      Map<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages) {
    super(schemaValidationErrorMessages);
  }

  @Override
  public void validate(ClaimResponse claim, SubmissionValidationContext context, String areaOfLaw) {
    String regex = null;
    if (StrategyTypes.CIVIL.contains(areaOfLaw.toUpperCase(Locale.ROOT))) {
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
