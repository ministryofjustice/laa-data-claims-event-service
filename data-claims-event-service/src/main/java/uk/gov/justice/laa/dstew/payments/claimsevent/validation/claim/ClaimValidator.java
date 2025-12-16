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
   * Validates the given claim within the provided submission validation context.
   *
   * @param claim the claim to validate
   * @param context the submission validation context
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
