package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.getClaimMessages;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.AreaOfLaw.CRIME_LOWER;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.AreaOfLaw.LEGAL_HELP;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.StageReachedClaimValidator.STAGE_REACHED_CRIME_LOWER_PATTERN;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.StageReachedClaimValidator.STAGE_REACHED_LEGAL_HELP_PATTERN;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@DisplayName("Stage reached claim validator test")
class StageReachedClaimValidatorTest {

  StageReachedClaimValidator validator = new StageReachedClaimValidator();

  @ParameterizedTest(
      name =
          "{index} => claimId={0}, stageReachedCode={1}, areaOfLaw={2}, regex={3}, "
              + "expectError={4}")
  @CsvSource({
    "1, AABB, LEGAL HELP, true",
    "2, AZ, LEGAL HELP, false",
    "3, C9, LEGAL HELP, false",
    "4, A!, LEGAL HELP, true",
    "5, A1, CRIME LOWER, true",
    "6, A-CD, CRIME LOWER, true",
    "7, ABCD, CRIME LOWER, true"
  })
  void checkStageReachedCode(
      int claimIdBit, String stageReachedCode, String areaOfLaw, boolean expectError) {
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
      String expectedMessage =
          String.format(
              "stage_reached_code (%s): does not match the regex pattern %s (provided value: %s)",
              areaOfLaw, getRegex(areaOfLaw), stageReachedCode);
      assertThat(getClaimMessages(context, claimId.toString()).getFirst().getTechnicalMessage())
          .isEqualTo(expectedMessage);
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
    validator.validate(claim, context, CRIME_LOWER.getValue());
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
              validator.validate(claim, context, "CIVIL");
            })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown area of law: CIVIL")
        .hasNoCause();
  }

  private String getRegex(String areaOfLaw) {
    return areaOfLaw.equals(LEGAL_HELP.getValue())
        ? STAGE_REACHED_LEGAL_HELP_PATTERN
        : STAGE_REACHED_CRIME_LOWER_PATTERN;
  }
}
