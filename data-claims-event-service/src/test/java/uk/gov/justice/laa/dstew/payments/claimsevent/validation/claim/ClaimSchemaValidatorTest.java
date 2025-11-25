package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.assertContextClaimError;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.JsonSchemaValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@ExtendWith(MockitoExtension.class)
class ClaimSchemaValidatorTest {

  @Mock private JsonSchemaValidator jsonSchemaValidator;

  private ClaimSchemaValidator claimSchemaValidator;

  @BeforeEach
  void beforeEach() {
    claimSchemaValidator = new ClaimSchemaValidator(jsonSchemaValidator);
  }

  @ParameterizedTest
  @EnumSource(value = AreaOfLaw.class)
  @DisplayName("Should have no errors if json schema validator returns no errors")
  void shouldHaveNoErrorsIfJsonSchemaValidatorReturnsNoErrors(AreaOfLaw areaOfLaw) {
    // Given
    List<ValidationMessagePatch> emptyErrorsList = List.of();
    String claimId = new UUID(1, 1).toString();
    ClaimResponse claimResponse = ClaimResponse.builder().id(claimId).build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    when(jsonSchemaValidator.validate("claim", claimResponse, areaOfLaw))
        .thenReturn(emptyErrorsList);
    // When
    claimSchemaValidator.validate(claimResponse, submissionValidationContext, areaOfLaw);
    // Then
    assertFalse(submissionValidationContext.hasErrors());
  }

  @ParameterizedTest
  @EnumSource(value = AreaOfLaw.class)
  @DisplayName("Should have errors if json schema validator returns errors")
  void shouldHaveErrorsIfJsonSchemaValidatorReturnsErrors(AreaOfLaw areaOfLaw) {
    // Given
    // Not the usual error returned by JSON schema validator, this validator should just add
    // whatever the schema validator returns so this is fine.
    String claimId = new UUID(1, 1).toString();
    List<ValidationMessagePatch> errorList =
        List.of(ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER.toPatch());
    ClaimResponse claimResponse = ClaimResponse.builder().id(claimId).build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    when(jsonSchemaValidator.validate("claim", claimResponse, areaOfLaw)).thenReturn(errorList);
    // When
    claimSchemaValidator.validate(claimResponse, submissionValidationContext, areaOfLaw);
    // Then
    assertTrue(submissionValidationContext.hasErrors());
    assertContextClaimError(
        submissionValidationContext,
        claimId,
        ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER);
  }
}
