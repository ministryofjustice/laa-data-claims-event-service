package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.getClaimMessages;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    "1, AABB, CIVIL, '^[a-zA-Z0-9]{2}$', true",
    "2, AZ, CIVIL, '^[a-zA-Z0-9]{2}$', false",
    "3, C9, CIVIL, '^[a-zA-Z0-9]{2}$', false",
    "4, A!, CIVIL, '^[a-zA-Z0-9]{2}$', true",
    "5, ABCD, CRIME, '^[A-Z]{4}$', false",
    "6, A1, CRIME, '^[A-Z]{4}$', true",
    "7, A-CD, CRIME, '^[A-Z]{4}$', true",
  })
  void checkStageReachedCode(
      int claimIdBit,
      String stageReachedCode,
      String areaOfLaw,
      String regex,
      boolean expectError) {
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
              areaOfLaw, regex, stageReachedCode);
      assertThat(getClaimMessages(context, claimId.toString()).getFirst().getTechnicalMessage())
          .isEqualTo(expectedMessage);
    } else {
      assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
    }
  }
}
