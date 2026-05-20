package uk.gov.justice.laa.dstew.payments.claimsevent.validator.claim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Integration tests for {@link
 * uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.ClaimSchemaValidator}.
 */
public class ClaimSchemaValidatorITest extends ClaimValidationIntegrationTestBase {

  private static final String VALIDATOR_PATH = CLAIMS_BASE_PATH + "ClaimSchemaValidator/";
  private static final Set<String> SCHEMA_ERROR = Set.of("SCHEMA_VALIDATION_ERROR");
  private static final Set<String> NONE = Set.of();

  static List<Arguments> claimsResponse() {
    return List.of(
        // ── Valid claims ──────────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-valid.json", NONE),
        Arguments.of(SUBMISSION_CRIME_LOWER, "cl-valid.json", NONE),
        Arguments.of(SUBMISSION_MEDIATION, "med-valid.json", NONE),

        // ── Required fields missing ───────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-status.json", SCHEMA_ERROR),
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-line-number.json", SCHEMA_ERROR),
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-missing-net-disbursement-amount.json", SCHEMA_ERROR),
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-missing-disbursements-vat-amount.json", SCHEMA_ERROR),
        // TODO: fee scheme has a different error first as it is used to get the fee type.
        // Arguments.of(SUBMISSION_LEGAL_HELP, "lh-missing-fee-code.json", SCHEMA_ERROR),

        // ── schedule_reference ────────────────────────────────────────────────
        // TODO: old schema validator concatenates multiple validation errors for the same field.
        //  The new validator produces them in one order, the old in the opposite order.
        // Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-schedule-reference.json", SCHEMA_ERROR),

        // ── case_reference_number ─────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-case-reference-number.json", SCHEMA_ERROR),

        // ── unique_file_number ────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-unique-file-number.json", SCHEMA_ERROR),

        // ── case_start_date ───────────────────────────────────────────────────
        // TODO: Test actually passes but it tries to use the invalid date when getting the fees
        // Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-case-start-date.json", SCHEMA_ERROR),

        // ── case_concluded_date ──────────────────────────────────────────────
        // TODO: Test actually passes but it tries to use the invalid date when getting the fees
        // Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-case-concluded-date.json", SCHEMA_ERROR),

        // ── matter_type_code ──────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-matter-type-code.json", NONE),
        Arguments.of(
            SUBMISSION_MEDIATION,
            "med-invalid-matter-type-code.json",
            Set.of("INVALID_MATTER_TYPE_CODE")),

        // ── crime_matter_type_code ────────────────────────────────────────────
        Arguments.of(
            SUBMISSION_CRIME_LOWER, "cl-invalid-crime-matter-type-code.json", SCHEMA_ERROR),

        // ── fee_code ─────────────────────────────────────────────────────────
        // TODO: errors because we try to get the fee type prior to validating
        // Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-fee-code.json", SCHEMA_ERROR),

        // ── procurement_area_code ─────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-procurement-area-code.json", SCHEMA_ERROR),

        // ── access_point_code ─────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-access-point-code.json", SCHEMA_ERROR),

        // ── delivery_location ─────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-delivery-location.json", SCHEMA_ERROR),

        // ── representation_order_date ─────────────────────────────────────────
        // TODO: Test actually passes but it tries to use the invalid date when getting the fees
        // Arguments.of(
        //    SUBMISSION_LEGAL_HELP, "lh-invalid-representation-order-date.json", SCHEMA_ERROR),

        // ── suspects_defendants_count ─────────────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-suspects-defendants-count.json", SCHEMA_ERROR),

        // ── police_station_court_attendances_count ────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "lh-invalid-police-station-court-attendances-count.json",
            SCHEMA_ERROR),

        // ── police_station_court_prison_id ────────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-police-station-court-prison-id.json", SCHEMA_ERROR),

        // ── dscc_number ───────────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-dscc-number.json", SCHEMA_ERROR),

        // ── maat_id ───────────────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-maat-id.json", SCHEMA_ERROR),

        // ── prison_law_prior_approval_number ──────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "lh-invalid-prison-law-prior-approval-number.json",
            SCHEMA_ERROR),

        // ── mediation_sessions_count ──────────────────────────────────────────
        Arguments.of(
            SUBMISSION_MEDIATION, "med-invalid-mediation-sessions-count.json", SCHEMA_ERROR),

        // ── mediation_time_minutes ────────────────────────────────────────────
        Arguments.of(SUBMISSION_MEDIATION, "med-invalid-mediation-time-minutes.json", SCHEMA_ERROR),

