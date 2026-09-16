package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.SchemaValidationConfig;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.JsonSchemaValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/** Parameterized unit tests for case_stage_code using the real JsonSchemaValidator. */
class ClaimCaseStageCodeParameterizedTest {

  private ObjectMapper createMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
    return mapper;
  }

  private ClaimSchemaValidator realValidator() throws Exception {
    ObjectMapper mapper = createMapper();
    SchemaValidationConfig schemaValidationConfig =
        new SchemaValidationConfig(
            mapper,
            new ClassPathResource("schemas/submission-fields.schema.json"),
            new ClassPathResource("schemas/claim-fields.schema.json"));
    JsonSchemaValidator jsonSchemaValidator =
        new JsonSchemaValidator(
            mapper,
            schemaValidationConfig.jsonSchemas(),
            schemaValidationConfig.schemaValidationErrorMessages());
    return new ClaimSchemaValidator(jsonSchemaValidator);
  }

  @ParameterizedTest
  @ValueSource(strings = {"FPL01", "FPL10", "FPL20", "FPL21", "FPC01", "MHL01", "MHL16"})
  void shouldAcceptValidCaseStageCodes(String caseStageCode) throws Exception {
    ClaimSchemaValidator validator = realValidator();

    String claimId = new UUID(1, 1).toString();
    ClaimResponse claimResponse = new ClaimResponse();
    claimResponse.setId(claimId);
    claimResponse.setLineNumber(1);
    claimResponse.setStatus(
        uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus.READY_TO_PROCESS);
    claimResponse.setNetDisbursementAmount(BigDecimal.valueOf(20.10));
    claimResponse.setDisbursementsVatAmount(BigDecimal.valueOf(10.20));
    claimResponse.setFeeCode("FeeCode");
    claimResponse.setCaseStageCode(caseStageCode);

    SubmissionValidationContext ctx = new SubmissionValidationContext();
    validator.validate(claimResponse, ctx, AreaOfLaw.LEGAL_HELP);
    assertFalse(ctx.hasErrors(), "Expected no errors for valid case_stage_code: " + caseStageCode);
  }

  @ParameterizedTest
  @ValueSource(strings = {"FPL22", "FPC04", "MHL17", "ABC12", "FPL1", ""})
  void shouldRejectInvalidCaseStageCodes(String caseStageCode) throws Exception {
    ClaimSchemaValidator validator = realValidator();

    String claimId = new UUID(2, 2).toString();
    ClaimResponse claimResponse = new ClaimResponse();
    claimResponse.setId(claimId);
    claimResponse.setLineNumber(1);
    claimResponse.setStatus(
        uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus.READY_TO_PROCESS);
    claimResponse.setNetDisbursementAmount(BigDecimal.valueOf(20.10));
    claimResponse.setDisbursementsVatAmount(BigDecimal.valueOf(10.20));
    claimResponse.setFeeCode("FeeCode");
    claimResponse.setCaseStageCode(caseStageCode);

    SubmissionValidationContext ctx = new SubmissionValidationContext();
    validator.validate(claimResponse, ctx, AreaOfLaw.LEGAL_HELP);
    assertTrue(ctx.hasErrors(), "Expected errors for invalid case_stage_code: " + caseStageCode);
  }
}
