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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ClaimsApiPactTestConfig;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"laa.claims-api.url=http://localhost:1234"})
@PactConsumerTest
@PactTestFor(providerName = AbstractPactTest.PROVIDER)
@MockServerConfig(port = "1234") // Same as Claims API URL port
@Import(ClaimsApiPactTestConfig.class)
@DisplayName("GET: /api/v1/bulk-submissions/{} PACT tests")
public final class GetBulkSubmissionPactTest extends AbstractPactTest {

  @Autowired DataClaimsRestClient dataClaimsRestClient;

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  public RequestResponsePact getBulkSubmission200(PactDslWithProvider builder) {
    // Defines expected 200 response for existing bulk submission
    return builder
        .given("a bulk submission exists")
        .uponReceiving("a request to fetch a specific bulk submission")
        .matchPath("/api/v1/bulk-submissions/(" + UUID_REGEX + ")")
        .matchHeader(HttpHeaders.AUTHORIZATION, UUID_REGEX)
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(Map.of("Content-Type", "application/json"))
        .body(
            LambdaDsl.newJsonBody(
                    body -> {
                      body.uuid("bulk_submission_id", BULK_SUBMISSION_ID);
                      body.stringType("status", "READY_FOR_PARSING");
                      body.stringType("created_by_user_id", "string");
                      body.stringType("error_code", "P100");
                      body.stringType("error_description", "string");
                      body.stringType("updated_by_user_id", "string");
                      body.object(
                          "details",
                          details -> {
                            details.object(
                                "office", office -> office.stringType("account", "string"));
                            details.object(
                                "schedule",
                                schedule -> {
                                  schedule.stringType("submission_period", "string");
                                  schedule.stringType("area_of_law", "string");
                                  schedule.stringType("schedule_num", "string");
                                });
                            details.minArrayLike(
                                "outcomes",
                                1,
                                outcome -> {
                                  outcome.stringType("matter_type", "string");
                                  outcome.stringType("fee_code", "string");
                                  outcome.stringType("case_ref_number", "string");
                                  outcome.stringType("case_start_date", "string");
                                  outcome.stringType("case_id", "string");
                                  outcome.stringType("case_stage_level", "string");
                                  outcome.stringType("ufn", "string");
                                  outcome.stringType("procurement_area", "string");
                                  outcome.stringType("access_point", "string");
                                  outcome.stringType("client_forename", "string");
                                  outcome.stringType("client_surname", "string");
                                  outcome.stringType("client_date_of_birth", "string");
                                  outcome.stringType("ucn", "string");
                                  outcome.stringType("cla_ref_number", "string");
                                  outcome.stringType("cla_exemption", "string");
                                  outcome.stringType("gender", "string");
                                  outcome.stringType("ethnicity", "string");
                                  outcome.stringType("disability", "string");
                                  outcome.stringType("client_post_code", "string");
                                  outcome.stringType("work_concluded_date", "string");
                                  outcome.numberType("advice_time", 0);
                                  outcome.numberType("travel_time", 0);
                                  outcome.numberType("waiting_time", 0);
                                  outcome.numberType("profit_cost", 0);
                                  outcome.numberType("value_of_costs", 0);
                                  outcome.numberType("disbursements_amount", 0);
                                  outcome.numberType("counsel_cost", 0);
                                  outcome.numberType("disbursements_vat", 0);
                                  outcome.numberType("travel_waiting_costs", 0);
                                  outcome.booleanType("vat_indicator", true);
                                  outcome.booleanType("london_nonlondon_rate", true);
                                  outcome.stringType("client_type", "string");
                                  outcome.booleanType("tolerance_indicator", true);
                                  outcome.numberType("travel_costs", 0);
                                  outcome.stringType("outcome_code", "string");
                                  outcome.booleanType("legacy_case", true);
                                  outcome.stringType("claim_type", "string");
                                  outcome.numberType("adjourned_hearing_fee", 0);
                                  outcome.stringType("type_of_advice", "string");
                                  outcome.booleanType("postal_appl_accp", true);
                                  outcome.stringType("schedule_ref", "string");
                                  outcome.stringType("cmrh_oral", "string");
                                  outcome.stringType("cmrh_telephone", "string");
                                  outcome.stringType("ait_hearing_centre", "string");
                                  outcome.booleanType("substantive_hearing", true);
                                  outcome.numberType("ho_interview", 0);
                                  outcome.stringType("ho_ucn", "string");
                                  outcome.stringType("transfer_date", "string");
                                  outcome.numberType("detention_travel_waiting_costs", 0);
                                  outcome.stringType("delivery_location", "string");
                                  outcome.stringType("prior_authority_ref", "string");
                                  outcome.numberType("jr_form_filling", 0);
                                  outcome.booleanType("additional_travel_payment", true);
                                  outcome.stringType("meetings_attended", "string");
                                  outcome.numberType("medical_reports_claimed", 0);
                                  outcome.numberType("desi_acc_rep", 0);
                                  outcome.stringType("mht_ref_number", "string");
                                  outcome.stringType("stage_reached", "string");
                                  outcome.stringType("follow_on_work", "string");
                                  outcome.booleanType("national_ref_mechanism_advice", true);
                                  outcome.stringType("exemption_criteria_satisfied", "string");
                                  outcome.stringType("excl_case_funding_ref", "string");
                                  outcome.numberType("no_of_clients", 0);
                                  outcome.numberType("no_of_surgery_clients", 0);
                                  outcome.booleanType("irc_surgery", true);
                                  outcome.stringType("surgery_date", "string");
                                  outcome.stringType("line_number", "string");
                                  outcome.stringType("crime_matter_type", "string");
                                  outcome.stringType("fee_scheme", "string");
                                  outcome.stringType("rep_order_date", "string");
                                  outcome.numberType("no_of_suspects", 0);
                                  outcome.numberType("no_of_police_station", 0);
                                  outcome.stringType("police_station", "string");
                                  outcome.stringType("dscc_number", "string");
                                  outcome.stringType("maat_id", "string");
                                  outcome.booleanType("duty_solicitor", true);
                                  outcome.booleanType("youth_court", true);
                                  outcome.stringType("scheme_id", "string");
                                  outcome.numberType("number_of_mediation_sessions", 0);
                                  outcome.numberType("mediation_time", 0);
                                  outcome.stringType("outreach", "string");
                                  outcome.stringType("referral", "string");
                                  outcome.booleanType("client_legally_aided", true);
                                  outcome.stringType("client2_forename", "string");
                                  outcome.stringType("client2_surname", "string");
                                  outcome.stringType("client2_date_of_birth", "string");
                                  outcome.stringType("client2_ucn", "string");
                                  outcome.stringType("client2_post_code", "string");
                                  outcome.stringType("client2_gender", "string");
                                  outcome.stringType("client2_ethnicity", "string");
                                  outcome.stringType("client2_disability", "string");
                                  outcome.booleanType("client2_legally_aided", true);
                                  outcome.stringType("unique_case_id", "string");
                                  outcome.stringType("standard_fee_cat", "string");
                                  outcome.booleanType("client_2_postal_appl_accp", true);
                                  outcome.numberType("costs_damages_recovered", 0);
                                  outcome.booleanType("eligible_client", true);
                                  outcome.stringType("court_location_hpcds", "string");
                                  outcome.stringType("local_authority_number", "string");
                                  outcome.stringType("pa_number", "string");
                                  outcome.numberType("excess_travel_costs", 0);
                                  outcome.stringType("med_concluded_date", "string");
                                });
                            details.minArrayLike(
                                "matter_starts",
                                1,
                                matterStart -> {
                                  matterStart.stringType("schedule_ref", "string");
                                  matterStart.stringType("category_code", "AAP");
                                  matterStart.stringType("mediation_type", "MDCS Child Only Sole");
                                  matterStart.stringType("procurement_area", "string");
                                  matterStart.stringType("access_point", "string");
                                  matterStart.stringType("delivery_location", "string");
                                  matterStart.numberType("number_of_matter_starts", 0);
                                });
                            details.minArrayLike(
                                "immigration_clr",
                                1,
                                immigrationClr -> {
                                  immigrationClr.stringType("additionalProp1", "string");
                                  immigrationClr.stringType("additionalProp2", "string");
                                });
                          });
                    })
                .build())
        .toPact();
  }

  @SneakyThrows
  @Pact(consumer = CONSUMER)
  public RequestResponsePact getBulkSubmission404(PactDslWithProvider builder) {
    // Defines expected 404 response for missing submission
    return builder
        .given("no bulk submission exists")
        .uponReceiving("a request to fetch a non-existent bulk submission")
        .matchPath("/api/v1/bulk-submissions/(" + UUID_REGEX + ")")
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(Map.of("Content-Type", "application/json"))
        .toPact();
  }

  @Test
  @DisplayName("Verify 200 response")
  @PactTestFor(pactMethod = "getBulkSubmission200")
  void verify200Response() {
    GetBulkSubmission200Response response =
        dataClaimsRestClient.getBulkSubmission(BULK_SUBMISSION_ID).getBody();

    assertThat(response).isNotNull();
    assertThat(response.getBulkSubmissionId()).isEqualTo(BULK_SUBMISSION_ID);
  }

  @Test
  @DisplayName("Verify 404 response")
  @PactTestFor(pactMethod = "getBulkSubmission404")
  void verify404Response() {
    assertThrows(
        NotFound.class, () -> dataClaimsRestClient.getBulkSubmission(BULK_SUBMISSION_ID).getBody());
  }
}
