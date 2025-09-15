package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonSchemaValidatorTest {

  public static final String CLAIM_SCHEMA = "claim";

  private JsonSchemaValidator jsonSchemaValidator;

  @BeforeAll
  void setUp() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);

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
      "status",
      "schedule_number",
      "is_nil_submission",
      "number_of_claims"
    })
    void validateErrorForMissingRequiredFields(String jsonField) {
      Object submission = getMinimumValidSubmission();
      String fieldName = toCamelCase(jsonField);
      setField(submission, fieldName, null);
      List<String> errors = jsonSchemaValidator.validate("submission", submission);
      assertThat(errors)
          .containsExactlyInAnyOrder(
              "$: required property '" + jsonField + "' not found (provided value: null)");
    }

    @Test
    void validateReturnsMultipleErrorsForInvalidSubmission() {
      SubmissionResponse submission = new SubmissionResponse();
      submission.setOfficeAccountNumber("abc123");
      submission.setSubmissionPeriod("OCTOBER-2024");
      submission.setAreaOfLaw("INVALID");
      submission.setNumberOfClaims(-1);
      submission.setScheduleNumber("SCHEDULE");
      submission.setIsNilSubmission(false);
      submission.setStatus(SubmissionStatus.CREATED);
      List<String> errors = jsonSchemaValidator.validate("submission", submission);

      assertThat(errors)
          .containsExactlyInAnyOrder(
              "office_account_number: does not match the regex pattern ^[A-Z0-9]{6}$ (provided value: abc123)",
              "submission_period: does not match the regex pattern ^(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)-[0-9]{4}$ (provided value: OCTOBER-2024)",
              "area_of_law: does not have a value in the enumeration [\"CIVIL\", \"CRIME\", \"MEDIATION\", \"CRIME LOWER\", \"LEGAL HELP\"] (provided value: INVALID)",
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
     * @param expectedError The expected validation error message for the field with the invalid
     *     value.
     * @throws Exception if an error occurs during JSON parsing or validation.
     */
    @ParameterizedTest(name = "Invalid type for field {0} should return error")
    @CsvSource({
      // integer fields
      "status, '\"SNAFU\"', 'status: does not have a value in the enumeration [\"CREATED\", \"READY_FOR_VALIDATION\", \"VALIDATION_IN_PROGRESS\", \"VALIDATION_SUCCEEDED\", \"VALIDATION_FAILED\", \"REPLACED\"] (provided value: SNAFU)'",
    })
    void validateSubmissionForInvalidDataTypes(
        String fieldName, String badJsonValue, String expectedError) throws Exception {
      List<String> errors =
          validateForInvalidDataTypes(
              "submission", getMinimumValidSubmission(), fieldName, badJsonValue);
      assertThat(errors).contains(expectedError);
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
      "scheduleNumber, 'A/VERY/LONG/SCHEDULE/NUMBER', 'schedule_number: must be at most 20 characters long (provided value: A/VERY/LONG/SCHEDULE/NUMBER)'",
    })
    void validateSubmissionIndividualInvalidField(
        String fieldName, String badValue, String expectedError) {
      SubmissionResponse submission = getMinimumValidSubmission();
      setField(submission, fieldName, badValue);
      List<String> errors = jsonSchemaValidator.validate("submission", submission);
      assertThat(errors).contains(expectedError);
    }

    @Test
    void validateReturnsEmptyListForValidSubmission() {
      SubmissionResponse submission = new SubmissionResponse();
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
      ClaimResponse claim = getMinimumValidClaim();
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
      "net_disbursement_amount",
      "is_vat_applicable",
      "fee_code",
    })
    void validateErrorForMissingRequiredClaimResponse(String jsonField) {
      Object claim = getMinimumValidClaim();
      String fieldName = toCamelCase(jsonField);
      setField(claim, fieldName, null);
      List<String> errors = jsonSchemaValidator.validate("claim", claim);
      assertThat(errors)
          .containsExactlyInAnyOrder(
              "$: required property '" + jsonField + "' not found (provided value: null)");
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
     * @param expectedError The expected validation error message for the field with the invalid
     *     value.
     * @throws Exception if an error occurs during JSON parsing or validation.
     */
    @ParameterizedTest(name = "Invalid type for field {0} should return error")
    @CsvSource({
      // integer fields
      "line_number, '\"abc\"', 'line_number: string found, integer expected (provided value: abc)'",
      "line_number, 2.3, 'line_number: number found, integer expected (provided value: 2.3)'",
      "advice_time, '\"abc\"', 'advice_time: string found, integer expected (provided value: abc)'",
      "advice_time, 2.3, 'advice_time: number found, integer expected (provided value: 2.3)'",
      "travel_time, '\"abc\"', 'travel_time: string found, integer expected (provided value: abc)'",
      "travel_time, 2.3, 'travel_time: number found, integer expected (provided value: 2.3)'",
      "waiting_time, '\"abc\"', 'waiting_time: string found, integer expected (provided value: abc)'",
      "waiting_time, 2.3, 'waiting_time: number found, integer expected (provided value: 2.3)'",
      "suspects_defendants_count, '\"abc\"', 'suspects_defendants_count: string found, integer expected (provided value: abc)'",
      "suspects_defendants_count, 2.3, 'suspects_defendants_count: number found, integer expected (provided value: 2.3)'",
      "police_station_court_attendances_count, '\"abc\"', 'police_station_court_attendances_count: string found, integer expected (provided value: abc)'",
      "police_station_court_attendances_count, 2.3, 'police_station_court_attendances_count: number found, integer expected (provided value: 2.3)'",
      "mediation_sessions_count, '\"abc\"', 'mediation_sessions_count: string found, integer expected (provided value: abc)'",
      "mediation_sessions_count, 2.3, 'mediation_sessions_count: number found, integer expected (provided value: 2.3)'",
      "mediation_time_minutes, '\"abc\"', 'mediation_time_minutes: string found, integer expected (provided value: abc)'",
      "mediation_time_minutes, 2.3, 'mediation_time_minutes: number found, integer expected (provided value: 2.3)'",
      "advice_time, '\"abc\"', 'advice_time: string found, integer expected (provided value: abc)'",
      "advice_time, true, 'advice_time: boolean found, integer expected (provided value: true)'",
      "travel_time, '\"abc\"', 'travel_time: string found, integer expected (provided value: abc)'",
      "travel_time, true, 'travel_time: boolean found, integer expected (provided value: true)'",
      "waiting_time, '\"abc\"', 'waiting_time: string found, integer expected (provided value: abc)'",
      "waiting_time, false, 'waiting_time: boolean found, integer expected (provided value: false)'",
      "adjourned_hearing_fee_amount, '\"abc\"', 'adjourned_hearing_fee_amount: string found, integer expected (provided value: abc)'",
      "adjourned_hearing_fee_amount, true, 'adjourned_hearing_fee_amount: boolean found, integer expected (provided value: true)'",
      "medical_reports_count, '\"abc\"', 'medical_reports_count: string found, integer expected (provided value: abc)'",
      "medical_reports_count, true, 'medical_reports_count: boolean found, integer expected (provided value: true)'",
      "surgery_clients_count, '\"abc\"', 'surgery_clients_count: string found, integer expected (provided value: abc)'",
      "surgery_clients_count, false, 'surgery_clients_count: boolean found, integer expected (provided value: false)'",
      "surgery_matters_count, '\"abc\"', 'surgery_matters_count: string found, integer expected (provided value: abc)'",
      "surgery_matters_count, true, 'surgery_matters_count: boolean found, integer expected (provided value: true)'",
      "cmrh_oral_count, '\"abc\"', 'cmrh_oral_count: string found, integer expected (provided value: abc)'",
      "cmrh_oral_count, false, 'cmrh_oral_count: boolean found, integer expected (provided value: false)'",
      "cmrh_telephone_count, '\"abc\"', 'cmrh_telephone_count: string found, integer expected (provided value: abc)'",
      "cmrh_telephone_count, true, 'cmrh_telephone_count: boolean found, integer expected (provided value: true)'",
      "ho_interview, '\"abc\"', 'ho_interview: string found, integer expected (provided value: abc)'",
      "ho_interview, true, 'ho_interview: boolean found, integer expected (provided value: true)'",
      // number fields
      "disbursements_vat_amount, '\"abc\"', 'disbursements_vat_amount: string found, number expected (provided value: abc)'",
      "disbursements_vat_amount, 'true', 'disbursements_vat_amount: boolean found, number expected (provided value: true)'",
      "net_profit_costs_amount, '\"abc\"', 'net_profit_costs_amount: string found, number expected (provided value: abc)'",
      "net_profit_costs_amount, 'true', 'net_profit_costs_amount: boolean found, number expected (provided value: true)'",
      "net_disbursement_amount, '\"abc\"', 'net_disbursement_amount: string found, number expected (provided value: abc)'",
      "net_disbursement_amount, false, 'net_disbursement_amount: boolean found, number expected (provided value: false)'",
      "net_counsel_costs_amount, '\"abc\"', 'net_counsel_costs_amount: string found, number expected (provided value: abc)'",
      "net_counsel_costs_amount, 'true', 'net_counsel_costs_amount: boolean found, number expected (provided value: true)'",
      "disbursements_vat_amount, '\"abc\"', 'disbursements_vat_amount: string found, number expected (provided value: abc)'",
      "disbursements_vat_amount, false, 'disbursements_vat_amount: boolean found, number expected (provided value: false)'",
      "travel_waiting_costs_amount, '\"abc\"', 'travel_waiting_costs_amount: string found, number expected (provided value: abc)'",
      "travel_waiting_costs_amount, 'true', 'travel_waiting_costs_amount: boolean found, number expected (provided value: true)'",
      "net_waiting_costs_amount, '\"abc\"', 'net_waiting_costs_amount: string found, number expected (provided value: abc)'",
      "net_waiting_costs_amount, false, 'net_waiting_costs_amount: boolean found, number expected (provided value: false)'",
      "costs_damages_recovered_amount, '\"abc\"', 'costs_damages_recovered_amount: string found, number expected (provided value: abc)'",
      "costs_damages_recovered_amount, false, 'costs_damages_recovered_amount: boolean found, number expected (provided value: false)'",
      "detention_travel_waiting_costs_amount, '\"abc\"', 'detention_travel_waiting_costs_amount: string found, number expected (provided value: abc)'",
      "detention_travel_waiting_costs_amount, 'true', 'detention_travel_waiting_costs_amount: boolean found, number expected (provided value: true)'",
      "jr_form_filling_amount, '\"abc\"', 'jr_form_filling_amount: string found, number expected (provided value: abc)'",
      "jr_form_filling_amount, false, 'jr_form_filling_amount: boolean found, number expected (provided value: false)'",
      // boolean fields
      "is_duty_solicitor, '\"yes\"', 'is_duty_solicitor: string found, boolean expected (provided value: yes)'",
      "is_duty_solicitor, '\"no\"', 'is_duty_solicitor: string found, boolean expected (provided value: no)'",
      "is_duty_solicitor, 1, 'is_duty_solicitor: integer found, boolean expected (provided value: 1)'",
      "is_duty_solicitor, 0, 'is_duty_solicitor: integer found, boolean expected (provided value: 0)'",
      "is_youth_court, '\"yes\"', 'is_youth_court: string found, boolean expected (provided value: yes)'",
      "is_youth_court, '\"no\"', 'is_youth_court: string found, boolean expected (provided value: no)'",
      "is_youth_court, 1, 'is_youth_court: integer found, boolean expected (provided value: 1)'",
      "is_youth_court, 0, 'is_youth_court: integer found, boolean expected (provided value: 0)'",
      "is_legally_aided, '\"yes\"', 'is_legally_aided: string found, boolean expected (provided value: yes)'",
      "is_legally_aided, '\"no\"', 'is_legally_aided: string found, boolean expected (provided value: no)'",
      "is_legally_aided, 1, 'is_legally_aided: integer found, boolean expected (provided value: 1)'",
      "is_legally_aided, 0, 'is_legally_aided: integer found, boolean expected (provided value: 0)'",
      "is_postal_application_accepted, '\"yes\"', 'is_postal_application_accepted: string found, boolean expected (provided value: yes)'",
      "is_postal_application_accepted, 1, 'is_postal_application_accepted: integer found, boolean expected (provided value: 1)'",
      "is_client_2_postal_application_accepted, '\"no\"', 'is_client_2_postal_application_accepted: string found, boolean expected (provided value: no)'",
      "is_client_2_postal_application_accepted, 0, 'is_client_2_postal_application_accepted: integer found, boolean expected (provided value: 0)'",
      "is_nrm_advice, '\"yes\"', 'is_nrm_advice: string found, boolean expected (provided value: yes)'",
      "is_nrm_advice, 1, 'is_nrm_advice: integer found, boolean expected (provided value: 1)'",
      "is_legacy_case, '\"yes\"', 'is_legacy_case: string found, boolean expected (provided value: yes)'",
      "is_legacy_case, 1, 'is_legacy_case: integer found, boolean expected (provided value: 1)'",
      "is_vat_applicable, '\"yes\"', 'is_vat_applicable: string found, boolean expected (provided value: yes)'",
      "is_vat_applicable, 1, 'is_vat_applicable: integer found, boolean expected (provided value: 1)'",
      "is_tolerance_applicable, '\"yes\"', 'is_tolerance_applicable: string found, boolean expected (provided value: yes)'",
      "is_tolerance_applicable, 0, 'is_tolerance_applicable: integer found, boolean expected (provided value: 0)'",
      "is_london_rate, '\"yes\"', 'is_london_rate: string found, boolean expected (provided value: yes)'",
      "is_london_rate, 1, 'is_london_rate: integer found, boolean expected (provided value: 1)'",
      "is_additional_travel_payment, '\"yes\"', 'is_additional_travel_payment: string found, boolean expected (provided value: yes)'",
      "is_additional_travel_payment, 1, 'is_additional_travel_payment: integer found, boolean expected (provided value: 1)'",
      "is_eligible_client, '\"yes\"', 'is_eligible_client: string found, boolean expected (provided value: yes)'",
      "is_eligible_client, 1, 'is_eligible_client: integer found, boolean expected (provided value: 1)'",
      "is_irc_surgery, '\"yes\"', 'is_irc_surgery: string found, boolean expected (provided value: yes)'",
      "is_irc_surgery, 1, 'is_irc_surgery: integer found, boolean expected (provided value: 1)'",
      "is_substantive_hearing, '\"yes\"', 'is_substantive_hearing: string found, boolean expected (provided value: yes)'",
      "is_substantive_hearing, 1, 'is_substantive_hearing: integer found, boolean expected (provided value: 1)'",
      // string fields
      "schedule_reference, 123, 'schedule_reference: integer found, string expected (provided value: 123)'",
      "schedule_reference, true, 'schedule_reference: boolean found, string expected (provided value: true)'",
      "case_reference_number, 123, 'case_reference_number: integer found, string expected (provided value: 123)'",
      "case_reference_number, true, 'case_reference_number: boolean found, string expected (provided value: true)'",
      "procurement_area_code, 123, 'procurement_area_code: integer found, string expected (provided value: 123)'",
      "procurement_area_code, true, 'procurement_area_code: boolean found, string expected (provided value: true)'",
      "crime_matter_type_code, 123, 'crime_matter_type_code: integer found, string expected (provided value: 123)'",
      "crime_matter_type_code, true, 'crime_matter_type_code: boolean found, string expected (provided value: true)'",
      "access_point_code, 123, 'access_point_code: integer found, string expected (provided value: 123)'",
      "access_point_code, true, 'access_point_code: boolean found, string expected (provided value: true)'",
      "delivery_location, 123, 'delivery_location: integer found, string expected (provided value: 123)'",
      "delivery_location, true, 'delivery_location: boolean found, string expected (provided value: true)'",
      "unique_file_number, 123, 'unique_file_number: integer found, string expected (provided value: 123)'",
      "unique_file_number, true, 'unique_file_number: boolean found, string expected (provided value: true)'",
      "police_station_court_prison_id, 123, 'police_station_court_prison_id: integer found, string expected (provided value: 123)'",
      "police_station_court_prison_id, true, 'police_station_court_prison_id: boolean found, string expected (provided value: true)'",
      "dscc_number, 123, 'dscc_number: integer found, string expected (provided value: 123)'",
      "dscc_number, true, 'dscc_number: boolean found, string expected (provided value: true)'",
      "maat_id, 123, 'maat_id: integer found, string expected (provided value: 123)'",
      "maat_id, true, 'maat_id: boolean found, string expected (provided value: true)'",
      "prison_law_prior_approval_number, 123, 'prison_law_prior_approval_number: integer found, string expected (provided value: 123)'",
      "prison_law_prior_approval_number, true, 'prison_law_prior_approval_number: boolean found, string expected (provided value: true)'",
      "scheme_id, 123, 'scheme_id: integer found, string expected (provided value: 123)'",
      "scheme_id, true, 'scheme_id: boolean found, string expected (provided value: true)'",
      "outreach_location, 123, 'outreach_location: integer found, string expected (provided value: 123)'",
      "outreach_location, true, 'outreach_location: boolean found, string expected (provided value: true)'",
      "referral_source, 10, 'referral_source: integer found, string expected (provided value: 10)'",
      "referral_source, true, 'referral_source: boolean found, string expected (provided value: true)'",
      "client_forename, 10, 'client_forename: integer found, string expected (provided value: 10)'",
      "client_forename, true, 'client_forename: boolean found, string expected (provided value: true)'",
      "client_surname, 10, 'client_surname: integer found, string expected (provided value: 10)'",
      "client_surname, true, 'client_surname: boolean found, string expected (provided value: true)'",
      "client_2_forename, 10, 'client_2_forename: integer found, string expected (provided value: 10)'",
      "client_2_forename, true, 'client_2_forename: boolean found, string expected (provided value: true)'",
      "client_2_surname, 10, 'client_2_surname: integer found, string expected (provided value: 10)'",
      "client_2_surname, true, 'client_2_surname: boolean found, string expected (provided value: true)'",
      "unique_client_number, 10, 'unique_client_number: integer found, string expected (provided value: 10)'",
      "unique_client_number, true, 'unique_client_number: boolean found, string expected (provided value: true)'",
      "client_postcode, 10, 'client_postcode: integer found, string expected (provided value: 10)'",
      "client_postcode, true, 'client_postcode: boolean found, string expected (provided value: true)'",
      "client_2_postcode, 10, 'client_2_postcode: integer found, string expected (provided value: 10)'",
      "client_2_postcode, true, 'client_2_postcode: boolean found, string expected (provided value: true)'",
      "gender_code, 12345, 'gender_code: integer found, string expected (provided value: 12345)'",
      "gender_code, true, 'gender_code: boolean found, string expected (provided value: true)'",
      "ethnicity_code, 12345, 'ethnicity_code: integer found, string expected (provided value: 12345)'",
      "ethnicity_code, true, 'ethnicity_code: boolean found, string expected (provided value: true)'",
      "disability_code, 12345, 'disability_code: integer found, string expected (provided value: 12345)'",
      "disability_code, true, 'disability_code: boolean found, string expected (provided value: true)'",
      "client_type_code, 12345, 'client_type_code: integer found, string expected (provided value: 12345)'",
      "client_type_code, true, 'client_type_code: boolean found, string expected (provided value: true)'",
      "home_office_client_number, 12345, 'home_office_client_number: integer found, string expected (provided value: 12345)'",
      "home_office_client_number, true, 'home_office_client_number: boolean found, string expected (provided value: true)'",
      "cla_reference_number, 12345, 'cla_reference_number: integer found, string expected (provided value: 12345)'",
      "cla_reference_number, false, 'cla_reference_number: boolean found, string expected (provided value: false)'",
      "cla_exemption_code, 1234, 'cla_exemption_code: integer found, string expected (provided value: 1234)'",
      "cla_exemption_code, true, 'cla_exemption_code: boolean found, string expected (provided value: true)'",
      "case_id, 123, 'case_id: integer found, string expected (provided value: 123)'",
      "case_id, false, 'case_id: boolean found, string expected (provided value: false)'",
      "unique_case_id, 12345, 'unique_case_id: integer found, string expected (provided value: 12345)'",
      "unique_case_id, true, 'unique_case_id: boolean found, string expected (provided value: true)'",
      "case_stage_code, 12345, 'case_stage_code: integer found, string expected (provided value: 12345)'",
      "case_stage_code, false, 'case_stage_code: boolean found, string expected (provided value: false)'",
      "stage_reached_code, 12, 'stage_reached_code: integer found, string expected (provided value: 12)'",
      "stage_reached_code, true, 'stage_reached_code: boolean found, string expected (provided value: true)'",
      "standard_fee_category_code, 1, 'standard_fee_category_code: integer found, string expected (provided value: 1)'",
      "standard_fee_category_code, true, 'standard_fee_category_code: boolean found, string expected (provided value: true)'",
      "outcome_code, 12, 'outcome_code: integer found, string expected (provided value: 12)'",
      "outcome_code, true, 'outcome_code: boolean found, string expected (provided value: true)'",
      "designated_accredited_representative_code, 3, 'designated_accredited_representative_code: integer found, string expected (provided value: 3)'",
      "designated_accredited_representative_code, false, 'designated_accredited_representative_code: boolean found, string expected (provided value: false)'",
      "mental_health_tribunal_reference, 12345678, 'mental_health_tribunal_reference: integer found, string expected (provided value: 12345678)'",
      "mental_health_tribunal_reference, true, 'mental_health_tribunal_reference: boolean found, string expected (provided value: true)'",
      "follow_on_work, 1, 'follow_on_work: integer found, string expected (provided value: 1)'",
      "follow_on_work, true, 'follow_on_work: boolean found, string expected (provided value: true)'",
      "exemption_criteria_satisfied, 1, 'exemption_criteria_satisfied: integer found, string expected (provided value: 1)'",
      "exemption_criteria_satisfied, true, 'exemption_criteria_satisfied: boolean found, string expected (provided value: true)'",
      "exceptional_case_funding_reference, 1234567, 'exceptional_case_funding_reference: integer found, string expected (provided value: 1234567)'",
      "exceptional_case_funding_reference, false, 'exceptional_case_funding_reference: boolean found, string expected (provided value: false)'",
      "prior_authority_reference, 1234567, 'prior_authority_reference: integer found, string expected (provided value: 1234567)'",
      "prior_authority_reference, true, 'prior_authority_reference: boolean found, string expected (provided value: true)'",
      "meetings_attended_code, 1, 'meetings_attended_code: integer found, string expected (provided value: 1)'",
      "meetings_attended_code, true, 'meetings_attended_code: boolean found, string expected (provided value: true)'",
      "court_location_code, 123, 'court_location_code: integer found, string expected (provided value: 123)'",
      "court_location_code, true, 'court_location_code: boolean found, string expected (provided value: true)'",
      "advice_type_code, 1, 'advice_type_code: integer found, string expected (provided value: 1)'",
      "advice_type_code, true, 'advice_type_code: boolean found, string expected (provided value: true)'",
      "surgery_date, 12345, 'surgery_date: integer found, string expected (provided value: 12345)'",
      "surgery_date, true, 'surgery_date: boolean found, string expected (provided value: true)'",
      "ait_hearing_centre_code, 1, 'ait_hearing_centre_code: integer found, string expected (provided value: 1)'",
      "ait_hearing_centre_code, false, 'ait_hearing_centre_code: boolean found, string expected (provided value: false)'",
      "local_authority_number, 12345, 'local_authority_number: integer found, string expected (provided value: 12345)'",
      "local_authority_number, false, 'local_authority_number: boolean found, string expected (provided value: false)'",
      // date fields (typed as string with format)
      "case_start_date, 12345, 'case_start_date: integer found, string expected (provided value: 12345)'",
      "case_start_date, true, 'case_start_date: boolean found, string expected (provided value: true)'",
      "representation_order_date, 12345, 'representation_order_date: integer found, string expected (provided value: 12345)'",
      "representation_order_date, true, 'representation_order_date: boolean found, string expected (provided value: true)'",
      "client_date_of_birth, 12345, 'client_date_of_birth: integer found, string expected (provided value: 12345)'",
      "client_date_of_birth, true, 'client_date_of_birth: boolean found, string expected (provided value: true)'",
      "client_2_date_of_birth, 12345, 'client_2_date_of_birth: integer found, string expected (provided value: 12345)'",
      "client_2_date_of_birth, true, 'client_2_date_of_birth: boolean found, string expected (provided value: true)'",
    })
    void validateClaimForInvalidDataTypes(
        String fieldName, String badJsonValue, String expectedError) throws Exception {
      List<String> errors =
          validateForInvalidDataTypes("claim", getMinimumValidClaim(), fieldName, badJsonValue);
      assertThat(errors).contains(expectedError);
    }

    /**
     * Validates individual fields of a claim to ensure they meet the required validation rules. The
     * method is parameterized to test various scenarios with invalid field values.
     *
     * @param fieldName The name of the claim field being validated.
     * @param badValue The invalid value assigned to the claim field.
     * @param expectedError The expected error message corresponding to the invalid field value.
     */
    @ParameterizedTest(name = "Invalid value for {0} should return error")
    @CsvSource({
      // -------- Integers with minimum/maximum --------
      "lineNumber, -1, 'line_number: must have a minimum value of 1 (provided value: -1)'",
      "lineNumber, 0, 'line_number: must have a minimum value of 1 (provided value: 0)'",
      "adviceTime, -1, 'advice_time: must have a minimum value of 0 (provided value: -1)'",
      "travelTime, -1, 'travel_time: must have a minimum value of 0 (provided value: -1)'",
      "waitingTime, -1, 'waiting_time: must have a minimum value of 0 (provided value: -1)'",
      "adviceTime, 100000, 'advice_time: must have a maximum value of 99999 (provided value: 100000)'",
      "travelTime, 100000, 'travel_time: must have a maximum value of 99999 (provided value: 100000)'",
      "waitingTime, 100000, 'waiting_time: must have a maximum value of 99999 (provided value: 100000)'",
      "suspectsDefendantsCount, -1, 'suspects_defendants_count: must have a minimum value of 0 (provided value: -1)'",
      "suspectsDefendantsCount, 100, 'suspects_defendants_count: must have a maximum value of 99 (provided value: 100)'",
      "policeStationCourtAttendancesCount, -1, 'police_station_court_attendances_count: must have a minimum value of 0 (provided value: -1)'",
      "policeStationCourtAttendancesCount, 100, 'police_station_court_attendances_count: must have a maximum value of 99 (provided value: 100)'",
      "mediationSessionsCount, 0, 'mediation_sessions_count: must have a minimum value of 1 (provided value: 0)'",
      "mediationSessionsCount, 100, 'mediation_sessions_count: must have a maximum value of 99 (provided value: 100)'",
      "mediationTimeMinutes, -1, 'mediation_time_minutes: must have a minimum value of 0 (provided value: -1)'",
      "mediationTimeMinutes, 100000, 'mediation_time_minutes: must have a maximum value of 99999 (provided value: 100000)'",
      "medicalReportsCount, -1, 'medical_reports_count: must have a minimum value of 0 (provided value: -1)'",
      "medicalReportsCount, 11, 'medical_reports_count: must have a maximum value of 10 (provided value: 11)'",
      "surgeryClientsCount, 0, 'surgery_clients_count: must have a minimum value of 1 (provided value: 0)'",
      "surgeryClientsCount, 21, 'surgery_clients_count: must have a maximum value of 20 (provided value: 21)'",
      "surgeryMattersCount, 0, 'surgery_matters_count: must have a minimum value of 1 (provided value: 0)'",
      "surgeryMattersCount, 25, 'surgery_matters_count: must have a maximum value of 20 (provided value: 25)'",
      "cmrhOralCount, -1, 'cmrh_oral_count: must have a minimum value of 0 (provided value: -1)'",
      "cmrhOralCount, 10, 'cmrh_oral_count: must have a maximum value of 9 (provided value: 10)'",
      "cmrhTelephoneCount, -1, 'cmrh_telephone_count: must have a minimum value of 0 (provided value: -1)'",
      "cmrhTelephoneCount, 15, 'cmrh_telephone_count: must have a maximum value of 9 (provided value: 15)'",
      "hoInterview, -1, 'ho_interview: must have a minimum value of 0 (provided value: -1)'",
      "hoInterview, 12, 'ho_interview: must have a maximum value of 9 (provided value: 12)'",
      // -------- Numbers (decimals) --------
      "disbursementsVatAmount, -0.01, 'disbursements_vat_amount: must have a minimum value of 0.0 (provided value: -0.01)'",
      "netProfitCostsAmount, -0.01, 'net_profit_costs_amount: must have a minimum value of 0.0 (provided value: -0.01)'",
      "netProfitCostsAmount, 1000000000, 'net_profit_costs_amount: must have a maximum value of 9.9999999999E8 (provided value: 1E+9)'",
      "netCounselCostsAmount, -0.01, 'net_counsel_costs_amount: must have a minimum value of 0.0 (provided value: -0.01)'",
      "netCounselCostsAmount, 100000, 'net_counsel_costs_amount: must have a maximum value of 99999.99 (provided value: 1E+5)'",
      "travelWaitingCostsAmount, -0.01, 'travel_waiting_costs_amount: must have a minimum value of 0.0 (provided value: -0.01)'",
      "travelWaitingCostsAmount, 10000, 'travel_waiting_costs_amount: must have a maximum value of 9999.99 (provided value: 1E+4)'",
      "netDisbursementAmount, -1, 'net_disbursement_amount: must have a minimum value of 0.0 (provided value: -1)'",
      "netDisbursementAmount, 1000000000, 'net_disbursement_amount: must have a maximum value of 9.9999999999E8 (provided value: 1E+9)'",
      "disbursementsVatAmount, -1, 'disbursements_vat_amount: must have a minimum value of 0.0 (provided value: -1)'",
      "disbursementsVatAmount, 100000, 'disbursements_vat_amount: must have a maximum value of 99999.99 (provided value: 1E+5)'",
      "netWaitingCostsAmount, -1, 'net_waiting_costs_amount: must have a minimum value of 0.0 (provided value: -1)'",
      "netWaitingCostsAmount, 1000000, 'net_waiting_costs_amount: must have a maximum value of 999999.99 (provided value: 1E+6)'",
      "adjournedHearingFeeAmount, -1, 'adjourned_hearing_fee_amount: must have a minimum value of 0 (provided value: -1)'",
      "adjournedHearingFeeAmount, 10, 'adjourned_hearing_fee_amount: must have a maximum value of 9 (provided value: 10)'",
      "costsDamagesRecoveredAmount, -1, 'costs_damages_recovered_amount: must have a minimum value of 0.0 (provided value: -1)'",
      "costsDamagesRecoveredAmount, 100000, 'costs_damages_recovered_amount: must have a maximum value of 99999.99 (provided value: 1E+5)'",
      "detentionTravelWaitingCostsAmount, -1, 'detention_travel_waiting_costs_amount: must have a minimum value of 0.0 (provided value: -1)'",
      "detentionTravelWaitingCostsAmount, 100000000, 'detention_travel_waiting_costs_amount: must have a maximum value of 9.999999999E7 (provided value: 1E+8)'",
      "jrFormFillingAmount, -1, 'jr_form_filling_amount: must have a minimum value of 0.0 (provided value: -1)'",
      "jrFormFillingAmount, 10000, 'jr_form_filling_amount: must have a maximum value of 9999.99 (provided value: 1E+4)'",
      // -------- String maxLength / pattern --------
      "scheduleReference, 'ABCDEFGHIJKLMNOPQRSTU', 'schedule_reference: must be at most 20 characters long (provided value: ABCDEFGHIJKLMNOPQRSTU)'",
      "scheduleReference, 'Schedule Ref', 'schedule_reference: does not match the regex pattern ^[a-zA-Z0-9/]+$ (provided value: Schedule Ref)'",
      "scheduleReference, 'Schedule:Ref', 'schedule_reference: does not match the regex pattern ^[a-zA-Z0-9/]+$ (provided value: Schedule:Ref)'",
      "caseReferenceNumber, \"ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890X\", 'case_reference_number: must be at most 30 characters long (provided value: \"ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890X\")'",
      "caseReferenceNumber, 'Case Ref 1', 'case_reference_number: does not match the regex pattern ^[a-zA-Z0-9/]+$ (provided value: Case Ref 1)'",
      "caseReferenceNumber, 'Case:Ref:123', 'case_reference_number: does not match the regex pattern ^[a-zA-Z0-9/]+$ (provided value: Case:Ref:123)'",
      "procurementAreaCode, 'Area/Code/123', 'procurement_area_code: does not match the regex pattern ^[A-Z]{2}[0-9]{5}$ (provided value: Area/Code/123)'",
      "crimeMatterTypeCode, 'AB', 'crime_matter_type_code: does not match the regex pattern ^[0-9][0-9]$ (provided value: AB)'",
      "crimeMatterTypeCode, '123', 'crime_matter_type_code: does not match the regex pattern ^[0-9][0-9]$ (provided value: 123)'",
      "crimeMatterTypeCode, '3', 'crime_matter_type_code: does not match the regex pattern ^[0-9][0-9]$ (provided value: 3)'",
      "accessPointCode, 'AB12345', 'access_point_code: does not match the regex pattern ^AP[0-9]{5}$ (provided value: AB12345)'",
      "accessPointCode, 'AP1234', 'access_point_code: does not match the regex pattern ^AP[0-9]{5}$ (provided value: AP1234)'",
      "accessPointCode, 'AP123456', 'access_point_code: does not match the regex pattern ^AP[0-9]{5}$ (provided value: AP123456)'",
      "accessPointCode, 'APP12345', 'access_point_code: does not match the regex pattern ^AP[0-9]{5}$ (provided value: APP12345)'",
      "deliveryLocation, 'APP12345', 'delivery_location: does not match the regex pattern ^[A-Z]{2}[0-9]{5}$ (provided value: APP12345)'",
      "deliveryLocation, 'XY1234', 'delivery_location: does not match the regex pattern ^[A-Z]{2}[0-9]{5}$ (provided value: XY1234)'",
      "deliveryLocation, 'XY123456', 'delivery_location: does not match the regex pattern ^[A-Z]{2}[0-9]{5}$ (provided value: XY123456)'",
      "uniqueFileNumber, '010123-001', 'unique_file_number: does not match the regex pattern ^[0-9]{6}/[0-9]{3}$ (provided value: 010123-001)'",
      "uniqueFileNumber, '20250101/001', 'unique_file_number: does not match the regex pattern ^[0-9]{6}/[0-9]{3}$ (provided value: 20250101/001)'",
      "policeStationCourtPrisonId, '20250101', 'police_station_court_prison_id: does not match the regex pattern ^[a-zA-Z0-9]{1,6}$ (provided value: 20250101)'",
      "policeStationCourtPrisonId, '', 'police_station_court_prison_id: does not match the regex pattern ^[a-zA-Z0-9]{1,6}$ (provided value: )'",
      "dsccNumber, '202101/001', 'dscc_number: does not match the regex pattern ^[a-zA-Z0-9]{10}$ (provided value: 202101/001)'",
      "dsccNumber, '202101A', 'dscc_number: does not match the regex pattern ^[a-zA-Z0-9]{10}$ (provided value: 202101A)'",
      "dsccNumber, '202101aTooLong', 'dscc_number: does not match the regex pattern ^[a-zA-Z0-9]{10}$ (provided value: 202101aTooLong)'",
      "maatId, '202101/001', 'maat_id: does not match the regex pattern ^[a-zA-Z0-9]{10}$ (provided value: 202101/001)'",
      "maatId, '202101A', 'maat_id: does not match the regex pattern ^[a-zA-Z0-9]{10}$ (provided value: 202101A)'",
      "maatId, '202101aTooLong', 'maat_id: does not match the regex pattern ^[a-zA-Z0-9]{10}$ (provided value: 202101aTooLong)'",
      "prisonLawPriorApprovalNumber, '202101/001', 'prison_law_prior_approval_number: does not match the regex pattern ^[a-zA-Z0-9]{10}$ (provided value: 202101/001)'",
      "prisonLawPriorApprovalNumber, '202101A', 'prison_law_prior_approval_number: does not match the regex pattern ^[a-zA-Z0-9]{10}$ (provided value: 202101A)'",
      "prisonLawPriorApprovalNumber, '202101aTooLong', 'prison_law_prior_approval_number: does not match the regex pattern ^[a-zA-Z0-9]{10}$ (provided value: 202101aTooLong)'",
      "schemeId, 'A/BC', 'scheme_id: does not match the regex pattern ^[a-zA-Z0-9]{4}$ (provided value: A/BC)'",
      "schemeId, 'AB', 'scheme_id: does not match the regex pattern ^[a-zA-Z0-9]{4}$ (provided value: AB)'",
      "schemeId, 'SchemeIdTooLong', 'scheme_id: does not match the regex pattern ^[a-zA-Z0-9]{4}$ (provided value: SchemeIdTooLong)'",
      "outreachLocation, 'A/B', 'outreach_location: does not match the regex pattern ^[a-zA-Z0-9]{3}$ (provided value: A/B)'",
      "outreachLocation, 'AB', 'outreach_location: does not match the regex pattern ^[a-zA-Z0-9]{3}$ (provided value: AB)'",
      "outreachLocation, 'TooLong', 'outreach_location: does not match the regex pattern ^[a-zA-Z0-9]{3}$ (provided value: TooLong)'",
      "referralSource, 'A/', 'referral_source: does not match the regex pattern ^(0[2-9]|1[0-1])$ (provided value: A/)'",
      "referralSource, 'AB', 'referral_source: does not match the regex pattern ^(0[2-9]|1[0-1])$ (provided value: AB)'",
      "referralSource, 'TooLong', 'referral_source: does not match the regex pattern ^(0[2-9]|1[0-1])$ (provided value: TooLong)'",
      "referralSource, '01', 'referral_source: does not match the regex pattern ^(0[2-9]|1[0-1])$ (provided value: 01)'",
      "referralSource, '12', 'referral_source: does not match the regex pattern ^(0[2-9]|1[0-1])$ (provided value: 12)'",
      "clientForename, 'Hello This is a name that is toooo long', 'client_forename: does not match the regex pattern ^[\\p{L}\\p{N}\\p{Zs}\\-’''&]{1,30}$ (provided value: Hello This is a name that is toooo long)'",
      "clientForename, 'Anthony, Gonsalves', 'client_forename: does not match the regex pattern ^[\\p{L}\\p{N}\\p{Zs}\\-’''&]{1,30}$ (provided value: Anthony, Gonsalves)'",
      "clientForename, '$£%^&*(', 'client_forename: does not match the regex pattern ^[\\p{L}\\p{N}\\p{Zs}\\-’''&]{1,30}$ (provided value: $£%^&*()'",
      "clientSurname, 'Hello This is a name that is toooo long', 'client_surname: does not match the regex pattern ^[\\p{L}\\p{N}\\p{Zs}\\-’''&]{1,30}$ (provided value: Hello This is a name that is toooo long)'",
      "clientSurname, 'Anthony, Gonsalves', 'client_surname: does not match the regex pattern ^[\\p{L}\\p{N}\\p{Zs}\\-’''&]{1,30}$ (provided value: Anthony, Gonsalves)'",
      "clientSurname, '$£%^&*(', 'client_surname: does not match the regex pattern ^[\\p{L}\\p{N}\\p{Zs}\\-’''&]{1,30}$ (provided value: $£%^&*()'",
      "client2Forename, 'Hello This is a name that is toooo long', 'client_2_forename: does not match the regex pattern ^[\\p{L}\\p{N}\\p{Zs}\\-’''&]{1,30}$ (provided value: Hello This is a name that is toooo long)'",
      "client2Forename, 'Anthony, Gonsalves', 'client_2_forename: does not match the regex pattern ^[\\p{L}\\p{N}\\p{Zs}\\-’''&]{1,30}$ (provided value: Anthony, Gonsalves)'",
      "client2Forename, '$£%^&*(', 'client_2_forename: does not match the regex pattern ^[\\p{L}\\p{N}\\p{Zs}\\-’''&]{1,30}$ (provided value: $£%^&*()'",
      "client2Surname, 'Hello This is a name that is toooo long', 'client_2_surname: does not match the regex pattern ^[\\p{L}\\p{N}\\p{Zs}\\-’''&]{1,30}$ (provided value: Hello This is a name that is toooo long)'",
      "client2Surname, 'Anthony, Gonsalves', 'client_2_surname: does not match the regex pattern ^[\\p{L}\\p{N}\\p{Zs}\\-’''&]{1,30}$ (provided value: Anthony, Gonsalves)'",
      "client2Surname, '$£%^&*(', 'client_2_surname: does not match the regex pattern ^[\\p{L}\\p{N}\\p{Zs}\\-’''&]{1,30}$ (provided value: $£%^&*()'",
      "uniqueClientNumber, '$£%^&*(', 'unique_client_number: does not match the regex pattern ^(0[1-9]|[12][0-9]|3[01])(0[1-9]|1[0-2])(19[0-9]{2}|20[0-9]{2})/[\\p{L}0-9 \\-’''&]/[\\p{L}0-9 \\-’''&]{1,4}$ (provided value: $£%^&*()'",
      "uniqueClientNumber, '123456/A/ABCD', 'unique_client_number: does not match the regex pattern ^(0[1-9]|[12][0-9]|3[01])(0[1-9]|1[0-2])(19[0-9]{2}|20[0-9]{2})/[\\p{L}0-9 \\-’''&]/[\\p{L}0-9 \\-’''&]{1,4}$ (provided value: 123456/A/ABCD)'",
      "uniqueClientNumber, '12121999-A-ABCD', 'unique_client_number: does not match the regex pattern ^(0[1-9]|[12][0-9]|3[01])(0[1-9]|1[0-2])(19[0-9]{2}|20[0-9]{2})/[\\p{L}0-9 \\-’''&]/[\\p{L}0-9 \\-’''&]{1,4}$ (provided value: 12121999-A-ABCD)'",
      "uniqueClientNumber, '31121899/A/ABCD', 'unique_client_number: does not match the regex pattern ^(0[1-9]|[12][0-9]|3[01])(0[1-9]|1[0-2])(19[0-9]{2}|20[0-9]{2})/[\\p{L}0-9 \\-’''&]/[\\p{L}0-9 \\-’''&]{1,4}$ (provided value: 31121899/A/ABCD)'",
      "uniqueClientNumber, '01012100/A/ABCD', 'unique_client_number: does not match the regex pattern ^(0[1-9]|[12][0-9]|3[01])(0[1-9]|1[0-2])(19[0-9]{2}|20[0-9]{2})/[\\p{L}0-9 \\-’''&]/[\\p{L}0-9 \\-’''&]{1,4}$ (provided value: 01012100/A/ABCD)'",
      "clientPostcode, 'ABCD', 'client_postcode: does not match the regex pattern ^NFA|GIR 0AA|[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}$ (provided value: ABCD)'",
      "clientPostcode, 'ABC12 BCD', 'client_postcode: does not match the regex pattern ^NFA|GIR 0AA|[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}$ (provided value: ABC12 BCD)'",
      "client2Postcode, 'ABCD', 'client_2_postcode: does not match the regex pattern ^NFA|GIR 0AA|[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}$ (provided value: ABCD)'",
      "client2Postcode, 'ABC12 BCD', 'client_2_postcode: does not match the regex pattern ^NFA|GIR 0AA|[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}$ (provided value: ABC12 BCD)'",
      "genderCode, 'MF', 'gender_code: does not match the regex pattern ^([MFU])$ (provided value: MF)'",
      "genderCode, 'A', 'gender_code: does not match the regex pattern ^([MFU])$ (provided value: A)'",
      "genderCode, '1', 'gender_code: does not match the regex pattern ^([MFU])$ (provided value: 1)'",
      "genderCode, '*', 'gender_code: does not match the regex pattern ^([MFU])$ (provided value: *)'",
      "ethnicityCode, '17', 'ethnicity_code: does not match the regex pattern ^(0[0-9]|1[0-6]|99)$ (provided value: 17)'",
      "ethnicityCode, 'AB', 'ethnicity_code: does not match the regex pattern ^(0[0-9]|1[0-6]|99)$ (provided value: AB)'",
      "ethnicityCode, '96', 'ethnicity_code: does not match the regex pattern ^(0[0-9]|1[0-6]|99)$ (provided value: 96)'",
      "ethnicityCode, '**', 'ethnicity_code: does not match the regex pattern ^(0[0-9]|1[0-6]|99)$ (provided value: **)'",
      "disabilityCode, '**', 'disability_code: does not match the regex pattern ^(NCD|MOB|DEA|HEA|VIS|BLI|MHC|LDD|COG|ILL|OTH|UKN|PHY|SEN)$ (provided value: **)'",
      "disabilityCode, 'ABC', 'disability_code: does not match the regex pattern ^(NCD|MOB|DEA|HEA|VIS|BLI|MHC|LDD|COG|ILL|OTH|UKN|PHY|SEN)$ (provided value: ABC)'",
      "clientTypeCode, 'MF', 'client_type_code: does not match the regex pattern ^([PCJ])$ (provided value: MF)'",
      "clientTypeCode, 'A', 'client_type_code: does not match the regex pattern ^([PCJ])$ (provided value: A)'",
      "clientTypeCode, '1', 'client_type_code: does not match the regex pattern ^([PCJ])$ (provided value: 1)'",
      "clientTypeCode, '*', 'client_type_code: does not match the regex pattern ^([PCJ])$ (provided value: *)'",
      "homeOfficeClientNumber, 'abc def', 'home_office_client_number: does not match the regex pattern ^[a-zA-Z0-9]+$ (provided value: abc def)'",
      "claReferenceNumber, 'ABC123', 'cla_reference_number: does not match the regex pattern ^[0-9]{1,7}$ (provided value: ABC123)'",
      "claReferenceNumber, '12345678', 'cla_reference_number: does not match the regex pattern ^[0-9]{1,7}$ (provided value: 12345678)'",
      "caseId, '12', 'case_id: does not match the regex pattern ^[0-9]{3}$ (provided value: 12)'",
      "caseId, '1234', 'case_id: does not match the regex pattern ^[0-9]{3}$ (provided value: 1234)'",
      "uniqueCaseId, '   ', 'unique_case_id: does not match the regex pattern ^\\s*\\S.*$ (provided value:    )'",
      "caseStageCode, 'ABCDE', 'case_stage_code: does not match the regex pattern ^(FPL(0[1-9]|1[0-9]|20|21)|FPC0[1-3]|MHL0[1-9])$ (provided value: ABCDE)'",
      "stageReachedCode, 'A$', 'stage_reached_code: does not match the regex pattern ^[a-zA-Z0-9]+$ (provided value: A$)'",
      "standardFeeCategoryCode, 'XYZ', 'standard_fee_category_code: does not match the regex pattern ^(1A|2A|1B|2B|1A-HSF|1B-HSF|1A-LSF|1B-LSF|1EW|1SO|2|3|ULF|UHF|CLF|CHF|Sending Hearing Fixed Fee)$ (provided value: XYZ)'",
      "outcomeCode, 'X$', 'outcome_code: does not match the regex pattern ^[a-zA-Z0-9]+$ (provided value: X$)'",
      "designatedAccreditedRepresentativeCode, '6', 'designated_accredited_representative_code: does not match the regex pattern ^[1-5]$ (provided value: 6)'",
      "mentalHealthTribunalReference, 'AB1234', 'mental_health_tribunal_reference: does not match the regex pattern ^([A-Z]{2}/[0-9]{4}/[0-9]{4}|[A-Z]{2}[0-9]{6})$ (provided value: AB1234)'",
      "mentalHealthTribunalReference, 'AB/123/1234', 'mental_health_tribunal_reference: does not match the regex pattern ^([A-Z]{2}/[0-9]{4}/[0-9]{4}|[A-Z]{2}[0-9]{6})$ (provided value: AB/123/1234)'",
      "exemptionCriteriaSatisfied, 'ZZ999', 'exemption_criteria_satisfied: does not match the regex pattern ^(DV0(0[1-9]|1[0-9]|19)|CA00[1-8]|TR001|CN001|UA001)$ (provided value: ZZ999)'",
      "exceptionalCaseFundingReference, '1234567ABX', 'exceptional_case_funding_reference: does not match the regex pattern ^[0-9]{7}[A-Z]{2}$ (provided value: 1234567ABX)'",
      "exceptionalCaseFundingReference, '12345AB', 'exceptional_case_funding_reference: does not match the regex pattern ^[0-9]{7}[A-Z]{2}$ (provided value: 12345AB)'",
      "priorAuthorityReference, 'ABC$123', 'prior_authority_reference: does not match the regex pattern ^[a-zA-Z0-9]+$ (provided value: ABC$123)'",
      "meetingsAttendedCode, 'MTGA00', 'meetings_attended_code: does not match the regex pattern ^MTGA(0[1-9]|1[0-2])$ (provided value: MTGA00)'",
      "meetingsAttendedCode, 'MTGA13', 'meetings_attended_code: does not match the regex pattern ^MTGA(0[1-9]|1[0-2])$ (provided value: MTGA13)'",
      "courtLocationCode, 'HPCDC001', 'court_location_code: does not match the regex pattern ^HPCDS(00[1-9]|0[1-9][0-9]|1[0-9]{2}|20[0-9]|21[0-5])$ (provided value: HPCDC001)'",
      "courtLocationCode, 'HPCDS999', 'court_location_code: does not match the regex pattern ^HPCDS(00[1-9]|0[1-9][0-9]|1[0-9]{2}|20[0-9]|21[0-5])$ (provided value: HPCDS999)'",
      "homeOfficeClientNumber, 'ABCDEFGHIJKLMNOPQ', 'home_office_client_number: must be at most 16 characters long (provided value: ABCDEFGHIJKLMNOPQ)'",
      "claExemptionCode, 'ABC', 'cla_exemption_code: must be at least 4 characters long (provided value: ABC)'",
      "claExemptionCode, 'ABCDE', 'cla_exemption_code: must be at most 4 characters long (provided value: ABCDE)'",
      "caseStageCode, 'FP01', 'case_stage_code: must be at least 5 characters long (provided value: FP01)'",
      "caseStageCode, 'FP0010', 'case_stage_code: must be at most 5 characters long (provided value: FP0010)'",
      "stageReachedCode, 'A', 'stage_reached_code: must be at least 2 characters long (provided value: A)'",
      "stageReachedCode, 'ABC', 'stage_reached_code: must be at most 2 characters long (provided value: ABC)'",
      "outcomeCode, 'X', 'outcome_code: must be at least 2 characters long (provided value: X)'",
      "outcomeCode, 'XYZ', 'outcome_code: must be at most 2 characters long (provided value: XYZ)'",
      "followOnWork, '', 'follow_on_work: must be at least 1 characters long (provided value: )'",
      "followOnWork, 'AB', 'follow_on_work: must be at most 1 characters long (provided value: AB)'",
      "priorAuthorityReference, 'ABC12', 'prior_authority_reference: must be at least 7 characters long (provided value: ABC12)'",
      "priorAuthorityReference, 'ABCDEFGH', 'prior_authority_reference: must be at most 7 characters long (provided value: ABCDEFGH)'",
      "adviceTypeCode, 'ABC', 'advice_type_code: does not match the regex pattern ^(FTF|REM)$ (provided value: ABC)'",
      "adviceTypeCode, '', 'advice_type_code: does not match the regex pattern ^(FTF|REM)$ (provided value: )'",
      "aitHearingCentreCode, '99', 'ait_hearing_centre_code: does not match the regex pattern ^(0[1-9]|1[0-6])$ (provided value: 99)'",
      "localAuthorityNumber, 'Schedule Ref', 'local_authority_number: does not match the regex pattern ^[a-zA-Z0-9]+$ (provided value: Schedule Ref)'",
      "surgeryDate, 'Surgery date longer than 30 char', 'surgery_date: must be at most 30 characters long (provided value: Surgery date longer than 30 char)'",
      "aitHearingCentreCode, '1', 'ait_hearing_centre_code: must be at least 2 characters long (provided value: 1)'",
      "aitHearingCentreCode, '001', 'ait_hearing_centre_code: must be at most 2 characters long (provided value: 001)'",
      "localAuthorityNumber, 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB', 'local_authority_number: must be at most 30 characters long (provided value: AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB)'",
      // Dates in YYYY-MM-DD format between 1900-01-01 and 2101-01-01
      "caseStartDate, '1899-12-31', 'case_start_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 1899-12-31)'",
      "caseStartDate, '2101-01-01', 'case_start_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2101-01-01)'",
      "caseStartDate, '2025-13-01', 'case_start_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-13-01)'",
      "caseStartDate, '2025-00-10', 'case_start_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-00-10)'",
      "caseStartDate, '2025-02-32', 'case_start_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-02-32)'",
      "caseConcludedDate, '1899-12-31', 'case_concluded_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 1899-12-31)'",
      "caseConcludedDate, '2101-01-01', 'case_concluded_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2101-01-01)'",
      "caseConcludedDate, '2025-13-01', 'case_concluded_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-13-01)'",
      "caseConcludedDate, '2025-00-10', 'case_concluded_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-00-10)'",
      "caseConcludedDate, '2025-02-32', 'case_concluded_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-02-32)'",
      "representationOrderDate, '1899-12-31', 'representation_order_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 1899-12-31)'",
      "representationOrderDate, '2101-01-01', 'representation_order_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2101-01-01)'",
      "representationOrderDate, '2025-13-01', 'representation_order_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-13-01)'",
      "representationOrderDate, '2025-00-10', 'representation_order_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-00-10)'",
      "representationOrderDate, '2025-02-32', 'representation_order_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-02-32)'",
      "clientDateOfBirth, '1899-12-31', 'client_date_of_birth: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 1899-12-31)'",
      "clientDateOfBirth, '2101-01-01', 'client_date_of_birth: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2101-01-01)'",
      "clientDateOfBirth, '2025-13-01', 'client_date_of_birth: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-13-01)'",
      "clientDateOfBirth, '2025-00-10', 'client_date_of_birth: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-00-10)'",
      "clientDateOfBirth, '2025-02-32', 'client_date_of_birth: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-02-32)'",
      "client2DateOfBirth, '1899-12-31', 'client_2_date_of_birth: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 1899-12-31)'",
      "client2DateOfBirth, '2101-01-01', 'client_2_date_of_birth: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2101-01-01)'",
      "client2DateOfBirth, '2025-13-01', 'client_2_date_of_birth: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-13-01)'",
      "client2DateOfBirth, '2025-00-10', 'client_2_date_of_birth: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-00-10)'",
      "client2DateOfBirth, '2025-02-32', 'client_2_date_of_birth: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-02-32)'",
      "transferDate, '1899-12-31', 'transfer_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 1899-12-31)'",
      "transferDate, '2101-01-01', 'transfer_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2101-01-01)'",
      "transferDate, '2025-13-01', 'transfer_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-13-01)'",
      "transferDate, '2025-00-10', 'transfer_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-00-10)'",
      "transferDate, '2025-02-32', 'transfer_date: does not match the regex pattern ^(19\\d{2}|20\\d{2}|2100)-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])$ (provided value: 2025-02-32)'"
    })
    void validateClaimIndividualInvalidField(
        String fieldName, String badValue, String expectedError) {
      ClaimResponse claim = getMinimumValidClaim();
      setField(claim, fieldName, badValue);
      List<String> errors = jsonSchemaValidator.validate("claim", claim);
      assertThat(errors).contains(expectedError);
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
    @CsvSource({
      "lineNumber, 2",
      "lineNumber, 2000",
      "adviceTime, 2",
      "travelTime, 2",
      "waitingTime, 2",
      "suspectsDefendantsCount, 10",
      "policeStationCourtAttendancesCount, 10",
      "scheduleReference, 'ShortNSweet'",
      "scheduleReference, 'Valid123'",
      "scheduleReference, '0U733A/2018/02'",
      "caseReferenceNumber, 'CASE123'",
      "caseReferenceNumber, 'SJ/4558/2018/275877'",
      "uniqueFileNumber, '010123/001'",
      "crimeMatterTypeCode, '87'",
      "accessPointCode, 'AP12345'",
      "deliveryLocation, 'XY12345'",
      "policeStationCourtPrisonId, 'C123'",
      "dsccNumber, 'XYZ12345AB'",
      "maatId, 'XYZ12345AB'",
      "prisonLawPriorApprovalNumber, 'XYZ12345AB'",
      "isDutySolicitor, 'true'",
      "isDutySolicitor, 'false'",
      "isYouthCourt, 'true'",
      "isYouthCourt, 'false'",
      "schemeId, 'AB12'",
      "mediationSessionsCount, 10",
      "mediationTimeMinutes, 1000",
      "clientForename, Tom Marvolo Ridle",
      "clientForename, James Bond",
      "clientForename, Mrs D'souza",
      "clientForename, Name That Is Thirty Characters",
      "clientForename, ÀçœñtëÐ Ñâmé",
      "clientSurname, Tom Marvolo Ridle",
      "clientSurname, James Bond",
      "clientSurname, Mrs D'souza",
      "clientSurname, Name That Is Thirty Characters",
      "clientSurname, ÀçœñtëÐ Ñâmé",
      "client2Forename, Tom Marvolo Ridle",
      "client2Forename, James Bond",
      "client2Forename, Mrs D'souza",
      "client2Forename, Name That Is Thirty Characters",
      "client2Forename, ÀçœñtëÐ Ñâmé",
      "client2Surname, Tom Marvolo Ridle",
      "client2Surname, James Bond",
      "client2Surname, Mrs D'souza",
      "client2Surname, Name That Is Thirty Characters",
      "client2Surname, ÀçœñtëÐ Ñâmé",
      "caseStartDate, '2020-8-1'",
      "caseStartDate, '2020-12-31'",
      "representationOrderDate, '2020-8-1'",
      "representationOrderDate, '2020-12-31'",
      "clientDateOfBirth, '2020-8-1'",
      "clientDateOfBirth, '2020-12-31'",
      "client2DateOfBirth, '2020-8-1'",
      "client2DateOfBirth, '2020-12-31'",
      "uniqueClientNumber, '12121999/A/HELL'",
      "uniqueClientNumber, '01011900/A/D''SO'",
      "uniqueClientNumber, '31122099/À/D123'",
      "clientPostcode, 'AB12 9CD'",
      "clientPostcode, 'A12 9CD'",
      "clientPostcode, 'A129CD'",
      "clientPostcode, 'A1B 9CD'",
      "clientPostcode, 'NFA'",
      "client2Postcode, 'AB12 9CD'",
      "client2Postcode, 'A12 9CD'",
      "client2Postcode, 'A129CD'",
      "client2Postcode, 'A1B 9CD'",
      "client2Postcode, 'NFA'",
      "genderCode, 'M'",
      "genderCode, 'F'",
      "genderCode, 'U'",
      "ethnicityCode, '00'",
      "ethnicityCode, '16'",
      "ethnicityCode, '99'",
      "disabilityCode, 'VIS'",
      "isLegallyAided, 'true'",
      "isLegallyAided, 'false'",
      "clientTypeCode, 'P'",
      "clientTypeCode, 'C'",
      "clientTypeCode, 'J'",
    })
    void validateClaimIndividualValidField(String fieldName, String badValue) {
      ClaimResponse claim = getMinimumValidClaim();
      setField(claim, fieldName, badValue);
      List<String> errors = jsonSchemaValidator.validate("claim", claim);
      assertThat(errors).isEmpty();
    }
  }

  private static @NotNull SubmissionResponse getMinimumValidSubmission() {
    SubmissionResponse submission = new SubmissionResponse();
    submission
        .submissionId(UUID.randomUUID())
        .bulkSubmissionId(UUID.randomUUID())
        .officeAccountNumber("2Q286D")
        .submissionPeriod("OCT-2024")
        .areaOfLaw("CRIME")
        .status(SubmissionStatus.CREATED)
        .scheduleNumber("SCHEDULE/NUMBER/1")
        .isNilSubmission(false)
        .numberOfClaims(1);
    return submission;
  }

  private static @NotNull ClaimResponse getMinimumValidClaim() {
    ClaimResponse claim = new ClaimResponse();
    claim
        .lineNumber(1)
        .caseReferenceNumber("CaseReferenceNumber")
        .status(ClaimStatus.READY_TO_PROCESS)
        .scheduleReference("ScheduleReference")
        .caseStartDate("2020-04-10")
        .netDisbursementAmount(BigDecimal.valueOf(20.10))
        .disbursementsVatAmount(BigDecimal.valueOf(10.20))
        .isVatApplicable(true)
        .feeCode("FeeCode");
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

  private List<String> validateForInvalidDataTypes(
      String schemaName, Object baseValidObject, String fieldName, String badJsonValue)
      throws Exception {

    ObjectMapper mapper =
        new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);

    JsonNode validNode = mapper.valueToTree(baseValidObject);
    ObjectNode node = validNode.deepCopy();
    node.set(fieldName, mapper.readTree(badJsonValue));

    return jsonSchemaValidator.validate(schemaName, node);
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
