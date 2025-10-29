package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.getClaimMessages;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.OutcomeCodeClaimValidator.OUTCOME_CODE_CRIME_LOWER_PATTERN;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.OutcomeCodeClaimValidator.OUTCOME_CODE_LEGAL_HELP_PATTERN;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.OutcomeCodeClaimValidator.OUTCOME_CODE_MEDIATION_PATTERN;

import java.util.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionAreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@DisplayName("Outcome code claim validator test")
class OutcomeCodeClaimValidatorTest {

  OutcomeCodeClaimValidator validator = new OutcomeCodeClaimValidator();

  private final Map<BulkSubmissionAreaOfLaw, String> outcomeCodePatterns =
      Map.of(
          BulkSubmissionAreaOfLaw.LEGAL_HELP, OUTCOME_CODE_LEGAL_HELP_PATTERN,
          BulkSubmissionAreaOfLaw.CRIME_LOWER, OUTCOME_CODE_CRIME_LOWER_PATTERN,
          BulkSubmissionAreaOfLaw.MEDIATION, OUTCOME_CODE_MEDIATION_PATTERN);

  @ParameterizedTest(
      name = "{index} => claimId={0}, outcomeCode={1}, areaOfLaw={2}, expectError={3}")
  @CsvSource({
    "1, IX, LEGAL_HELP, false",
    "2, ABCD, LEGAL_HELP, true",
    "3, C9, LEGAL_HELP, false",
    "4, --, LEGAL_HELP, false",
    "5, I@, LEGAL_HELP, true",
    "6, I, LEGAL_HELP, true",
    "7, cp01, CRIME_LOWER, false",
    "8, CP01, CRIME_LOWER, false",
    "9, CP28, CRIME_LOWER, false",
    "10, CN04, CRIME_LOWER, false",
    "11, CN13, CRIME_LOWER, false",
    "12, PL01, CRIME_LOWER, false",
    "13, PL14, CRIME_LOWER, false",
    "14, CP29, CRIME_LOWER, true",
    "15, CN14, CRIME_LOWER, true",
    "16, PL15, CRIME_LOWER, true",
    "17, XY01, CRIME_LOWER, true",
    "18, A!, MEDIATION, true",
    "19, A, MEDIATION, false",
    "20, b, MEDIATION, false",
    "21, -, MEDIATION, true",
    "22, X, MEDIATION, true",
    "23, AB, MEDIATION, true"
  })
  void checkStageReachedCode(
      int claimIdBit, String outcomeCode, BulkSubmissionAreaOfLaw areaOfLaw, boolean expectError) {
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
