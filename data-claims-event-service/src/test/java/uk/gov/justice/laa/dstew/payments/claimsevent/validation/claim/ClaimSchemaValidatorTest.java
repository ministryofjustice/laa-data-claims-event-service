package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.assertContextClaimError;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.SchemaValidationConfig;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
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

  @Test
  @DisplayName("Should accept valid case_stage_code when using real JsonSchemaValidator")
  void shouldAcceptValidCaseStageCodeWithRealValidator() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);

    SchemaValidationConfig schemaValidationConfig =
        new SchemaValidationConfig(
            mapper,
            new ClassPathResource("schemas/submission-fields.schema.json"),
            new ClassPathResource("schemas/claim-fields.schema.json"));

    JsonSchemaValidator realJsonSchemaValidator =
        new JsonSchemaValidator(
            mapper,
            schemaValidationConfig.jsonSchemas(),
            schemaValidationConfig.schemaValidationErrorMessages());

    ClaimSchemaValidator realClaimSchemaValidator =
        new ClaimSchemaValidator(realJsonSchemaValidator);

    String claimId = new UUID(1, 1).toString();
    ClaimResponse claimResponse = new ClaimResponse();
    claimResponse.setId(claimId);
    claimResponse.setLineNumber(1);
    claimResponse.setStatus(
        uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus.READY_TO_PROCESS);
    claimResponse.setNetDisbursementAmount(BigDecimal.valueOf(20.10));
    claimResponse.setDisbursementsVatAmount(BigDecimal.valueOf(10.20));
    claimResponse.setFeeCode("FeeCode");
    claimResponse.setCaseStageCode("FPL01");

    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    realClaimSchemaValidator.validate(
        claimResponse, submissionValidationContext, AreaOfLaw.LEGAL_HELP);

    assertFalse(submissionValidationContext.hasErrors());
  }

  @Test
  @DisplayName("Should reject invalid case_stage_code when using real JsonSchemaValidator")
  void shouldRejectInvalidCaseStageCodeWithRealValidator() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);

    SchemaValidationConfig schemaValidationConfig =
        new SchemaValidationConfig(
            mapper,
            new ClassPathResource("schemas/submission-fields.schema.json"),
            new ClassPathResource("schemas/claim-fields.schema.json"));

    JsonSchemaValidator realJsonSchemaValidator =
        new JsonSchemaValidator(
            mapper,
            schemaValidationConfig.jsonSchemas(),
            schemaValidationConfig.schemaValidationErrorMessages());

    ClaimSchemaValidator realClaimSchemaValidator =
        new ClaimSchemaValidator(realJsonSchemaValidator);

    String claimId = new UUID(2, 2).toString();
    ClaimResponse claimResponse = new ClaimResponse();
    claimResponse.setId(claimId);
    claimResponse.setLineNumber(1);
    claimResponse.setStatus(
        uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus.READY_TO_PROCESS);
    claimResponse.setNetDisbursementAmount(BigDecimal.valueOf(20.10));
    claimResponse.setDisbursementsVatAmount(BigDecimal.valueOf(10.20));
    claimResponse.setFeeCode("FeeCode");
    claimResponse.setCaseStageCode("FPL22"); // invalid per schema

    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();
    realClaimSchemaValidator.validate(
        claimResponse, submissionValidationContext, AreaOfLaw.LEGAL_HELP);

    assertTrue(submissionValidationContext.hasErrors());
    // assert that at least one claim message contains the expected display message
    var messages =
        submissionValidationContext
            .getClaimReport(claimId)
            .map(ClaimValidationReport::getMessages)
            .orElse(java.util.List.of());
    boolean contains =
        messages.stream()
            .anyMatch(
                m ->
                    m.getDisplayMessage() != null
                        && m.getDisplayMessage().contains("Case Stage/Level Code must be valid"));
    org.junit.jupiter.api.Assertions.assertTrue(
        contains, "Expected display message to mention Case Stage/Level Code must be valid");
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
