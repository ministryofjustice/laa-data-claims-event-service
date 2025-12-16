package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import java.util.Objects;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Interface for a claim validator. Implementations should be annotated with @Component.
 *
 * @author Jamie Briggs
 */
public interface ClaimValidator {

  int priority();

  /**
   * Checks whether any validation error message associated with the given claim contains the
   * specified text.
   *
   * @param claim the claim whose validation messages should be checked
   * @param context the validation context providing access to claim reports
   * @param stringToMatch the text to search for within validation message display content
   * @return {@code true} if any message contains the specified text; {@code false} otherwise
   */
  default boolean isErrorPresent(
      ClaimResponse claim, SubmissionValidationContext context, String stringToMatch) {

    return context.getClaimReport(claim.getId()).stream()
        .flatMap(report -> report.getMessages().stream())
        .map(ValidationMessagePatch::getDisplayMessage)
        .filter(Objects::nonNull)
        .anyMatch(msg -> msg.contains(stringToMatch));
  }
}
