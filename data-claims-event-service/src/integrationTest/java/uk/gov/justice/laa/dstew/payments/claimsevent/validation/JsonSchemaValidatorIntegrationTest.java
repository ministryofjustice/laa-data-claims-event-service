package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimFields;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionFields;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;

@SpringBootTest
class JsonSchemaValidatorIntegrationTest {

  public static final String CLAIM_SCHEMA = "claim";
  @Autowired private JsonSchemaValidator jsonSchemaValidator;

  @Test
  void validateReturnsErrorsForInvalidSubmission() {
    SubmissionFields submission = new SubmissionFields();
    submission.setOfficeAccountNumber("abc123");
    submission.setSubmissionPeriod("OCTOBER-2024");
    submission.setAreaOfLaw("INVALID");
    submission.setNumberOfClaims(-1);
    submission.setStatus(SubmissionStatus.CREATED);
    List<String> errors = jsonSchemaValidator.validate("submission", submission);

    assertThat(errors)
        .containsExactlyInAnyOrder(
            "office_account_number: does not match the regex pattern ^[A-Z0-9]{6}$ (provided value: abc123)",
            "submission_period: does not match the regex pattern ^(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)-[0-9]{4}$ (provided value: OCTOBER-2024)",
            "area_of_law: does not have a value in the enumeration [\"CIVIL\", \"CRIME\", \"MEDIATION\", \"CRIME LOWER\", \"LEGAL HELP\"] (provided value: INVALID)",
            "number_of_claims: must have a minimum value of 0 (provided value: -1)");
  }

  @Test
  void validateReturnsEmptyListForValidSubmission() {
    SubmissionFields submission = new SubmissionFields();
    submission.setSubmissionId(UUID.randomUUID());
    submission.setBulkSubmissionId(UUID.randomUUID());
    submission.setStatus(SubmissionStatus.CREATED);
    submission.setScheduleNumber("abc123");
    submission.setOfficeAccountNumber("2Q286D");
    submission.setSubmissionPeriod("OCT-2024");
    submission.setAreaOfLaw("CRIME");
    submission.isNilSubmission(false);
    submission.setNumberOfClaims(3);

    List<String> errors = jsonSchemaValidator.validate("submission", submission);

    assertThat(errors).isEmpty();
  }

  @Test
  void validateNoErrorsForClaimWithRequiredFields() {
    ClaimFields claim = getMinimumValidClaim();
    List<String> errors = jsonSchemaValidator.validate(CLAIM_SCHEMA, claim);
    assertThat(errors).isEmpty();
  }

  @Test
  void validateErrorsForInvalidLineNumber() {
    ClaimFields claim = getMinimumValidClaim();
    claim.setLineNumber(-2);
    List<String> errors = jsonSchemaValidator.validate(CLAIM_SCHEMA, claim);
    assertThat(errors)
        .containsExactlyInAnyOrder(
            "line_number: must have a minimum value of 1 (provided value: -2)");
  }

  @Test
  void validateErrorsForInvalidScheduleReferenceTooLong() {
    ClaimFields claim = getMinimumValidClaim();
    claim.setScheduleReference("ScheduleReferenceLongerThan20");
    List<String> errors = jsonSchemaValidator.validate(CLAIM_SCHEMA, claim);
    assertThat(errors)
        .containsExactlyInAnyOrder(
            "schedule_reference: must be at most 20 characters long (provided value: ScheduleReferenceLongerThan20)");
  }

  @Test
  void validateErrorsForInvalidScheduleReferenceContainingSpace() {
    ClaimFields claim = getMinimumValidClaim();
    claim.setScheduleReference("Schedule Reference 1");
    List<String> errors = jsonSchemaValidator.validate(CLAIM_SCHEMA, claim);
    assertThat(errors)
        .containsExactlyInAnyOrder(
            "schedule_reference: does not match the regex pattern ^[a-zA-Z0-9]+$ (provided value: Schedule Reference 1)");
  }

  @Test
  void validateValidScheduleReference() {
    ClaimFields claim = getMinimumValidClaim();
    claim.setScheduleReference("Valid123");
    List<String> errors = jsonSchemaValidator.validate(CLAIM_SCHEMA, claim);
    assertThat(errors).isEmpty();
  }

  @Test
  void validateErrorsForInvalidCaseReferenceTooLong() {
    ClaimFields claim = getMinimumValidClaim();
    claim.setCaseReferenceNumber("CaseReferenceNumberLongerThan30");
    List<String> errors = jsonSchemaValidator.validate(CLAIM_SCHEMA, claim);
    assertThat(errors)
        .containsExactlyInAnyOrder(
            "case_reference_number: must be at most 30 characters long (provided value: CaseReferenceNumberLongerThan30)");
  }

  @Test
  void validateInvalidCaseReferenceNumberWithSpace() {
    ClaimFields claim = getMinimumValidClaim();
    claim.setCaseReferenceNumber("Case Ref 123");
    List<String> errors = jsonSchemaValidator.validate(CLAIM_SCHEMA, claim);
    assertThat(errors)
        .containsExactly(
            "case_reference_number: does not match the regex pattern ^[a-zA-Z0-9]+$ (provided value: Case Ref 123)");
  }

  @Test
  void validateValidCaseReferenceNumber() {
    ClaimFields claim = getMinimumValidClaim();
    claim.setCaseReferenceNumber("CASE123");
    List<String> errors = jsonSchemaValidator.validate(CLAIM_SCHEMA, claim);
    assertThat(errors).isEmpty();
  }

  @Test
  void validateValidUniqueFileNumber() {
    ClaimFields claim = getMinimumValidClaim();
    claim.setUniqueFileNumber("010123/001");
    List<String> errors = jsonSchemaValidator.validate(CLAIM_SCHEMA, claim);
    assertThat(errors).isEmpty();
  }

  @Test
  void validateErrorsForInvalidUniqueFileNumberWrongFormat() {
    ClaimFields claim = getMinimumValidClaim();
    claim.setUniqueFileNumber("20250101-001"); // wrong format
    List<String> errors = jsonSchemaValidator.validate(CLAIM_SCHEMA, claim);
    assertThat(errors)
        .containsExactlyInAnyOrder(
            "unique_file_number: must be the constant value '/' (provided value: 20250101-001)",
            "unique_file_number: does not match the regex pattern ^[0-9]{6}/[0-9]{3}$ (provided value: 20250101-001)",
            "unique_file_number: must be at most 10 characters long (provided value: 20250101-001)");
  }

  @Test
  void validateNoErrorsForUniqueFileNumberWithJustASlash() {
    ClaimFields claim = getMinimumValidClaim();
    claim.setUniqueFileNumber("/"); // wrong format
    List<String> errors = jsonSchemaValidator.validate(CLAIM_SCHEMA, claim);
    assertThat(errors).isEmpty();
  }

  private static @NotNull ClaimFields getMinimumValidClaim() {
    ClaimFields claim = new ClaimFields();
    claim
        .lineNumber(1)
        .caseReferenceNumber("CaseReferenceNumber")
        .status(ClaimStatus.READY_TO_PROCESS)
        .scheduleReference("ScheduleReference")
        .caseStartDate("10/04/2020")
        .disbursementsVatAmount(BigDecimal.valueOf(10.20))
        .feeCode("FeeCode");
    return claim;
  }
}
