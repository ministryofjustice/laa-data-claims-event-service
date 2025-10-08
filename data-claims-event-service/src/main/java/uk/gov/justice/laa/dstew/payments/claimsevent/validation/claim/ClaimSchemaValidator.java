package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.JsonSchemaValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Validates that a claim's scheme is valid.
 *
 * @author Jamie Briggs
 * @see ClaimResponse
 * @see SubmissionValidationContext
 * @see BasicClaimValidator
 */
@Component
@RequiredArgsConstructor
public class ClaimSchemaValidator implements ClaimValidator, BasicClaimValidator {

  private final JsonSchemaValidator jsonSchemaValidator;

  @Override
  public void validate(ClaimResponse claim, SubmissionValidationContext context) {
    List<ValidationMessagePatch> schemaMessages = jsonSchemaValidator.validate("claim", claim);
    context.addClaimMessages(claim.getId(), schemaMessages);
  }

  @Override
  public int priority() {
    return 1;
  }
}
