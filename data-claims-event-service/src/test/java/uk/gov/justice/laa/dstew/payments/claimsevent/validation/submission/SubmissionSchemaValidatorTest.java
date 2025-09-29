package uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.assertContextClaimError;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.JsonSchemaValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@ExtendWith(MockitoExtension.class)
@DisplayName("Submission Schema Validator Test")
class SubmissionSchemaValidatorTest {

  @Mock private JsonSchemaValidator jsonSchemaValidator;

  private SubmissionSchemaValidator submissionSchemaValidator;

  @BeforeEach
  void beforeEach() {
    submissionSchemaValidator = new SubmissionSchemaValidator(jsonSchemaValidator);
  }

  @Test
  @DisplayName("Should have no errors if json schema validator returns no errors")
  void shouldHaveNoErrorsIfJsonSchemaValidatorReturnsNoErrors() {
    // Given
    List<ValidationMessagePatch> emptyErrorsList = List.of();
    SubmissionResponse submissionResponse = SubmissionResponse.builder().build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    when(jsonSchemaValidator.validate("submission", submissionResponse))
        .thenReturn(emptyErrorsList);
    // When
    submissionSchemaValidator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertFalse(submissionValidationContext.hasErrors());
  }

  @Test
  @DisplayName("Should have errors if json schema validator returns errors")
  void shouldHaveErrorsIfJsonSchemaValidatorReturnsErrors() {
    // Given
    // Not the usual error returned by JSON schema validator, this validator should just add
    // whatever
    //  the schema validator returns so this is fine.
    List<ValidationMessagePatch> emptyErrorsList =
        List.of(ClaimValidationError.SUBMISSION_STATE_IS_NULL.toPatch());
    SubmissionResponse submissionResponse = SubmissionResponse.builder().build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    when(jsonSchemaValidator.validate("submission", submissionResponse))
        .thenReturn(emptyErrorsList);
    // When
    submissionSchemaValidator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertTrue(submissionValidationContext.hasErrors());
    assertContextClaimError(
        submissionValidationContext, ClaimValidationError.SUBMISSION_STATE_IS_NULL);
  }
}
