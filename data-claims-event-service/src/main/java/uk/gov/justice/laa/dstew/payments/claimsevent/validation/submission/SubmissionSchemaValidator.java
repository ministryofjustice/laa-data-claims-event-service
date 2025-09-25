package uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.JsonSchemaValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Validates that a submission's JSON schema is valid.
 *
 * @author Jamie Briggs
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionSchemaValidator implements SubmissionValidator {

  private final JsonSchemaValidator jsonSchemaValidator;


  @Override
  public void validate(final SubmissionResponse submission, SubmissionValidationContext context) {
    context.addSubmissionValidationErrors(jsonSchemaValidator.validate("submission", submission));
  }

  @Override
  public int priority() {
    return 10;
  }

}
