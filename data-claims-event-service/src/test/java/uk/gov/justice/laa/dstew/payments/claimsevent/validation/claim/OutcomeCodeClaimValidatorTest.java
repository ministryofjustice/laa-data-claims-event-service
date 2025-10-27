package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.getClaimMessages;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.OutcomeCodeClaimValidator.OUTCOME_CODE_CIVIL_PATTERN;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.OutcomeCodeClaimValidator.OUTCOME_CODE_CRIME_PATTERN;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.OutcomeCodeClaimValidator.OUTCOME_CODE_MEDIATION_PATTERN;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@DisplayName("Outcome code claim validator test")
class OutcomeCodeClaimValidatorTest {

  OutcomeCodeClaimValidator validator = new OutcomeCodeClaimValidator(new HashMap<>());

  private final Map<String, String> outcomeCodePatterns =
      Map.of(
          "CIVIL", OUTCOME_CODE_CIVIL_PATTERN,
          "CRIME", OUTCOME_CODE_CRIME_PATTERN,
          "MEDIATION", OUTCOME_CODE_MEDIATION_PATTERN);

  @ParameterizedTest(
      name = "{index} => claimId={0}, outcomeCode={1}, areaOfLaw={2}, expectError={3}")
  @CsvSource({
    "1, IX, CIVIL, false",
    "2, ABCD, CIVIL, true",
    "3, C9, CIVIL, false",
    "4, --, CIVIL, false",
    "5, I@, CIVIL, true",
    "6, I, CIVIL, true",
    "7, cp01, CRIME, false",
    "8, CP01, CRIME, false",
    "9, CP28, CRIME, false",
    "10, CN04, CRIME, false",
    "11, CN13, CRIME, false",
    "12, PL01, CRIME, false",
    "13, PL14, CRIME, false",
    "14, CP29, CRIME, true",
    "15, CN14, CRIME, true",
    "16, PL15, CRIME, true",
    "17, XY01, CRIME, true",
    "18, A!, MEDIATION, true",
    "19, A, MEDIATION, false",
    "20, b, MEDIATION, false",
    "21, -, MEDIATION, true",
    "22, X, MEDIATION, true",
    "23, AB, MEDIATION, true"
  })
  void checkStageReachedCode(
      int claimIdBit, String outcomeCode, String areaOfLaw, boolean expectError) {
    UUID claimId = new UUID(claimIdBit, claimIdBit);
    ClaimResponse claim =
        new ClaimResponse()
            .id(claimId.toString())
            .feeCode("feeCode1")
            .caseStartDate("2025-08-14")
            .status(ClaimStatus.READY_TO_PROCESS)
            .uniqueFileNumber("010101/123")
            .outcomeCode(outcomeCode);

    SubmissionValidationContext context = new SubmissionValidationContext();

    // Run validation
    validator.validate(claim, context, areaOfLaw);

    if (expectError) {
      String expectedMessage =
          String.format(
              "outcome_code (%s): does not match the regex pattern %s (provided value: %s)",
              areaOfLaw, outcomeCodePatterns.get(areaOfLaw), outcomeCode);
      assertThat(getClaimMessages(context, claimId.toString()).getFirst().getTechnicalMessage())
          .isEqualTo(expectedMessage);
    } else {
      assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
    }
  }
}
