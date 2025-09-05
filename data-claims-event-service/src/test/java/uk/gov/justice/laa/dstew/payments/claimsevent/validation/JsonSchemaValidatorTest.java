package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimFields;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionFields;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonSchemaValidatorTest {

  public static final String CLAIM_SCHEMA = "claim";

  private JsonSchemaValidator jsonSchemaValidator;

  @BeforeAll
  void setUp() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    // Manually load schemas for testing
    Map<String, JsonSchema> schemas =
        Map.of(
            "submission", loadSchema("schemas/submission-fields.schema.json", mapper),
            "claim", loadSchema("schemas/claim-fields.schema.json", mapper));
    jsonSchemaValidator = new JsonSchemaValidator(mapper, schemas);
  }

  private JsonSchema loadSchema(String path, ObjectMapper mapper) throws Exception {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
      if (in == null) throw new IllegalArgumentException("Schema not found: " + path);
      JsonNode schemaNode = mapper.readTree(in);
      return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaNode);
    }
  }

  @Nested
  @DisplayName("Submission Schema Validation Tests")
  class SubmissionValidationTests {

    @Test
    void validateNoErrorsForMinimumSubmission() {
      List<String> errors = jsonSchemaValidator.validate("submission", getMinimumValidSubmission());
      assertThat(errors).isEmpty();
    }

    @ParameterizedTest(name = "Missing required field: {0}")
    @CsvSource({
        "office_account_number",
        "submission_period",
        "area_of_law",
        "number_of_claims"
    })
    void validateErrorForMissingRequiredFields(String jsonField) {
      Object submission = getMinimumValidSubmission();
      String fieldName = toCamelCase(jsonField);
      setField(submission, fieldName, null);
      List<String> errors = jsonSchemaValidator.validate("submission", submission);
      assertThat(errors)
          .containsExactlyInAnyOrder(
              "$: required property '" + jsonField + "' not found (provided value: null)"
          );
    }

    @Test
    void validateReturnsMultipleErrorsForInvalidSubmission() {
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

    @ParameterizedTest(name = "Invalid value for {0} should return error")
    @CsvSource({
        "officeAccountNumber, abc123, 'office_account_number: does not match the regex pattern ^[A-Z0-9]{6}$ (provided value: abc123)'",
        "numberOfClaims, -10, 'number_of_claims: must have a minimum value of 0 (provided value: -10)'",
        "areaOfLaw, 'WILD WEST', 'area_of_law: does not have a value in the enumeration "
            + "[\"CIVIL\", \"CRIME\", \"MEDIATION\", \"CRIME LOWER\", \"LEGAL HELP\"] (provided value: WILD WEST)'",
        "submissionPeriod, 'OCTOBER-20', 'submission_period: does not match the regex pattern "
            + "^(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)-[0-9]{4}$ (provided value: OCTOBER-20)'",
        "submissionPeriod, 'OCT-20', 'submission_period: does not match the regex pattern "
            + "^(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)-[0-9]{4}$ (provided value: OCT-20)'",
        "submissionPeriod, 'OCT/2020', 'submission_period: does not match the regex pattern "
            + "^(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)-[0-9]{4}$ (provided value: OCT/2020)'",
    })
    void validateSubmissionIndividualInvalidField(String fieldName, String badValue,
        String expectedError) {
      SubmissionFields submission = getMinimumValidSubmission();
      setField(submission, fieldName, badValue);
      List<String> errors = jsonSchemaValidator.validate("submission", submission);
      assertThat(errors).contains(expectedError);
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

  }

  @Nested
  @DisplayName("Claim Schema Validation Tests")
  class ClaimValidationTests {

    @Test
    void validateNoErrorsForClaimWithRequiredFields() {
      ClaimFields claim = getMinimumValidClaim();
      List<String> errors = jsonSchemaValidator.validate(CLAIM_SCHEMA, claim);
      assertThat(errors).isEmpty();
    }

    @ParameterizedTest(name = "Missing required field: {0}")
    @CsvSource({
        "status",
        "schedule_reference",
        "line_number",
        "case_reference_number",
        "disbursements_vat_amount",
        "fee_code",
    })
    void validateErrorForMissingRequiredClaimFields(String jsonField) {
      Object claim = getMinimumValidClaim();
      String fieldName = toCamelCase(jsonField);
      setField(claim, fieldName, null);
      List<String> errors = jsonSchemaValidator.validate("claim", claim);
      assertThat(errors)
          .containsExactlyInAnyOrder(
              "$: required property '" + jsonField + "' not found (provided value: null)"
          );
    }

    @ParameterizedTest(name = "Invalid type for field {0} should return error")
    @CsvSource({
        "line_number, '\"abc\"', 'line_number: string found, integer expected (provided value: abc)'",
        "line_number, 2.3, 'line_number: number found, integer expected (provided value: 2.3)'",
        "disbursements_vat_amount, '\"oops\"', 'disbursements_vat_amount: string found, number expected (provided value: oops)'",
    })
    void validateErrorForInvalidDataTypes(String fieldName, String badJsonValue, String expectedError) throws Exception {
      ObjectMapper mapper = new ObjectMapper()
          .setSerializationInclusion(JsonInclude.Include.NON_NULL);
      JsonNode validNode = mapper.valueToTree(getMinimumValidClaim());
      ObjectNode node = validNode.deepCopy();
      node.set(fieldName, mapper.readTree(badJsonValue));
      List<String> errors = jsonSchemaValidator.validate("claim", node);
      assertThat(errors).contains(expectedError);
    }

    @ParameterizedTest(name = "Invalid value for {0} should return error")
    @CsvSource({
        "lineNumber, -2, 'line_number: must have a minimum value of 1 (provided value: -2)'",
        "lineNumber, 0, 'line_number: must have a minimum value of 1 (provided value: 0)'",
        "scheduleReference, 'ScheduleReferenceLongerThan20', 'schedule_reference: must be at most 20 characters long (provided value: ScheduleReferenceLongerThan20)'",
        "scheduleReference, 'Schedule Reference 1', 'schedule_reference: does not match the regex pattern ^[a-zA-Z0-9]+$ (provided value: Schedule Reference 1)'",
        "scheduleReference, 'Schedule/Reference', 'schedule_reference: does not match the regex pattern ^[a-zA-Z0-9]+$ (provided value: Schedule/Reference)'",
        "caseReferenceNumber, 'CaseReferenceNumberLongerThan30', 'case_reference_number: must be at most 30 characters long (provided value: CaseReferenceNumberLongerThan30)'",
        "caseReferenceNumber, 'Case Ref 123', 'case_reference_number: does not match the regex pattern ^[a-zA-Z0-9]+$ (provided value: Case Ref 123)'",
        "caseReferenceNumber, 'Case/Ref/123', 'case_reference_number: does not match the regex pattern ^[a-zA-Z0-9]+$ (provided value: Case/Ref/123)'",
        "uniqueFileNumber, '010123-001', 'unique_file_number: does not match the regex pattern ^[0-9]{6}/[0-9]{3}$ (provided value: 010123-001)'",
        "uniqueFileNumber, '20250101/001', 'unique_file_number: does not match the regex pattern ^[0-9]{6}/[0-9]{3}$ (provided value: 20250101/001)'",
    })
    void validateClaimIndividualInvalidField(String fieldName, String badValue, String expectedError) {
      ClaimFields claim = getMinimumValidClaim();
      setField(claim, fieldName, badValue);
      List<String> errors = jsonSchemaValidator.validate("claim", claim);
      assertThat(errors).contains(expectedError);
    }

    @ParameterizedTest(name = "Valid value for {0} should NOT return error")
    @CsvSource({
        "lineNumber, 2",
        "lineNumber, 2000",
        "scheduleReference, 'ShortNSweet'",
        "scheduleReference, 'Valid123'",
        "caseReferenceNumber, 'CASE123'",
        "uniqueFileNumber, '/'",
        "uniqueFileNumber, '010123/001'",
    })
    void validateClaimIndividualValidField(String fieldName, String badValue) {
      ClaimFields claim = getMinimumValidClaim();
      setField(claim, fieldName, badValue);
      List<String> errors = jsonSchemaValidator.validate("claim", claim);
      assertThat(errors).isEmpty();
    }
  }

  private static @NotNull SubmissionFields getMinimumValidSubmission() {
    SubmissionFields submission = new SubmissionFields();
    submission.submissionId(UUID.randomUUID())
        .bulkSubmissionId(UUID.randomUUID())
        .officeAccountNumber("2Q286D")
        .submissionPeriod("OCT-2024")
        .areaOfLaw("CRIME")
        .numberOfClaims(1);
    return submission;
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

  private <T> void setField(T target, String fieldName, Object value) {
    try {
      // Build the expected setter method name: e.g., "areaOfLaw" → "setAreaOfLaw"
      String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

      // Find a setter method that matches the name (ignore parameter type for now)
      Method setter = Arrays.stream(target.getClass().getMethods())
          .filter(m -> m.getName().equals(setterName) && m.getParameterCount() == 1)
          .findFirst()
          .orElseThrow(() -> new NoSuchMethodException(
              "Setter not found for field: " + fieldName));

      // Determine parameter type
      Class<?> paramType = setter.getParameterTypes()[0];

      // Convert value if necessary
      Object convertedValue = convertValue(value, paramType);

      // Invoke setter
      setter.invoke(target, convertedValue);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field '" + fieldName + "' on object " + target, e);
    }
  }

  /** Converts value to the required type if possible, otherwise returns null */
  private Object convertValue(Object value, Class<?> targetType) {
    if (value == null) {
      return null;
    }
    if (targetType.isAssignableFrom(value.getClass())) {
      return value;
    }
    if (targetType == Integer.class || targetType == int.class) {
      return Integer.valueOf(value.toString());
    }
    if (targetType == Long.class || targetType == long.class) {
      return Long.valueOf(value.toString());
    }
    if (targetType == Boolean.class || targetType == boolean.class) {
      return Boolean.valueOf(value.toString());
    }
    if (targetType == String.class) {
      return value.toString();
    }
    throw new IllegalArgumentException("Cannot convert " + value + " to " + targetType);
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
}
