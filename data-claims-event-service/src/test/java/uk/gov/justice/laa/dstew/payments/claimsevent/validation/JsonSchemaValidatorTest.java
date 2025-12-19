package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.SchemaValidationConfig;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.StringCaseUtil;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.model.ValidationErrorMessage;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonSchemaValidatorTest {

  private static final String ERROR_MESSAGE =
      "must be a maximum of 20 characters and contain only letters, numbers and forward slashes'";
  private static final String LEGAL_HELP_SUBMISSION_REF_INVALID_MSG =
      "'Legal Help Submission Reference " + ERROR_MESSAGE;
  private static final String CRIME_LOWER_SCHEDULE_NUMBER_INVALID_MSG =
      "'Crime Lower Schedule Number " + ERROR_MESSAGE;
  private static final String MEDIATION_SUBMISSION_REF_INVALID_MSG =
      "'Mediation Submission Reference " + ERROR_MESSAGE;

  private JsonSchemaValidator jsonSchemaValidator;

  private SchemaValidationConfig schemaValidationConfig;

  private Map<String, Set<ValidationErrorMessage>> schemaValidationErrorMessages;

  @BeforeAll
  void setUp() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    schemaValidationConfig =
        new SchemaValidationConfig(
            mapper,
            new ClassPathResource("schemas/submission-fields.schema.json"),
            new ClassPathResource("schemas/claim-fields.schema.json"));
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);

    // Manually load schemas for testing
    schemaValidationErrorMessages = schemaValidationConfig.schemaValidationErrorMessages();
    jsonSchemaValidator =
        new JsonSchemaValidator(
            mapper, schemaValidationConfig.jsonSchemas(), schemaValidationErrorMessages);
  }

  @Nested
  @DisplayName("Submission Schema Validation Tests")
  class SubmissionValidationTests {

    @Test
    void validateNoErrorsForMinimumSubmission() {
      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate(
              "submission", getMinimumValidSubmission(), AreaOfLaw.LEGAL_HELP);
      assertThat(errors).isEmpty();
    }

    @ParameterizedTest(name = "Missing required field: {0}")
    @CsvSource({
      "office_account_number",
      "submission_period",
      "area_of_law",
      "status",
      "is_nil_submission",
      "number_of_claims"
    })
    void validateErrorForMissingRequiredFields(String jsonField) {
      Object submission = getMinimumValidSubmission();
      String fieldName = toCamelCase(jsonField);
      setField(submission, fieldName, null);
      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate("submission", submission, AreaOfLaw.LEGAL_HELP);
      assertThat(errors)
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .containsExactlyInAnyOrder(StringCaseUtil.toTitleCase(jsonField) + " is required");
    }

    @ParameterizedTest(name = "Missing {0} for area of law {1}")
    @CsvSource({
      "legal_help_submission_reference,LEGAL_HELP,Legal Help Submission Reference is required",
      "crime_lower_schedule_number,CRIME_LOWER,Crime Lower Schedule Number is required",
      "mediation_submission_reference,MEDIATION,Mediation Submission Reference is required"
    })
    void validateErrorForMissingSubmissionReferenceFields(
        String jsonField, String areaOfLaw, String expectedError) {
      SubmissionResponse submission = getMinimumValidSubmission();
      submission.setAreaOfLaw(AreaOfLaw.valueOf(areaOfLaw));
      String fieldName = toCamelCase(jsonField);
      setField(submission, fieldName, null);
      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate("submission", submission, AreaOfLaw.LEGAL_HELP);
      assertThat(errors)
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .containsExactlyInAnyOrder(expectedError);
    }

    @ParameterizedTest(name = "Field {0} set for area of law {1}")
    @CsvSource({
      "legal_help_submission_reference,LEGAL_HELP,1A123B/CIVIL",
      "crime_lower_schedule_number,CRIME_LOWER,CRM/1A123B/13",
      "mediation_submission_reference,MEDIATION,1A123B/MEDI2010/14"
    })
    void validateNoErrorWhenAValidValueIsSetForScheduleNum(
        String jsonField, String areaOfLaw, String scheduleNum) {
      SubmissionResponse submission = getMinimumValidSubmission();
      submission.setAreaOfLaw(AreaOfLaw.valueOf(areaOfLaw));
      String fieldName = toCamelCase(jsonField);
      setField(submission, fieldName, scheduleNum);
      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate("submission", submission, AreaOfLaw.LEGAL_HELP);
      assertThat(errors).isEmpty();
    }

    @ParameterizedTest(name = "Field {0} set for area of law {1} with invalid characters")
    @CsvSource({
      "legal_help_submission_reference,LEGAL_HELP,ABC01_SCH,"
          + LEGAL_HELP_SUBMISSION_REF_INVALID_MSG,
      "legal_help_submission_reference,LEGAL_HELP,ABC01?," + LEGAL_HELP_SUBMISSION_REF_INVALID_MSG,
      "legal_help_submission_reference,LEGAL_HELP,REALLYLONGREFERENCE1234567890,"
          + LEGAL_HELP_SUBMISSION_REF_INVALID_MSG,
      "crime_lower_schedule_number,CRIME_LOWER,ABC01_SCH,"
          + CRIME_LOWER_SCHEDULE_NUMBER_INVALID_MSG,
      "crime_lower_schedule_number,CRIME_LOWER,ABC01?," + CRIME_LOWER_SCHEDULE_NUMBER_INVALID_MSG,
      "crime_lower_schedule_number,CRIME_LOWER,REALLYLONGREFERENCE1234567890,"
          + CRIME_LOWER_SCHEDULE_NUMBER_INVALID_MSG,
      "mediation_submission_reference,MEDIATION,ABC01_SCH," + MEDIATION_SUBMISSION_REF_INVALID_MSG,
      "mediation_submission_reference,MEDIATION,ABC01?," + MEDIATION_SUBMISSION_REF_INVALID_MSG,
      "mediation_submission_reference,MEDIATION,REALLYLONGREFERENCE1234567890,"
          + MEDIATION_SUBMISSION_REF_INVALID_MSG
    })
    void validateErrorsForInvalidSubmissionReferenceFields(
        String jsonField, String areaOfLaw, String value, String expectedError) {
      SubmissionResponse submission = getMinimumValidSubmission();
      submission.setAreaOfLaw(AreaOfLaw.valueOf(areaOfLaw));
      String fieldName = toCamelCase(jsonField);
      setField(submission, fieldName, value);
      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate("submission", submission, AreaOfLaw.LEGAL_HELP);
      assertThat(errors)
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .containsExactlyInAnyOrder(expectedError);
    }

    @Test
    void validateReturnsMultipleErrorsForInvalidSubmission() {
      SubmissionResponse submission = new SubmissionResponse();
      submission.setOfficeAccountNumber("abc123");
      submission.setSubmissionPeriod("OCTOBER-2024");
      submission.setAreaOfLaw(null);
      submission.setNumberOfClaims(-1);
      submission.setCrimeLowerScheduleNumber("SCHEDULE");
      submission.setIsNilSubmission(false);
      submission.setStatus(SubmissionStatus.CREATED);
      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate("submission", submission, AreaOfLaw.LEGAL_HELP);

      assertThat(errors)
          .extracting(ValidationMessagePatch::getTechnicalMessage)
          .containsExactlyInAnyOrder(
              "office_account_number: does not match the regex pattern ^[A-Z0-9]{6}$ (provided "
                  + "value: abc123)",
              "submission_period: does not match the regex pattern ^"
                  + "(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)-[0-9]{4}$ (provided value:"
                  + " OCTOBER-2024)",
              "$: required property 'area_of_law' not found (provided value: null)",
              "number_of_claims: must have a minimum value of 0 (provided value: -1)");
      assertThat(errors)
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .containsExactlyInAnyOrder(
              "Office Account Number must be exactly 6 characters containing uppercase letters "
                  + "and numbers.",
              "Submission period wrong format, should be in the format MMM-YYYY",
              "Area of Law is required",
              "number_of_claims: must have a minimum value of 0 (provided value: -1)");
    }

    /**
     * Validates that an error is returned when a field in the JSON object contains an invalid data
     * type. The method performs validation against a JSON schema for a given field, using invalid
     * data types as input to confirm that the correct error messages are returned. A parameterized
     * test is used to evaluate multiple fields and invalid data combinations.
     *
     * @param fieldName The name of the JSON field to be tested (as found in the json schema, i.e.
     *     snake_case).
     * @param badJsonValue The invalid data value to be set for the field (as a JSON-formatted
     *     string).
     * @param technicalMessage The expected validation error message for the field with the invalid
     *     value.
     * @throws Exception if an error occurs during JSON parsing or validation.
     */
    @ParameterizedTest(name = "Invalid type for field {0} should return error")
    @CsvSource({
      // integer fields
      "status, '\"SNAFU\"', 'status: does not have a value in the enumeration [\"CREATED\", "
          + "\"READY_FOR_VALIDATION\", \"VALIDATION_IN_PROGRESS\", \"VALIDATION_SUCCEEDED\", "
          + "\"VALIDATION_FAILED\", \"REPLACED\"] (provided value: SNAFU)',"
          + "'Status must be one of: CREATED, READY_FOR_VALIDATION, VALIDATION_IN_PROGRESS, "
          + "VALIDATION_SUCCEEDED, VALIDATION_FAILED, or REPLACED'",
    })
    void validateSubmissionForInvalidDataTypes(
        String fieldName, String badJsonValue, String technicalMessage, String displayMessage)
        throws Exception {
      List<ValidationMessagePatch> errors =
          validateForInvalidDataTypes(
              "submission", getMinimumValidSubmission(), fieldName, badJsonValue);
      assertThat(errors)
          .extracting(ValidationMessagePatch::getTechnicalMessage)
          .contains(technicalMessage);
      assertThat(errors)
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .contains(displayMessage);
    }

    @ParameterizedTest(name = "Invalid value for {0} should return error")
    @CsvSource({
      "officeAccountNumber, abc123, 'office_account_number: does not match the regex pattern "
          + "^[A-Z0-9]{6}$ (provided value: abc123)'",
      "numberOfClaims, -10, 'number_of_claims: must have a minimum value of 0 (provided value: "
          + "-10)'",
      "submissionPeriod, 'OCTOBER-20', 'submission_period: does not match the regex pattern "
          + "^(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)-[0-9]{4}$ (provided value: "
          + "OCTOBER-20)'",
      "submissionPeriod, 'OCT-20', 'submission_period: does not match the regex pattern "
          + "^(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)-[0-9]{4}$ (provided value: "
          + "OCT-20)'",
      "submissionPeriod, 'OCT/2020', 'submission_period: does not match the regex pattern "
          + "^(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)-[0-9]{4}$ (provided value: "
          + "OCT/2020)'",
      "crimeLowerScheduleNumber, 'A/VERY/LONG/SCHEDULE/NUMBER', 'crime_lower_schedule_number: "
          + "must be at most 20 characters long (provided value: A/VERY/LONG/SCHEDULE/NUMBER)'",
    })
    void validateSubmissionIndividualInvalidField(
        String fieldName, String badValue, String technicalMessage) {
      SubmissionResponse submission = getMinimumValidSubmission();
      setField(submission, fieldName, badValue);
      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate("submission", submission, AreaOfLaw.LEGAL_HELP);
      assertThat(errors)
          .extracting(ValidationMessagePatch::getTechnicalMessage)
          .contains(technicalMessage);
      String displayMessage =
          getDisplayMessage(convertCamelCaseToSnakeCase(fieldName), technicalMessage);
      assertThat(errors)
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .contains(displayMessage);
    }

    @Test
    void validateReturnsEmptyListForValidSubmission() {
      SubmissionResponse submission = new SubmissionResponse();
      submission.setSubmissionId(UUID.randomUUID());
      submission.setBulkSubmissionId(UUID.randomUUID());
      submission.setStatus(SubmissionStatus.CREATED);
      submission.setCrimeLowerScheduleNumber("abc123");
      submission.setOfficeAccountNumber("2Q286D");
      submission.setSubmissionPeriod("OCT-2024");
      submission.setAreaOfLaw(AreaOfLaw.CRIME_LOWER);
      submission.isNilSubmission(false);
      submission.setNumberOfClaims(3);

      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate("submission", submission, AreaOfLaw.LEGAL_HELP);

      assertThat(errors).isEmpty();
    }
  }

  private String getDisplayMessage(String fieldName, String technicalMessage) {
    return getDisplayMessage(fieldName, technicalMessage, AreaOfLaw.LEGAL_HELP);
  }

  private String getDisplayMessage(String fieldName, String technicalMessage, AreaOfLaw areaOfLaw) {
    return Optional.ofNullable(schemaValidationErrorMessages.get(fieldName))
        .orElse(new HashSet<>())
        .stream()
        .filter(
            validationErrorMessage ->
                Objects.equals(validationErrorMessage.key(), "ALL")
                    || Objects.equals(validationErrorMessage.key(), areaOfLaw.getValue()))
        .map(ValidationErrorMessage::value)
        .findFirst()
        .orElse(technicalMessage);
  }

  @Nested
  @DisplayName("Claim Schema Validation Tests")
  class ClaimValidationTests {

    @Test
    void validateNoErrorsForClaimWithRequiredFields() {
      ClaimResponse claim = getMinimumValidClaim();
      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate("claim", claim, AreaOfLaw.LEGAL_HELP);
      assertThat(errors).isEmpty();
    }

    @ParameterizedTest(name = "Missing required field: {0}")
    @CsvSource({
      "status",
      "line_number",
      "disbursements_vat_amount",
      "net_disbursement_amount",
      "fee_code",
    })
    void validateErrorForMissingRequiredClaimResponse(String jsonField) {
      Object claim = getMinimumValidClaim();
      String fieldName = toCamelCase(jsonField);
      setField(claim, fieldName, null);
      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate("claim", claim, AreaOfLaw.LEGAL_HELP);
      assertThat(errors)
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .containsExactlyInAnyOrder(StringCaseUtil.toTitleCase(jsonField) + " is required");
    }

    @Test
    void validateNoErrorForMissingCaseStartDateInClaimResponse() {
      Object claim = getMinimumValidClaim();
      String fieldName = toCamelCase("case_start_date");
      setField(claim, fieldName, null);
      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate("claim", claim, AreaOfLaw.LEGAL_HELP);
      assertThat(errors).isEmpty();
    }

    static Stream<Arguments> areaOfLawAndCodes() {
      return Stream.of(null, "")
          .flatMap(
              code ->
                  Stream.of(AreaOfLaw.values()).map(areaOfLaw -> Arguments.of(areaOfLaw, code)));
    }

    @ParameterizedTest(name = "AreaOfLaw={0}, code=\"{1}\" should produce no errors")
    @MethodSource("areaOfLawAndCodes")
    void validateNoErrorForNullOrEmptyCrimeMatterTypeCode(
        AreaOfLaw areaOfLaw, String matterTypeCode) {
      Object claim = getMinimumValidClaim();
      String fieldName = toCamelCase("crime_matter_type_code");
      setField(claim, fieldName, matterTypeCode);
      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate("claim", claim, areaOfLaw);
      assertThat(errors).isEmpty();
    }

    /**
     * Validates that an error is returned when a field in the JSON object contains an invalid data
     * type. The method performs validation against a JSON schema for a given field, using invalid
     * data types as input to confirm that the correct error messages are returned. A parameterized
     * test is used to evaluate multiple fields and invalid data combinations.
     *
     * @param fieldName The name of the JSON field to be tested (as found in the json schema, i.e.
     *     snake_case).
     * @param badJsonValue The invalid data value to be set for the field (as a JSON-formatted
     *     string).
     * @param technicalError The expected validation error message for the field with the invalid
     *     value.
     * @throws Exception if an error occurs during JSON parsing or validation.
     */
    @ParameterizedTest(name = "Invalid type for field {0} should return error")
    @CsvFileSource(resources = "/testData/invalidFieldReturnError.csv")
    void validateClaimForInvalidDataTypes(
        String fieldName, String badJsonValue, String technicalError) throws Exception {
      List<ValidationMessagePatch> errors =
          validateForInvalidDataTypes("claim", getMinimumValidClaim(), fieldName, badJsonValue);
      assertThat(errors)
          .extracting(ValidationMessagePatch::getTechnicalMessage)
          .anyMatch(msg -> msg.contains(technicalError));
    }

    /**
     * Validates individual fields of a claim to ensure they meet the required validation rules. The
     * method is parameterized to test various scenarios with invalid field values.
     *
     * @param fieldName The name of the claim field being validated.
     * @param badValue The invalid value assigned to the claim field.
     * @param technicalError The expected error message corresponding to the invalid field value.
     */
    @ParameterizedTest(name = "Invalid value {1} for {0} should return error")
    @CsvFileSource(resources = "/testData/validateClaimIndividualInvalidFieldLegalHelp.csv")
    void validateClaimIndividualInvalidFieldLegalHelp(
        String fieldName, String badValue, String technicalError) {
      ClaimResponse claim = getMinimumValidClaim();
      setField(claim, fieldName, badValue);
      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate("claim", claim, AreaOfLaw.LEGAL_HELP);

      String displayMessage =
          getDisplayMessage(
              convertCamelCaseToSnakeCase(fieldName), technicalError, AreaOfLaw.LEGAL_HELP);
      assertThat(errors)
          .extracting(ValidationMessagePatch::getTechnicalMessage)
          .map("'%s'"::formatted)
          .anyMatch(
              msg -> msg.contains(technicalError),
              "Technical message didn't contain expected text: %s".formatted(technicalError));
      assertThat(errors)
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .anyMatch(
              msg -> msg.contains(displayMessage),
              "Display message didn't contain expected text: %s".formatted(displayMessage));
    }

    @ParameterizedTest(name = "Invalid value {1} for {0} should return error")
    @CsvFileSource(resources = "/testData/validateClaimIndividualInvalidFieldCrimeLower.csv")
    void validateClaimIndividualInvalidFieldCrimeLower(
        String fieldName, String badValue, String technicalError) {
      ClaimResponse claim = getMinimumValidClaim();
      setField(claim, fieldName, badValue);
      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate("claim", claim, AreaOfLaw.CRIME_LOWER);

      String displayMessage =
          getDisplayMessage(
              convertCamelCaseToSnakeCase(fieldName), technicalError, AreaOfLaw.CRIME_LOWER);
      assertThat(errors)
          .extracting(ValidationMessagePatch::getTechnicalMessage)
          .map("'%s'"::formatted)
          .anyMatch(
              msg -> msg.contains(technicalError),
              "Technical message didn't contain expected text: %s".formatted(technicalError));
      assertThat(errors)
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .anyMatch(
              msg -> msg.contains(displayMessage),
              "Display message didn't contain expected text: %s".formatted(displayMessage));
    }

    /**
     * Validates individual fields of a claim to ensure they meet the required validation rules. The
     * method is parameterized to test various scenarios with invalid field values.
     *
     * @param fieldName The name of the claim field being validated.
     * @param badValue The invalid value assigned to the claim field.
     * @param technicalError The expected error message corresponding to the invalid field value.
     */
    @ParameterizedTest(name = "Invalid value {1} for {0} should return error")
    @CsvFileSource(resources = "/testData/validateClaimIndividualInvalidFieldMediation.csv")
    void validateClaimIndividualInvalidFieldMediation(
        String fieldName, String badValue, String technicalError) {
      ClaimResponse claim = getMinimumValidClaim();
      setField(claim, fieldName, badValue);
      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate("claim", claim, AreaOfLaw.MEDIATION);

      String displayMessage =
          getDisplayMessage(
              convertCamelCaseToSnakeCase(fieldName), technicalError, AreaOfLaw.MEDIATION);
      assertThat(errors)
          .extracting(ValidationMessagePatch::getTechnicalMessage)
          .map("'%s'"::formatted)
          .anyMatch(
              msg -> msg.contains(technicalError),
              "Technical message didn't contain expected text: %s".formatted(technicalError));
      assertThat(errors)
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .anyMatch(
              msg -> msg.contains(displayMessage),
              "Display message didn't contain expected text: %s".formatted(displayMessage));
    }

    @ParameterizedTest(name = "Invalid value {1} for {0} should return error")
    @CsvSource({
      "uniqueClientNumber, '$£%^&*(', LEGAL HELP, 'Unique Client Number must be in the format DDMMYYYY/X/ZZZZ with valid date, and be a maximum of 15 characters'",
      "client2Ucn, '12121999-A-ABCD', LEGAL HELP, Client 2 Unique Client Number must be in the format DDMMYYYY/X/ZZZZ with valid date, and be a maximum of 15 characters'",
      "caseReferenceNumber, 'Case:Ref:123', LEGAL HELP, Case Reference Number must contain only letters, numbers, forward slashes, periods, hyphens, and spaces, and be a maximum of 30 characters",
      "uniqueFileNumber, '010123-001', LEGAL HELP, Unique File Number (UFN) must be in the format DDMMYY/NNN with a date in the past",
      "caseStartDate, '1899-12-31', LEGAL HELP, Case Start Date must be a valid date in the format DD/MM/YYYY",
      "caseConcludedDate, '1899-12-31', LEGAL HELP, Case Concluded Date must be a valid date in the format DD/MM/YYYY",
      "crimeMatterTypeCode, '123', CRIME LOWER, Crime Lower Matter Type Code must be one of the permitted values. Please refer to the guidance.",
      "feeCode, 'ABC_90', LEGAL HELP, Fee Code must contain only letters and numbers, and be a maximum of 10 characters",
      "procurementAreaCode, 'aa543', LEGAL HELP, Procurement Area Code must be 2 uppercase letters followed by 5 digits",
      "accessPointCode, 'ap12543', LEGAL HELP, Access Point Code must be in the format AP##### (AP followed by 5 digits)",
      "deliveryLocation, 'ap12543', LEGAL HELP, Delivery Location must be 2 uppercase letters followed by 5 digits",
      "representationOrderDate, '10/01/12', LEGAL HELP, Representation Order Date must be a valid date in the format DD/MM/YYYY",
      "suspectsDefendantsCount, '121', LEGAL HELP, Suspects Defendants Count must be between 0 and 99",
      "policeStationCourtAttendancesCount, '121', LEGAL HELP, Police Station Court Attendances Count must be between 0 and 99",
      "policeStationCourtPrisonId, '121', LEGAL HELP, Police Station Court Prison ID must be 1–6 alphanumeric characters and contain at least one letter",
      "dsccNumber, '121', LEGAL HELP, DSCC Number must be exactly 10 alphanumeric characters",
      "maatId, 'fdsdfs&121', LEGAL HELP, MAAT ID must be up to 10 alphanumeric characters",
      "prisonLawPriorApprovalNumber, 'fd121', LEGAL HELP, Prison Law Prior Approval Number must be exactly 10 alphanumeric characters",
      "schemeId, 'AB', LEGAL HELP, Scheme ID must be exactly 4 alphanumeric characters",
      "mediationSessionsCount, 122, LEGAL HELP, Mediation Sessions Count must be between 1 and 99",
      "mediationTimeMinutes, 9999999, LEGAL HELP, Mediation Time Minutes must be between 0 and 99999",
      "outreachLocation, ABCD5, LEGAL HELP, Outreach Location must be exactly 3 alphanumeric characters",
      "referralSource, 100, LEGAL HELP, Referral Source must be a valid 2-digit code (02-11)",
      "clientForename, 'Anthony, Gonsalves', LEGAL HELP, Client Forename must contain only letters, numbers, spaces, hyphens, apostrophes, ampersands, and be a maximum of 30 characters",
      "client2Forename, 'Anthony, Gonsalves', LEGAL HELP, Client 2 Forename must contain only letters, numbers, spaces, hyphens, apostrophes, ampersands, and be a maximum of 30 characters",
      "clientSurname, 'Anthony, Gonsalves', LEGAL HELP, Client Surname must contain only letters, numbers, spaces, hyphens, apostrophes, ampersands, and be a maximum of 30 characters",
      "client2Surname, 'Anthony, Gonsalves', LEGAL HELP, Client 2 Surname must contain only letters, numbers, spaces, hyphens, apostrophes, ampersands, and be a maximum of 30 characters",
      "clientDateOfBirth, 01/09/08, LEGAL HELP, Client Date of Birth must be a valid date in the format DD/MM/YYYY",
      "clientPostcode, ABCD, LEGAL HELP, Client Postcode must be a valid UK postcode or NFA",
      "client2Postcode, ABCD, LEGAL HELP, Client 2 Postcode must be a valid UK postcode or NFA",
      "genderCode, Z, LEGAL HELP, Gender code must be valid",
      "client2GenderCode, Z, LEGAL HELP, Client 2 Gender code must be valid",
      "ethnicityCode, 17, LEGAL HELP, Ethnicity Code must be valid",
      "client2EthnicityCode, 17, LEGAL HELP, Client 2 Ethnicity Code must be valid",
      "disabilityCode, ABC, LEGAL HELP, Disability Code must be valid",
      "client2DisabilityCode, ABC, LEGAL HELP, Client 2 Disability Code must be valid",
      "clientTypeCode, MF, LEGAL HELP, Client Type Code must be valid",
      "homeOfficeClientNumber, abc def, LEGAL HELP, Home Office Client Number must contain only letters and numbers, and be a maximum of 16 characters",
      "claReferenceNumber, ABC123, LEGAL HELP, CLA Reference Number must be between 1 and 7 digits",
      "claExemptionCode, ABCDE, LEGAL HELP, CLA Exemption Code must be exactly 4 characters",
      "client2DateOfBirth, 01/09/09, LEGAL HELP, Client 2 Date of Birth must be a valid date in the format DD/MM/YYYY",
      "transferDate, 01/09/09, LEGAL HELP, Transfer Date must be a valid date in the format DD/MM/YYYY",
      "caseId, ABCD, LEGAL HELP, Case ID must be exactly 3 digits",
      "uniqueCaseId, '  ', LEGAL HELP, Unique Case ID must contain at least one non-whitespace character",
      "caseStageCode, ABCDE, LEGAL HELP, Case Stage/Level Code must be valid",
      "stageReachedCode, ABCDE, LEGAL HELP, Stage Reached Code must be exactly 2 alphanumeric characters for Legal Help claims",
      "stageReachedCode, ABCDE, CRIME LOWER, Stage Reached Code must be exactly 4 uppercase letters for Crime Lower claims",
      "standardFeeCategoryCode, XYZ, LEGAL HELP, Standard Fee Category Code must be valid",
      "designatedAccreditedRepresentativeCode, 6, LEGAL HELP, Designated Accredited Representative Code must be valid",
      "mentalHealthTribunalReference, AB/123/1234, LEGAL HELP, Mental Health Tribunal Reference must be in format XX/YYYY/YYYY or XXYYYZZZ",
      "followOnWork, AB, LEGAL HELP, Follow On Work must be a single character",
      "exemptionCriteriaSatisfied, ab120, LEGAL HELP, Exemption Criteria Satisfied must be 2 uppercase letters followed by 3 digits",
      "exceptionalCaseFundingReference, 1234567ABX, LEGAL HELP, Exceptional Case Funding Reference must be 7 digits followed by 2 uppercase letters",
      "adviceTime, 99999999, LEGAL HELP, Advice Time must be in minutes",
      "travelTime, 99999999, LEGAL HELP, Travel Time must be in minutes",
      "waitingTime, 99999999, LEGAL HELP, Waiting Time must be in minutes",
      "netProfitCostsAmount, 1000000000, LEGAL HELP, Net Profit Costs Amount must be a valid monetary value",
      "netDisbursementAmount, 1000000000, LEGAL HELP, Net Disbursement Amount must be a valid monetary value",
      "netCounselCostsAmount, 1000000000, LEGAL HELP, Net Counsel Costs Amount must be a valid monetary value",
      "disbursementsVatAmount, 10.011, LEGAL HELP, Disbursements VAT Amount must be a valid monetary value",
      "travelWaitingCostsAmount, 10000, LEGAL HELP, Travel Waiting Costs Amount must be a valid monetary value",
      "netWaitingCostsAmount, 10.011, LEGAL HELP, Net Waiting Costs Amount must be a valid monetary value",
      "priorAuthorityReference, ABC$123, LEGAL HELP, Prior Authority Reference must be exactly 7 alphanumeric characters",
      "adjournedHearingFeeAmount, 10, LEGAL HELP, Adjourned Hearing Fee Amount must be between 0 and 9",
      "costsDamagesRecoveredAmount, 100000, LEGAL HELP, Costs Damages Recovered Amount must be a valid monetary value",
      "meetingsAttendedCode, MTGA25, LEGAL HELP, Meetings Attended Code must be valid",
      "detentionTravelWaitingCostsAmount, 10.011, LEGAL HELP, Detention Travel Waiting Costs Amount must be a valid monetary value",
      "jrFormFillingAmount, 10.011, LEGAL HELP, JR Form Filling Amount must be a valid monetary value",
      "adviceTypeCode, ABC, LEGAL HELP, Advice Type Code must be valid",
      "medicalReportsCount, 11, LEGAL HELP, Medical Reports Count must be between 0 and 10",
      "surgeryClientsCount, 21, LEGAL HELP, Surgery Clients Count must be between 1 and 20",
      "surgeryMattersCount, 21, LEGAL HELP, Surgery Matters Count must be between 1 and 20",
      "cmrhOralCount, 10, LEGAL HELP, CMRH Oral Count must be between 0 and 9",
      "cmrhTelephoneCount, 10, LEGAL HELP, CMRH Telephone Count must be between 0 and 9",
      "aitHearingCentreCode, 99, LEGAL HELP, AIT Hearing Centre Code must be valid",
      "hoInterview, 12, LEGAL HELP, HO Interview must be between 0 and 9",
      "localAuthorityNumber, ABC$, LEGAL HELP, Local Authority Number must contain only letters and numbers, and be a maximum of 30 characters"
    })
    void validateClaimIndividualInvalidFieldDisplayMessage(
        String fieldName, String badValue, String areaOfLaw, String displayErrorMessage) {
      ClaimResponse claim = getMinimumValidClaim();
      setField(claim, fieldName, badValue);
      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate("claim", claim, AreaOfLaw.fromValue(areaOfLaw));
      assertThat(errors)
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .map("'%s'"::formatted)
          .anyMatch(
              msg -> msg.contains(displayErrorMessage),
              "Display message didn't contain expected text: %s".formatted(displayErrorMessage));
    }

    /**
     * Validates that providing a valid value for the specified field of a claim does not produce
     * any validation errors.
     *
     * @param fieldName The name of the field in the claim object to validate.
     * @param badValue A valid value to test for the specified field to ensure there are no
     *     validation errors.
     */
    @ParameterizedTest(name = "Valid value for {0} should NOT return error")
    @CsvFileSource(resources = "/testData/validateClaimIndividualValidField.csv")
    void validateClaimIndividualValidField(String fieldName, String badValue) {
      ClaimResponse claim = getMinimumValidClaim();
      setField(claim, fieldName, badValue);
      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate("claim", claim, AreaOfLaw.LEGAL_HELP);
      assertThat(errors).isEmpty();
    }

    @DisplayName("should create only one ValidationMessagePatch for unique display message")
    @Test
    void shouldCreateOneValidationMessagePatch() {
      var claim = getMinimumValidClaim();
      setField(claim, "ethnicityCode", "999999");
      final List<ValidationMessagePatch> errors =
          jsonSchemaValidator.validate("claim", claim, AreaOfLaw.LEGAL_HELP);

      assertThat(errors).hasSize(1);

      assertThat(errors.getFirst().getTechnicalMessage())
          .isEqualTo(
              "ethnicity_code: does not match the regex pattern ^(0[0-9]|1[0-6]|99)$ (provided "
                  + "value: 999999)"
                  + " : ethnicity_code: must be at most 2 characters long (provided value: "
                  + "999999)");
    }
  }

  private static @NotNull SubmissionResponse getMinimumValidSubmission() {
    SubmissionResponse submission = new SubmissionResponse();
    submission
        .submissionId(UUID.randomUUID())
        .bulkSubmissionId(UUID.randomUUID())
        .officeAccountNumber("2Q286D")
        .submissionPeriod("OCT-2024")
        .areaOfLaw(AreaOfLaw.CRIME_LOWER)
        .status(SubmissionStatus.CREATED)
        .isNilSubmission(false)
        .numberOfClaims(1)
        .crimeLowerScheduleNumber("SCHEDULE/NUMBER/1");
    return submission;
  }

  private static @NotNull ClaimResponse getMinimumValidClaim() {
    ClaimResponse claim = new ClaimResponse();
    claim
        .lineNumber(1)
        .status(ClaimStatus.READY_TO_PROCESS)
        .scheduleReference("ScheduleReference")
        .caseStartDate("2020-04-10")
        .netDisbursementAmount(BigDecimal.valueOf(20.10))
        .disbursementsVatAmount(BigDecimal.valueOf(10.20))
        .isVatApplicable(true)
        .feeCode("FeeCode")
        .caseStageCode("MHL10")
        .courtLocationCode("ABCDE")
        .exemptionCriteriaSatisfied("CM001")
        .meetingsAttendedCode("MTGA24");
    return claim;
  }

  private void setField(Object target, String fieldName, String rawValue) {
    try {
      PropertyDescriptor pd = new PropertyDescriptor(fieldName, target.getClass());
      Method setter = pd.getWriteMethod();
      Class<?> paramType = setter.getParameterTypes()[0];

      Object value = null;
      if (rawValue != null) {
        if (paramType.equals(Integer.class)) {
          value = Integer.valueOf(rawValue);
        } else if (paramType.equals(BigDecimal.class)) {
          value = new BigDecimal(rawValue);
        } else if (paramType.equals(Boolean.class)) {
          value = Boolean.valueOf(rawValue);
        } else if (paramType.isEnum()) {
          @SuppressWarnings({"rawtypes", "unchecked"})
          Class<? extends Enum> enumType = (Class<? extends Enum>) paramType;
          value = Enum.valueOf(enumType, rawValue);
        } else {
          value = rawValue; // default: String
        }
      }
      setter.invoke(target, value);
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to set field '" + fieldName + "' on object " + target.getClass(), e);
    }
  }

  private List<ValidationMessagePatch> validateForInvalidDataTypes(
      String schemaName, Object baseValidObject, String fieldName, String badJsonValue)
      throws Exception {

    ObjectMapper mapper =
        new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);

    JsonNode validNode = mapper.valueToTree(baseValidObject);
    ObjectNode node = validNode.deepCopy();
    node.set(fieldName, mapper.readTree(badJsonValue));

    return jsonSchemaValidator.validate(schemaName, node, AreaOfLaw.LEGAL_HELP);
  }

  // Utility to convert snake_case to camelCase
  private static String toCamelCase(String snakeCase) {
    StringBuilder sb = new StringBuilder();
    boolean nextUpper = false;
    for (char c : snakeCase.toCharArray()) {
      if (c == '_') {
        nextUpper = true;
      } else {
        sb.append(nextUpper ? Character.toUpperCase(c) : c);
        nextUpper = false;
      }
    }
    return sb.toString();
  }

  public String convertCamelCaseToSnakeCase(String value) {
    return value
        .replaceAll("([a-z])([A-Z])", "$1_$2")
        .replaceAll("([A-Za-z])([0-9])", "$1_$2")
        .replaceAll("([0-9])([A-Za-z])", "$1_$2")
        .toLowerCase();
  }
}
