package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.getClaimMessages;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@DisplayName("Schedule reference claim validator test")
class ScheduleReferenceClaimValidatorTest {

  private ScheduleReferenceClaimValidator validator = new ScheduleReferenceClaimValidator();

  @ParameterizedTest(
      name =
          "{index} => claimId={0}, matterType={1}, areaOfLaw={2}, caseReferenceNumber={3}, scheduleReference={4}, regex={6}, expectError={7}")
  @CsvSource({
    "1, ab12:bc24, LEGAL HELP, 123, SCH123, '^[a-zA-Z0-9/.\\-]{1,20}$', false",
    "2, ab12:bc24, LEGAL HELP, 123, ABCDEFGHIJKLMNOPQRST123, '^[a-zA-Z0-9/.\\-]{1,20}$', true",
    "3, ab12:bc24, LEGAL HELP, 123, SCH/ABC-12.34, '^[a-zA-Z0-9/.\\-]{1,20}$', false",
    "4, ab12:bc24, LEGAL HELP, 123, Schedule Ref, '^[a-zA-Z0-9/.\\-]{1,20}$', true",
    "5, ab12:bc24, LEGAL HELP, 123, Schedule:Ref, '^[a-zA-Z0-9/.\\-]{1,20}$', true",
    "6, ab12:bc24, CRIME LOWER,,, '^[a-zA-Z0-9/.\\-]{1,20}$', false",
    "7, ab12:bc24, CRIME LOWER,, ABCD, '^[a-zA-Z0-9/.\\-]{1,20}$', false",
    "8, ABCD:EFGH, MEDIATION, 123, ABCDEFGHIJKLMNOPQRST, '^[a-zA-Z0-9/.\\-]{1,20}$', false",
    "9, ABCD:EFGH, MEDIATION, 123, ABCD, '^[a-zA-Z0-9/.\\-]{1,20}$', false"
  })
  void validateFormatForScheduleReference(
      int claimIdBit,
      String matterTypeCode,
      String areaOfLaw,
      String caseReferenceNumber,
      String scheduleReference,
      String regex,
      boolean expectError) {
    UUID claimId = new UUID(claimIdBit, claimIdBit);
    ClaimResponse claim =
        new ClaimResponse()
            .id(claimId.toString())
            .feeCode("feeCode1")
            .caseStartDate("2025-08-14")
            .caseConcludedDate("2025-09-14")
            .caseReferenceNumber(caseReferenceNumber)
            .scheduleReference(scheduleReference)
            .status(ClaimStatus.READY_TO_PROCESS)
            .uniqueFileNumber("010101/123")
            .matterTypeCode(matterTypeCode);

    SubmissionValidationContext context = new SubmissionValidationContext();
    context.addClaimReports(List.of(new ClaimValidationReport(claim.getId())));

    // Run validation
    validator.validate(claim, context, AreaOfLaw.LEGAL_HELP);

    if (expectError) {
      String expectedMessage =
          String.format(
              "schedule_reference (%s): does not match the regex pattern %s (provided value: %s)",
              areaOfLaw, regex, scheduleReference);
      assertThat(getClaimMessages(context, claimId.toString()).getFirst().getTechnicalMessage())
          .isEqualTo(expectedMessage);
    } else {
      assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
    }
  }
}