        // ── outreach_location ─────────────────────────────────────────────────
        Arguments.of(SUBMISSION_MEDIATION, "med-invalid-outreach-location.json", SCHEMA_ERROR),

        // ── referral_source ───────────────────────────────────────────────────
        Arguments.of(SUBMISSION_MEDIATION, "med-invalid-referral-source.json", SCHEMA_ERROR),

        // ── client_forename ───────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-client-forename.json", SCHEMA_ERROR),

        // ── client_surname ────────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-client-surname.json", SCHEMA_ERROR),

        // ── client_date_of_birth ──────────────────────────────────────────────
        // TODO: invalid test as current does not dedupe and so has 2 errors when only should be 1
        // Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-client-date-of-birth.json",
        // SCHEMA_ERROR),

        // ── unique_client_number ──────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-unique-client-number.json", SCHEMA_ERROR),

        // ── client_postcode ───────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-client-postcode.json", SCHEMA_ERROR),

        // ── gender_code ───────────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-gender-code.json", SCHEMA_ERROR),

        // ── ethnicity_code ────────────────────────────────────────────────────
        // TODO: old schema validator concatenates multiple validation errors for the same field.
        //  The new validator produces them in one order, the old in the opposite order.
        // Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-ethnicity-code.json", SCHEMA_ERROR),

        // ── disability_code ───────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-disability-code.json", SCHEMA_ERROR),

        // ── client_type_code ──────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-client-type-code.json", SCHEMA_ERROR),

        // ── home_office_client_number ─────────────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-home-office-client-number.json", SCHEMA_ERROR),

        // ── cla_reference_number ──────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-cla-reference-number.json", SCHEMA_ERROR),

        // ── cla_exemption_code ────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-cla-exemption-code.json", SCHEMA_ERROR),

        // ── client_2_forename ─────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-client-2-forename.json", SCHEMA_ERROR),

        // ── client_2_surname ──────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-client-2-surname.json", SCHEMA_ERROR),

        // ── client_2_date_of_birth ────────────────────────────────────────────
        // TODO: invalid test as current does not dedupe and so has 2 errors when only should be 1
        // Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-client-2-date-of-birth.json",
        // SCHEMA_ERROR),

        // ── client_2_ucn ──────────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-client-2-ucn.json", SCHEMA_ERROR),

        // ── client_2_postcode ─────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-client-2-postcode.json", SCHEMA_ERROR),

        // ── client_2_gender_code ──────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-client-2-gender-code.json", SCHEMA_ERROR),

        // ── client_2_ethnicity_code ───────────────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-client-2-ethnicity-code.json", SCHEMA_ERROR),

        // ── client_2_disability_code ──────────────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-client-2-disability-code.json", SCHEMA_ERROR),

        // ── case_id ───────────────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-case-id.json", SCHEMA_ERROR),

        // ── unique_case_id ────────────────────────────────────────────────────
        // TODO: the original does not dedupe and so has 2 messages
        // Arguments.of(SUBMISSION_MEDIATION, "med-invalid-unique-case-id.json", SCHEMA_ERROR),

        // ── case_stage_code ───────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-case-stage-code.json", SCHEMA_ERROR),

        // ── stage_reached_code ────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-stage-reached-code.json", SCHEMA_ERROR),
        // TODO: order of technical messgage is different,
        // Arguments.of(SUBMISSION_CRIME_LOWER, "cl-invalid-stage-reached-code.json", SCHEMA_ERROR),

        // ── standard_fee_category_code ────────────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-standard-fee-category-code.json", SCHEMA_ERROR),

        // ── outcome_code ──────────────────────────────────────────────────────
        // It just needs a value validation done in another validator as the schema allows any
        // string,
        // so we return no schema error but an INVALID_OUTCOME_CODE instead.
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-outcome-code.json", Set.of("INVALID_OUTCOME_CODE")),
        Arguments.of(
            SUBMISSION_CRIME_LOWER, "cl-invalid-outcome-code.json", Set.of("INVALID_OUTCOME_CODE")),
        Arguments.of(
            SUBMISSION_MEDIATION, "med-invalid-outcome-code.json", Set.of("INVALID_OUTCOME_CODE")),

