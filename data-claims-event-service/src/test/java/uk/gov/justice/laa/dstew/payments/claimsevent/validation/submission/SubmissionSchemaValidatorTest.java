package uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.assertContextClaimError;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.JsonSchemaValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationError;

@ExtendWith(MockitoExtension.class)
@DisplayName("Submission Schema Validator Test")
class SubmissionSchemaValidatorTest {

  @Mock private JsonSchemaValidator jsonSchemaValidator;

  private SubmissionSchemaValidator submissionSchemaValidator;

  @BeforeEach
  void beforeEach() {
    submissionSchemaValidator = new SubmissionSchemaValidator(jsonSchemaValidator);
  }

  @ParameterizedTest
  @EnumSource(AreaOfLaw.class)
  @DisplayName("Should have no errors if json schema validator returns no errors")
  void shouldHaveNoErrorsIfJsonSchemaValidatorReturnsNoErrors(AreaOfLaw areaOfLaw) {
    // Given
    List<ValidationMessagePatch> emptyErrorsList = List.of();
    SubmissionResponse submissionResponse =
        SubmissionResponse.builder().areaOfLaw(areaOfLaw).build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    when(jsonSchemaValidator.validate("submission", submissionResponse, areaOfLaw))
        .thenReturn(emptyErrorsList);
    // When
    submissionSchemaValidator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertFalse(submissionValidationContext.hasErrors());
  }

  @ParameterizedTest
  @EnumSource(AreaOfLaw.class)
  @DisplayName("Should have errors if json schema validator returns errors")
  void shouldHaveErrorsIfJsonSchemaValidatorReturnsErrors(AreaOfLaw areaOfLaw) {
    // Given
    // Not the usual error returned by JSON schema validator, this validator should just add
    // whatever the schema validator returns so this is fine.
    List<ValidationMessagePatch> emptyErrorsList =
        List.of(SubmissionValidationError.SUBMISSION_STATUS_IS_NULL.toPatch());
    SubmissionResponse submissionResponse =
        SubmissionResponse.builder().areaOfLaw(areaOfLaw).build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    when(jsonSchemaValidator.validate("submission", submissionResponse, areaOfLaw))
        .thenReturn(emptyErrorsList);
    // When
    submissionSchemaValidator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertTrue(submissionValidationContext.hasErrors());
    assertContextClaimError(
        submissionValidationContext, SubmissionValidationError.SUBMISSION_STATUS_IS_NULL);
  }
}
