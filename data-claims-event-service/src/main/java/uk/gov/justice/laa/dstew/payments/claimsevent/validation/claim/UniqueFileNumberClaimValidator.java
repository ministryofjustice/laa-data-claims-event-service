package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import java.time.LocalDate;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.EventServiceIllegalArgumentException;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.UniqueFileNumberUtil;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Validates the unique file number of the given claim to ensure it contains a valid and non-future
 * date in the format DDMMYY. If the date is invalid or in the future, an error is added to the
 * submission validation context.
 *
 * @author Jamie Briggs
 * @see ClaimResponse
 * @see SubmissionValidationContext
 * @see BasicClaimValidator
 */
@Component
public final class UniqueFileNumberClaimValidator implements BasicClaimValidator, ClaimValidator {

  @Override
  public void validate(ClaimResponse claim, SubmissionValidationContext context) {
    String uniqueFileNumber = claim.getUniqueFileNumber();
    if (uniqueFileNumber != null && uniqueFileNumber.length() > 1) {
      try {
        LocalDate date = UniqueFileNumberUtil.parse(uniqueFileNumber);
        if (date.isAfter(LocalDate.now())) {
          context.addClaimError(
              claim.getId(), ClaimValidationError.INVALID_DATE_IN_UNIQUE_FILE_NUMBER);
        }
      } catch (EventServiceIllegalArgumentException e) {
        context.addClaimError(
            claim.getId(), ClaimValidationError.INVALID_DATE_IN_UNIQUE_FILE_NUMBER);
      }
    }
  }

  @Override
  public int priority() {
    return 100;
  }
}
