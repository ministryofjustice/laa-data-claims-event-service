package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.mockserver.model.HttpRequest.request;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockserver.model.Parameter;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MessageListenerBase;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;

@ActiveProfiles("test")
@ImportTestcontainers(MessageListenerBase.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(MockServerIntegrationTest.ClaimsConfiguration.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.cloud.aws.sqs.enabled=false", // Disable AWS SQS functionality
      "laa.bulk-claim-queue.name=not-used", // Dummy queue name to avoid initialization issues
    })
public class ProviderDetailsServiceRetryTest extends MockServerIntegrationTest {

  private static final String OFFICE_CODE = "string";

  @Autowired ProviderDetailsService providerDetailsService;

  @Test
  void shouldRetryOnFailure() throws Exception {
    // given
    LocalDate effectiveDate = LocalDate.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    stubForGetProviderOfficeReturnsError(
        OFFICE_CODE,
        List.of(
            new Parameter("areaOfLaw", AreaOfLaw.LEGAL_HELP.getValue()),
            new Parameter("effectiveDate", formatter.format(effectiveDate))));

    // when
    StepVerifier.create(
            providerDetailsService.getProviderFirmSchedules(
                OFFICE_CODE, AreaOfLaw.LEGAL_HELP.getValue(), effectiveDate))
        .expectError()
        .verify();

    // then
    mockServerClient.verify(
        request()
            .withMethod("GET")
            .withPath("/api/v1/provider-offices/" + OFFICE_CODE + "/schedules"),
        VerificationTimes.exactly(3));
  }

  @Test
  void shouldRetryOnFailure_successAtThird() throws Exception {
    // given
    LocalDate effectiveDate = LocalDate.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    stubForGetProviderOfficeReturnsErrorWithTimes(
        OFFICE_CODE,
        List.of(
            new Parameter("areaOfLaw", AreaOfLaw.LEGAL_HELP.getValue()),
            new Parameter("effectiveDate", formatter.format(effectiveDate))),
        2);
    stubForGetProviderOfficeWithTimes(
        OFFICE_CODE,
        List.of(
            new Parameter("areaOfLaw", AreaOfLaw.LEGAL_HELP.getValue()),
            new Parameter("effectiveDate", formatter.format(effectiveDate))),
        "provider-details/get-firm-schedules-openapi-200.json",
        1);

    // when
    StepVerifier.create(
            providerDetailsService.getProviderFirmSchedules(
                OFFICE_CODE, AreaOfLaw.LEGAL_HELP.getValue(), effectiveDate))
        .expectNextMatches(dto -> dto.getOffice().getFirmOfficeCode().equals(OFFICE_CODE))
        .expectComplete()
        .verify();

    // then
    mockServerClient.verify(
        request()
            .withMethod("GET")
            .withPath("/api/v1/provider-offices/" + OFFICE_CODE + "/schedules"),
        VerificationTimes.exactly(3));
  }
}
