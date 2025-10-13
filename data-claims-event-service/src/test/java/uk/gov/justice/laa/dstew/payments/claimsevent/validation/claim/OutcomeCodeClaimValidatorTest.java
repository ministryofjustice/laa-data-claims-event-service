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

@DisplayName("Outcome code claim validator test")
class OutcomeCodeClaimValidatorTest {

  OutcomeCodeClaimValidator validator = new OutcomeCodeClaimValidator();

  @ParameterizedTest(
      name =
          "{index} => claimId={0}, outcomeCode={1}, areaOfLaw={2}, regex={3}, " + "expectError={4}")
  @CsvSource({
    "1, IX, CIVIL, '^[A-Za-z0-9-]{2}$', false",
    "2, ABCD, CIVIL, '^[A-Za-z0-9-]{2}$', true",
    "3, C9, CIVIL, '^[A-Za-z0-9-]{2}$', false",
    "4, --, CIVIL, '^[A-Za-z0-9-]{2}$', false",
    "5, I@, CIVIL, '^[A-Za-z0-9-]{2}$', true",
    "6, I, CIVIL, '^[A-Za-z0-9-]{2}$', true",
    "7, CP01, CRIME, '^(CP(0[1-9]|1[0-9]|2[0-8])|CN(0[1-9]|1[0-3])|PL(0[1-9]|1[0-4]))?$', false",
    "8, CP28, CRIME, '^(CP(0[1-9]|1[0-9]|2[0-8])|CN(0[1-9]|1[0-3])|PL(0[1-9]|1[0-4]))?$', false",
    "9, CN04, CRIME, '^(CP(0[1-9]|1[0-9]|2[0-8])|CN(0[1-9]|1[0-3])|PL(0[1-9]|1[0-4]))?$', false",
    "10, CN13, CRIME, '^(CP(0[1-9]|1[0-9]|2[0-8])|CN(0[1-9]|1[0-3])|PL(0[1-9]|1[0-4]))?$', false",
    "11, PL01, CRIME, '^(CP(0[1-9]|1[0-9]|2[0-8])|CN(0[1-9]|1[0-3])|PL(0[1-9]|1[0-4]))?$', false",
    "12, PL14, CRIME, '^(CP(0[1-9]|1[0-9]|2[0-8])|CN(0[1-9]|1[0-3])|PL(0[1-9]|1[0-4]))?$', false",
    "13, CP29, CRIME, '^(CP(0[1-9]|1[0-9]|2[0-8])|CN(0[1-9]|1[0-3])|PL(0[1-9]|1[0-4]))?$', true",
    "14, CN14, CRIME, '^(CP(0[1-9]|1[0-9]|2[0-8])|CN(0[1-9]|1[0-3])|PL(0[1-9]|1[0-4]))?$', true",
    "15, PL15, CRIME, '^(CP(0[1-9]|1[0-9]|2[0-8])|CN(0[1-9]|1[0-3])|PL(0[1-9]|1[0-4]))?$', true",
    "16, XY01, CRIME, '^(CP(0[1-9]|1[0-9]|2[0-8])|CN(0[1-9]|1[0-3])|PL(0[1-9]|1[0-4]))?$', true",
    "17, A!, MEDIATION, '^(A|B|S|C|P)?$', true",
    "18, A, MEDIATION, '^(A|B|S|C|P)?$', false",
    "19, -, MEDIATION, '^(A|B|S|C|P)?$', true",
    "20, X, MEDIATION, '^(A|B|S|C|P)?$', true",
    "21, AB, MEDIATION, '^(A|B|S|C|P)?$', true"
  })
  void checkStageReachedCode(
      int claimIdBit, String outcomeCode, String areaOfLaw, String regex, boolean expectError) {
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
              areaOfLaw, regex, outcomeCode);
      assertThat(getClaimMessages(context, claimId.toString()).getFirst().getTechnicalMessage())
          .isEqualTo(expectedMessage);
    } else {
      assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
    }
  }
}
