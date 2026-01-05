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
   * Checks if there are any existing schema validation errors for a specific field in the claim's
   * technical messages.
   *
   * @param claim the claim whose validation messages should be checked
   * @param context the validation context providing access to claim reports
   * @param fieldName the field name to search for within technical validation messages
   * @return {@code true} if any technical message contains the specified fieldName; {@code false}
   *     otherwise
   */
  default boolean hasFieldSchemaValidationError(
      ClaimResponse claim, SubmissionValidationContext context, String fieldName) {

    return context.getClaimReport(claim.getId()).stream()
        .flatMap(report -> report.getMessages().stream())
        .map(ValidationMessagePatch::getTechnicalMessage)
        .filter(Objects::nonNull)
        .anyMatch(msg -> msg.contains(fieldName));
  }
}
