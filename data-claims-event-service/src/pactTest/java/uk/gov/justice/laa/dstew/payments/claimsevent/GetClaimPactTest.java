package uk.gov.justice.laa.dstew.payments.claimsevent;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException.NotFound;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ClaimsApiPactTestConfig;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"laa.claims-api.url=http://localhost:1232"})
@PactConsumerTest
@PactTestFor(providerName = AbstractPactTest.PROVIDER)
@MockServerConfig(port = "1232") // Same as Claims API URL port
@Import(ClaimsApiPactTestConfig.class)
@DisplayName("GET: /api/v1/submissions/{}/claims/{} PACT tests")
public final class GetClaimPactTest extends AbstractPactTest {

  @Autowired DataClaimsRestClient dataClaimsRestClient;

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact getClaim200(PactDslWithProvider builder) {
    // Defines expected 200 response for existing claim using matchers
    return builder
        .given("a claim exists")
        .uponReceiving("a request to fetch an existing claim")
        .matchPath("/api/v1/submissions/(" + UUID_REGEX + ")/claims/(" + UUID_REGEX + ")")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json"))
        .body(
            LambdaDsl.newJsonBody(
                    body -> {
                      body.uuid("id", CLAIM_ID);
                      body.uuid("submission_id", SUBMISSION_ID);
                      body.stringType("status", "READY_TO_PROCESS");
                      body.stringType("schedule_reference", "string");
                      body.numberType("line_number", 0);
                      body.stringType("case_reference_number", "string");
                      body.stringType("unique_file_number", "string");
                      body.stringType("case_start_date", "string");
                      body.stringType("case_concluded_date", "string");
                      body.stringType("matter_type_code", "string");
                      body.stringType("crime_matter_type_code", "string");
                      body.stringType("fee_scheme_code", "string");
                      body.stringType("fee_code", "string");
                      body.stringType("procurement_area_code", "string");
                      body.stringType("access_point_code", "string");
                      body.stringType("delivery_location", "string");
                      body.stringType("representation_order_date", "string");
                      body.numberType("suspects_defendants_count", 0);
                      body.numberType("police_station_court_attendances_count", 0);
                      body.stringType("police_station_court_prison_id", "string");
                      body.stringType("dscc_number", "string");
                      body.stringType("maat_id", "string");
                      body.stringType("prison_law_prior_approval_number", "string");
                      body.booleanType("is_duty_solicitor", true);
                      body.booleanType("is_youth_court", true);
                      body.stringType("scheme_id", "string");
                      body.numberType("mediation_sessions_count", 0);
                      body.numberType("mediation_time_minutes", 0);
                      body.stringType("outreach_location", "string");
                      body.stringType("referral_source", "string");
                      body.stringType("client_forename", "string");
                      body.stringType("client_surname", "string");
                      body.stringType("client_date_of_birth", "string");
                      body.stringType("unique_client_number", "string");
                      body.stringType("client_postcode", "string");
                      body.stringType("gender_code", "string");
                      body.stringType("ethnicity_code", "string");
                      body.stringType("disability_code", "string");
                      body.booleanType("is_legally_aided", true);
                      body.stringType("client_type_code", "string");
                      body.stringType("home_office_client_number", "string");
                      body.stringType("cla_reference_number", "string");
                      body.stringType("cla_exemption_code", "string");
                      body.stringType("client_2_forename", "string");
                      body.stringType("client_2_surname", "string");
                      body.stringType("client_2_date_of_birth", "string");
                      body.stringType("client_2_ucn", "string");
                      body.stringType("client_2_postcode", "string");
                      body.stringType("client_2_gender_code", "string");
                      body.stringType("client_2_ethnicity_code", "string");
                      body.stringType("client_2_disability_code", "string");
                      body.booleanType("client_2_is_legally_aided", true);
                      body.stringType("case_id", "string");
                      body.stringType("unique_case_id", "string");
                      body.stringType("case_stage_code", "string");
                      body.stringType("stage_reached_code", "string");
                      body.stringType("standard_fee_category_code", "string");
                      body.stringType("outcome_code", "string");
                      body.stringType("designated_accredited_representative_code", "string");
                      body.booleanType("is_postal_application_accepted", true);
                      body.booleanType("is_client_2_postal_application_accepted", true);
                      body.stringType("mental_health_tribunal_reference", "string");
                      body.booleanType("is_nrm_advice", true);
                      body.stringType("follow_on_work", "string");
                      body.stringType("transfer_date", "string");
                      body.stringType("exemption_criteria_satisfied", "string");
                      body.stringType("exceptional_case_funding_reference", "string");
                      body.booleanType("is_legacy_case", true);
                      body.numberType("advice_time", 0);
                      body.numberType("travel_time", 0);
                      body.numberType("waiting_time", 0);
                      body.numberType("net_profit_costs_amount", 0);
                      body.numberType("net_disbursement_amount", 0);
                      body.numberType("net_counsel_costs_amount", 0);
                      body.numberType("disbursements_vat_amount", 0);
                      body.numberType("travel_waiting_costs_amount", 0);
                      body.numberType("net_waiting_costs_amount", 0);
                      body.booleanType("is_vat_applicable", true);
                      body.booleanType("is_tolerance_applicable", true);
                      body.stringType("prior_authority_reference", "string");
                      body.booleanType("is_london_rate", true);
                      body.numberType("adjourned_hearing_fee_amount", 0);
                      body.booleanType("is_additional_travel_payment", true);
                      body.numberType("costs_damages_recovered_amount", 0);
                      body.stringType("meetings_attended_code", "string");
                      body.numberType("detention_travel_waiting_costs_amount", 0);
                      body.numberType("jr_form_filling_amount", 0);
                      body.booleanType("is_eligible_client", true);
                      body.stringType("court_location_code", "string");
                      body.stringType("advice_type_code", "string");
                      body.numberType("medical_reports_count", 0);
                      body.booleanType("is_irc_surgery", true);
                      body.stringType("surgery_date", "string");
                      body.numberType("surgery_clients_count", 0);
                      body.numberType("surgery_matters_count", 0);
                      body.numberType("cmrh_oral_count", 0);
                      body.numberType("cmrh_telephone_count", 0);
                      body.stringType("ait_hearing_centre_code", "string");
                      body.booleanType("is_substantive_hearing", true);
                      body.numberType("ho_interview", 0);
                      body.stringType("local_authority_number", "string");
                      body.stringType("submission_period", "string");
                      body.stringType("created_by_user_id", "string");
                      body.object(
                          "fee_calculation_response",
                          fee -> {
                            fee.stringType("calculated_fee_detail_id", "string");
                            fee.uuid("claim_summary_fee_id", CLAIM_SUMMARY_FEE_ID);
                            fee.uuid("claim_id", CLAIM_ID);
                            fee.stringType("fee_code", "string");
                            fee.stringType("fee_code_description", "string");
                            fee.stringType("fee_type", "HOURLY");
                            fee.stringType("category_of_law", "string");
                            fee.numberType("total_amount", 0);
                            fee.booleanType("vat_indicator", true);
                            fee.numberType("vat_rate_applied", 0);
                            fee.numberType("calculated_vat_amount", 0);
                            fee.numberType("disbursement_amount", 0);
                            fee.numberType("requested_net_disbursement_amount", 0);
                            fee.numberType("disbursement_vat_amount", 0);
                            fee.numberType("hourly_total_amount", 0);
                            fee.numberType("fixed_fee_amount", 0);
                            fee.numberType("net_profit_costs_amount", 0);
                            fee.numberType("requested_net_profit_costs_amount", 0);
                            fee.numberType("net_cost_of_counsel_amount", 0);
                            fee.numberType("net_travel_costs_amount", 0);
                            fee.numberType("net_waiting_costs_amount", 0);
                            fee.numberType("detention_travel_and_waiting_costs_amount", 0);
                            fee.numberType("jr_form_filling_amount", 0);
                            fee.numberType("travel_and_waiting_costs_amount", 0);
                            fee.object(
                                "bolt_on_details",
                                bolt -> {
                                  bolt.numberType("bolt_on_total_fee_amount", 0);
                                  bolt.numberType("bolt_on_adjourned_hearing_count", 0);
                                  bolt.numberType("bolt_on_adjourned_hearing_fee", 0);
                                  bolt.numberType("bolt_on_cmrh_telephone_count", 0);
                                  bolt.numberType("bolt_on_cmrh_telephone_fee", 0);
                                  bolt.numberType("bolt_on_cmrh_oral_count", 0);
                                  bolt.numberType("bolt_on_cmrh_oral_fee", 0);
                                  bolt.numberType("bolt_on_home_office_interview_count", 0);
                                  bolt.numberType("bolt_on_home_office_interview_fee", 0);
                                  bolt.numberType("bolt_on_substantive_hearing_fee", 0);
                                  bolt.booleanType("escape_case_flag", true);
                                  bolt.stringType("scheme_id", "string");
                                });
                          });
                    })
                .build())
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact getClaim404(PactDslWithProvider builder) {
    // Defines expected 404 response for when either submission or claim does not exist
    return builder
        .given("no claim exists")
        .uponReceiving("a request to fetch a non-existent claim")
        .matchPath("/api/v1/submissions/(" + UUID_REGEX + ")/claims/(" + UUID_REGEX + ")")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(Map.of("Content-Type", "application/json"))
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  RequestResponsePact getClaimNoSubmission404(PactDslWithProvider builder) {
    // Defines expected 404 response for when either submission or claim does not exist
    return builder
        .given("no claim exists")
        .uponReceiving("a request to fetch a claim from a non-existent submission")
        .matchPath("/api/v1/submissions/(" + UUID_REGEX + ")/claims/(" + UUID_REGEX + ")")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(Map.of("Content-Type", "application/json"))
        .toPact();
  }

  @Test
  @DisplayName("Verify 200 response")
  @PactTestFor(pactMethod = "getClaim200")
  void verify200Response() {
    ClaimResponse claimResponse = dataClaimsRestClient.getClaim(SUBMISSION_ID, CLAIM_ID).getBody();

    assertThat(claimResponse).isNotNull();
    assertThat(claimResponse.getId()).isEqualTo(CLAIM_ID.toString());
    assertThat(claimResponse.getSubmissionId()).isEqualTo(SUBMISSION_ID.toString());
  }

  @Test
  @DisplayName("Verify 404 response")
  @PactTestFor(pactMethod = "getClaim404")
  void verify404Response() {
    assertThrows(NotFound.class, () -> dataClaimsRestClient.getClaim(SUBMISSION_ID, CLAIM_ID));
  }

  @Test
  @DisplayName("Verify 404 response no submission")
  @PactTestFor(pactMethod = "getClaimNoSubmission404")
  void verify404ResponseNoSubmission() {
    assertThrows(NotFound.class, () -> dataClaimsRestClient.getClaim(SUBMISSION_ID, CLAIM_ID));
  }
}