        // ── designated_accredited_representative_code ─────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "lh-invalid-designated-accredited-representative-code.json",
            SCHEMA_ERROR),

        // ── mental_health_tribunal_reference ──────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "lh-invalid-mental-health-tribunal-reference.json",
            SCHEMA_ERROR),

        // ── transfer_date ─────────────────────────────────────────────────────
        // TODO: the original does not dedupe and so has 2 messages
        // Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-transfer-date.json", SCHEMA_ERROR),

        // ── exemption_criteria_satisfied ──────────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-exemption-criteria-satisfied.json", SCHEMA_ERROR),

        // ── exceptional_case_funding_reference ────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "lh-invalid-exceptional-case-funding-reference.json",
            SCHEMA_ERROR),

        // ── advice_time ───────────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-advice-time.json", SCHEMA_ERROR),

        // ── travel_time ───────────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-travel-time.json", SCHEMA_ERROR),

        // ── waiting_time ──────────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-waiting-time.json", SCHEMA_ERROR),

        // ── net_profit_costs_amount ───────────────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-net-profit-costs-amount.json", SCHEMA_ERROR),

        // ── net_disbursement_amount ───────────────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-net-disbursement-amount.json", SCHEMA_ERROR),

        // ── net_counsel_costs_amount ──────────────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-net-counsel-costs-amount.json", SCHEMA_ERROR),

        // ── disbursements_vat_amount ──────────────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-disbursements-vat-amount.json", SCHEMA_ERROR),

        // ── travel_waiting_costs_amount ───────────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-travel-waiting-costs-amount.json", SCHEMA_ERROR),

        // ── net_waiting_costs_amount ──────────────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-net-waiting-costs-amount.json", SCHEMA_ERROR),

        // ── prior_authority_reference ─────────────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-prior-authority-reference.json", SCHEMA_ERROR),

        // ── adjourned_hearing_fee_amount ──────────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-adjourned-hearing-fee-amount.json", SCHEMA_ERROR),

        // ── costs_damages_recovered_amount ────────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-costs-damages-recovered-amount.json", SCHEMA_ERROR),

        // ── meetings_attended_code ────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-meetings-attended-code.json", SCHEMA_ERROR),

        // ── detention_travel_waiting_costs_amount ─────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP,
            "lh-invalid-detention-travel-waiting-costs-amount.json",
            SCHEMA_ERROR),

        // ── jr_form_filling_amount ────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-jr-form-filling-amount.json", SCHEMA_ERROR),

        // ── advice_type_code ──────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-advice-type-code.json", SCHEMA_ERROR),

        // ── medical_reports_count ─────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-medical-reports-count.json", SCHEMA_ERROR),

        // ── surgery_clients_count ─────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-surgery-clients-count.json", SCHEMA_ERROR),

        // ── surgery_matters_count ─────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-surgery-matters-count.json", SCHEMA_ERROR),

        // ── cmrh_oral_count ───────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-cmrh-oral-count.json", SCHEMA_ERROR),

        // ── cmrh_telephone_count ──────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-cmrh-telephone-count.json", SCHEMA_ERROR),

        // ── ait_hearing_centre_code ───────────────────────────────────────────
        Arguments.of(
            SUBMISSION_LEGAL_HELP, "lh-invalid-ait-hearing-centre-code.json", SCHEMA_ERROR),

        // ── ho_interview ──────────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-ho-interview.json", SCHEMA_ERROR),

        // ── local_authority_number ────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-local-authority-number.json", SCHEMA_ERROR),

        // ── scheme_id ─────────────────────────────────────────────────────────
        Arguments.of(SUBMISSION_LEGAL_HELP, "lh-invalid-scheme-id.json", SCHEMA_ERROR));
  }

  @DisplayName("Claim schema validator - validate claim responses produce matching reports")
  @ParameterizedTest(name = "{1}")
  @MethodSource("claimsResponse")
  void shouldMatchValidationReportForClaimsResponse(
      String submission, String claimsResponse, Set<String> ignored) throws Exception {
    var context = runSubmissionValidationWithClaims(submission, VALIDATOR_PATH + claimsResponse);
    var claims = parseClaimsFromFixture(VALIDATOR_PATH + claimsResponse);
    for (var cr : claims) {
      assertExactMatchBetweenValidationAndReport(cr, claims, context);
    }
  }

  @DisplayName("Claim schema validator - should only produce expected error codes")
  @ParameterizedTest(name = "{1}")
  @MethodSource("claimsResponse")
  void shouldOnlyProduceExpectedErrorCodes(
      String submission, String fixture, Set<String> expectedCodes) throws Exception {
    Set<String> actual = collectValidationIssueCodes(submission, VALIDATOR_PATH + fixture);
    assertEquals(expectedCodes, actual, "Unexpected or missing error codes for " + fixture);
  }
}
