package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.mockserver.model.HttpRequest.request;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.Parameter;
import org.mockserver.verify.VerificationTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.http.HttpStatusCode;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MessageListenerBase;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleDetails;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeSummary;

@ActiveProfiles("test")
@ImportTestcontainers(MessageListenerBase.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(MockServerIntegrationTest.ClaimsConfiguration.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.cloud.aws.sqs.enabled=false",
      "laa.bulk-claim-queue.name=not-used",
    })
class ProviderDetailsServiceCacheIntegrationTest extends MockServerIntegrationTest {

  private static final String AREA_OF_LAW = AreaOfLaw.LEGAL_HELP.getValue();
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

  @Autowired ProviderDetailsService providerDetailsService;

  @Test
  void shouldMergeCoverageWindowsAndReturnCachedHits() throws Exception {
    String officeCode = "0P322F";
    LocalDate initial = LocalDate.of(2019, 8, 20);
    LocalDate extendEnd = LocalDate.of(2019, 9, 26);
    LocalDate extendStart = LocalDate.of(2017, 6, 27);
    LocalDate gapFill = LocalDate.of(2018, 7, 30);

    stubCoverage(officeCode, initial, LocalDate.of(2018, 9, 1), LocalDate.of(2019, 8, 31));
    stubCoverage(officeCode, extendEnd, LocalDate.of(2018, 9, 1), LocalDate.of(2020, 8, 31));
    stubCoverage(officeCode, extendStart, LocalDate.of(2017, 4, 1), LocalDate.of(2018, 3, 31));
    stubCoverage(officeCode, gapFill, LocalDate.of(2017, 4, 1), LocalDate.of(2020, 8, 31));

    StepVerifier.create(call(officeCode, initial))
        .expectNextMatches(dto -> covers(dto, LocalDate.of(2018, 11, 21)))
        .verifyComplete();

    StepVerifier.create(call(officeCode, LocalDate.of(2018, 11, 21)))
        .expectNextMatches(dto -> covers(dto, LocalDate.of(2018, 11, 21)))
        .verifyComplete();

    StepVerifier.create(call(officeCode, extendEnd))
        .expectNextMatches(dto -> covers(dto, extendEnd))
        .verifyComplete();

    StepVerifier.create(call(officeCode, extendStart))
        .expectNextMatches(dto -> covers(dto, extendStart))
        .verifyComplete();

    StepVerifier.create(call(officeCode, gapFill))
        .expectNextMatches(dto -> covers(dto, gapFill))
        .verifyComplete();

    List<LocalDate> cachedDates =
        List.of(
            LocalDate.of(2019, 8, 5),
            LocalDate.of(2019, 4, 29),
            LocalDate.of(2018, 5, 30),
            LocalDate.of(2019, 4, 12),
            LocalDate.of(2019, 1, 22),
            LocalDate.of(2018, 3, 26),
            LocalDate.of(2017, 11, 10),
            LocalDate.of(2018, 10, 1),
            LocalDate.of(2019, 2, 28));

    cachedDates.forEach(
        date ->
            StepVerifier.create(call(officeCode, date))
                .expectNextMatches(dto -> dto.getSchedules().size() == 4 && covers(dto, date))
                .verifyComplete());

    verifyCall(officeCode, initial, 1);
    verifyCall(officeCode, extendEnd, 1);
    verifyCall(officeCode, extendStart, 1);
    verifyCall(officeCode, gapFill, 1);
  }

  @Test
  void shouldCacheNegativeResponsesPerEffectiveDate() throws Exception {
    String officeCode = "NEG_0P322F";
    LocalDate missingDate = LocalDate.of(2020, 1, 1);
    LocalDate otherDate = LocalDate.of(2020, 2, 1);

    stubNegative(officeCode, missingDate);
    stubCoverage(officeCode, otherDate, otherDate, otherDate.plusMonths(1));

    StepVerifier.create(call(officeCode, missingDate)).verifyComplete();
    StepVerifier.create(call(officeCode, missingDate)).verifyComplete();

    StepVerifier.create(call(officeCode, otherDate))
        .expectNextMatches(dto -> covers(dto, otherDate))
        .verifyComplete();

    verifyCall(officeCode, missingDate, 1);
    verifyCall(officeCode, otherDate, 1);
  }

  private Mono<ProviderFirmOfficeContractAndScheduleDto> call(
      String officeCode, LocalDate effectiveDate) {
    return providerDetailsService.getProviderFirmSchedules(officeCode, AREA_OF_LAW, effectiveDate);
  }

  private void stubCoverage(
      String officeCode, LocalDate effectiveDate, LocalDate start, LocalDate end) throws Exception {
    ProviderFirmOfficeContractAndScheduleDto dto = new ProviderFirmOfficeContractAndScheduleDto();
    dto.setOffice(ProviderFirmOfficeSummary.builder().firmOfficeCode(officeCode).build());
    FirmOfficeContractAndScheduleDetails schedule =
        FirmOfficeContractAndScheduleDetails.builder()
            .scheduleStartDate(start)
            .scheduleEndDate(end)
            .build();
    dto.setSchedules(List.of(schedule));
    String body = objectMapper.writeValueAsString(dto);
    mockServerClient
        .when(
            request()
                .withMethod("GET")
                .withPath("/api/v1/provider-offices/" + officeCode + "/schedules")
                .withQueryStringParameters(
                    new Parameter("areaOfLaw", AREA_OF_LAW),
                    new Parameter("effectiveDate", FORMATTER.format(effectiveDate))),
            Times.exactly(1))
        .respond(
            HttpResponse.response()
                .withStatusCode(HttpStatusCode.OK)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(body));
  }

  private void stubNegative(String officeCode, LocalDate effectiveDate) {
    mockServerClient
        .when(
            request()
                .withMethod("GET")
                .withPath("/api/v1/provider-offices/" + officeCode + "/schedules")
                .withQueryStringParameters(
                    new Parameter("areaOfLaw", AREA_OF_LAW),
                    new Parameter("effectiveDate", FORMATTER.format(effectiveDate))),
            Times.exactly(1))
        .respond(
            HttpResponse.response()
                .withStatusCode(HttpStatusCode.NO_CONTENT)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE));
  }

  private void verifyCall(String officeCode, LocalDate effectiveDate, int times) {
    mockServerClient.verify(
        request()
            .withMethod("GET")
            .withPath("/api/v1/provider-offices/" + officeCode + "/schedules")
            .withQueryStringParameters(
                new Parameter("areaOfLaw", AREA_OF_LAW),
                new Parameter("effectiveDate", FORMATTER.format(effectiveDate))),
        VerificationTimes.exactly(times));
  }

  private boolean covers(ProviderFirmOfficeContractAndScheduleDto dto, LocalDate effectiveDate) {
    return dto.getSchedules().stream()
        .anyMatch(
            schedule ->
                !effectiveDate.isBefore(schedule.getScheduleStartDate())
                    && !effectiveDate.isAfter(schedule.getScheduleEndDate()));
  }
}
