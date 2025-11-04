package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.getClaimMessages;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.StageReachedClaimValidator.STAGE_REACHED_CRIME_LOWER_PATTERN;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.StageReachedClaimValidator.STAGE_REACHED_LEGAL_HELP_PATTERN;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.SchemaValidationConfig;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@DisplayName("Stage reached claim validator test")
class StageReachedClaimValidatorTest {

  StageReachedClaimValidator validator;

  @BeforeEach
  void beforeEach() throws IOException {
    SchemaValidationConfig config =
        new SchemaValidationConfig(
            new ObjectMapper(),
            new ClassPathResource("schemas/submission-fields.schema.json"),
            new ClassPathResource("schemas/claim-fields.schema.json"));
    validator = new StageReachedClaimValidator(config.schemaValidationErrorMessages());
  }

  @ParameterizedTest(
      name =
          "{index} => claimId={0}, stageReachedCode={1}, areaOfLaw={2}, "
              + "expectError={3}, displayMessage={4}")
  @CsvSource({
    "1, AABB, LEGAL_HELP, true, Stage Reached Code must be exactly 2 alphanumeric characters for Legal Help claims",
    "2, AZ, LEGAL_HELP, false, NA",
    "3, C9, LEGAL_HELP, false, NA",
    "4, A!, LEGAL_HELP, true, Stage Reached Code must be exactly 2 alphanumeric characters for Legal Help claims",
    "5, A1, CRIME_LOWER, true, Stage Reached Code must be exactly 4 uppercase letters for Crime Lower claims",
    "6, A-CD, CRIME_LOWER, true, Stage Reached Code must be exactly 4 uppercase letters for Crime Lower claims",
    "7, ABCD, CRIME_LOWER, true, Stage Reached Code must be exactly 4 uppercase letters for Crime Lower claims"
  })
  void checkStageReachedCode(
      int claimIdBit,
      String stageReachedCode,
      AreaOfLaw areaOfLaw,
      boolean expectError,
      String expectedErrorMsg) {
    UUID claimId = new UUID(claimIdBit, claimIdBit);
    ClaimResponse claim =
        new ClaimResponse()
            .id(claimId.toString())
            .feeCode("feeCode1")
            .caseStartDate("2025-08-14")
            .status(ClaimStatus.READY_TO_PROCESS)
            .uniqueFileNumber("010101/123")
            .stageReachedCode(stageReachedCode);

    SubmissionValidationContext context = new SubmissionValidationContext();

    // Run validation
    validator.validate(claim, context, areaOfLaw);

    if (expectError) {
      String expectedTechnicalMessage =
          String.format(
              "stage_reached_code (%s): does not match the regex pattern %s (provided value: %s)",
              areaOfLaw, getRegex(areaOfLaw), stageReachedCode);
      assertThat(getClaimMessages(context, claimId.toString()).getFirst().getTechnicalMessage())
          .isEqualTo(expectedTechnicalMessage);
      assertThat(getClaimMessages(context, claimId.toString()).getFirst().getDisplayMessage())
          .isEqualTo(expectedErrorMsg);
    } else {
      assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
    }
  }

  @ParameterizedTest(name = "stageReachedCode={0}")
  @ValueSource(
      strings = {
        "INVA", "INVB", "INVC", "INVD", "INVE", "INVF", "INVG", "INVH", "INVI", "INVJ",
        "INVK", "INVL", "INVM", "PRIA", "PRIB", "PRIC", "PRID", "PRIE", "PROC", "PROD",
        "PROE", "PROF", "PROH", "PROI", "PROJ", "PROK", "PROL", "PROP", "PROT", "PROU",
        "PROV", "PROW", "APPA", "APPB", "APPC", "ASMS", "ASPL", "ASAS", "YOUE", "YOUF",
        "YOUK", "YOUL", "YOUX", "YOUY", "VOID"
      })
  void checkStageReachedCodeForAllAllowedCodesForCrimeLower(String stageReachedCode) {
    UUID claimId = new UUID(1, 1);
    ClaimResponse claim =
        new ClaimResponse()
            .id(claimId.toString())
            .feeCode("feeCode1")
            .caseStartDate("2025-08-14")
            .status(ClaimStatus.READY_TO_PROCESS)
            .uniqueFileNumber("010101/123")
            .stageReachedCode(stageReachedCode);

    SubmissionValidationContext context = new SubmissionValidationContext();

    // Run validation
    validator.validate(claim, context, AreaOfLaw.CRIME_LOWER);
    assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
  }

  @Test
  void exceptionIsThrownForUnrecognisedAreaOfLaw() {
    UUID claimId = new UUID(1, 1);
    ClaimResponse claim = new ClaimResponse().id(claimId.toString());

    SubmissionValidationContext context = new SubmissionValidationContext();

    // Run validation
    assertThatThrownBy(
            () -> {
              validator.validate(claim, context, AreaOfLaw.valueOf("INVALID"));
            })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("No enum constant")
        .hasNoCause();
  }

  private String getRegex(AreaOfLaw areaOfLaw) {
    return AreaOfLaw.LEGAL_HELP.equals(areaOfLaw)
        ? STAGE_REACHED_LEGAL_HELP_PATTERN
        : STAGE_REACHED_CRIME_LOWER_PATTERN;
  }
}
