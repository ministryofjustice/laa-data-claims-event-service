package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.mockserver.model.HttpRequest.request;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MessageListenerBase;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;
import uk.gov.justice.laadata.providers.model.FirmOfficeContractAndScheduleDetails;
import uk.gov.justice.laadata.providers.model.ProviderFirmOfficeContractAndScheduleDto;
import uk.gov.justice.laadata.providers.model.ProviderFirmOfficeSummary;

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
@DisplayName("Provider details service per-claim integration tests")
class ProviderDetailsServicePerClaimIntegrationTest extends MockServerIntegrationTest {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

  @Autowired ProviderDetailsService providerDetailsService;

  @Nested
  @DisplayName("Per-invocation Provider Details calls")
  class PerInvocationCalls {

    @Test
    @DisplayName("calls Provider Details for every invocation, including the same date")
    void shouldCallProviderDetailsForEveryInvocationIncludingSameDate() throws Exception {
      String officeCode = "PER_CLAIM_CALLS";
      LocalDate effectiveDate = LocalDate.of(2025, 6, 1);
      stubSchedules(
          officeCode, effectiveDate, List.of(schedule(LocalDate.of(2025, 1, 1), null, "CIVIL")), 2);

      StepVerifier.create(
              providerDetailsService.getProviderFirmSchedules(officeCode, effectiveDate))
          .expectNextCount(1)
          .verifyComplete();
      StepVerifier.create(
              providerDetailsService.getProviderFirmSchedules(officeCode, effectiveDate))
          .expectNextCount(1)
          .verifyComplete();

      verifyCall(officeCode, effectiveDate, 2);
    }

    @Test
    @DisplayName("calls Provider Details separately for each effective date")
    void shouldCallProviderDetailsForEachEffectiveDate() throws Exception {
      String officeCode = "DIFFERENT_DATES";
      LocalDate civilDate = LocalDate.of(2025, 6, 1);
      LocalDate crimeDate = LocalDate.of(2026, 7, 1);
      stubSchedules(
          officeCode, civilDate, List.of(schedule(LocalDate.of(2025, 1, 1), null, "CIVIL")), 1);
      stubSchedules(
          officeCode,
          crimeDate,
          List.of(schedule(LocalDate.of(2025, 10, 1), null, "CRIME LOWER")),
          1);

      StepVerifier.create(providerDetailsService.getProviderFirmSchedules(officeCode, civilDate))
          .expectNextMatches(dto -> containsAreaOfLaw(dto, "CIVIL"))
          .verifyComplete();
      StepVerifier.create(providerDetailsService.getProviderFirmSchedules(officeCode, crimeDate))
          .expectNextMatches(
              dto -> containsAreaOfLaw(dto, "CRIME LOWER") && !containsAreaOfLaw(dto, "CIVIL"))
          .verifyComplete();

      verifyCall(officeCode, civilDate, 1);
      verifyCall(officeCode, crimeDate, 1);
    }

    @Test
    @DisplayName("calls Provider Details again after a no-content response")
    void shouldCallProviderDetailsAgainAfterNoContentResponse() throws Exception {
      String officeCode = "NO_CONTENT";
      LocalDate effectiveDate = LocalDate.of(2025, 6, 1);
      stubNegative(officeCode, effectiveDate, 1);
      stubSchedules(
          officeCode, effectiveDate, List.of(schedule(LocalDate.of(2025, 1, 1), null, "CIVIL")), 1);

      StepVerifier.create(
              providerDetailsService.getProviderFirmSchedules(officeCode, effectiveDate))
          .verifyComplete();
      StepVerifier.create(
              providerDetailsService.getProviderFirmSchedules(officeCode, effectiveDate))
          .expectNextCount(1)
          .verifyComplete();

      verifyCall(officeCode, effectiveDate, 2);
    }
  }

  @Nested
  @DisplayName("Mixed effective-date schedule isolation")
  class MixedEffectiveDateScheduleIsolation {

    @ParameterizedTest(name = "later date {0} receives its own schedule")
    @MethodSource(
        "uk.gov.justice.laa.dstew.payments.claimsevent.service.ProviderDetailsServicePerClaimIntegrationTest#laterCrimeDatedClaimDatesProvider")
    @DisplayName("uses the response for the requested effective date")
    void shouldUseResponseForRequestedEffectiveDate(LocalDate crimeDate) throws Exception {
      String officeCode = "INCIDENT_2Q949Z_" + crimeDate;
      LocalDate civilDate = LocalDate.of(2025, 6, 1);
      stubSchedules(
          officeCode, civilDate, List.of(schedule(LocalDate.of(2025, 1, 1), null, "CIVIL")), 1);
      stubSchedules(
          officeCode,
          crimeDate,
          List.of(schedule(LocalDate.of(2025, 10, 1), null, "CRIME LOWER")),
          1);

      StepVerifier.create(providerDetailsService.getProviderFirmSchedules(officeCode, civilDate))
          .expectNextMatches(dto -> containsAreaOfLaw(dto, "CIVIL"))
          .verifyComplete();
      StepVerifier.create(providerDetailsService.getProviderFirmSchedules(officeCode, crimeDate))
          .expectNextMatches(
              dto -> containsAreaOfLaw(dto, "CRIME LOWER") && !containsAreaOfLaw(dto, "CIVIL"))
          .verifyComplete();

      verifyCall(officeCode, civilDate, 1);
      verifyCall(officeCode, crimeDate, 1);
    }

    @Test
    @DisplayName("produces the same date-specific outcomes regardless of processing order")
    void shouldProduceSameOutcomesRegardlessOfProcessingOrder() throws Exception {
      String forwardOffice = "ORDER_FWD_2Q949Z";
      String reverseOffice = "ORDER_REV_2Q949Z";
      LocalDate civilDate = LocalDate.of(2025, 6, 1);
      List<LocalDate> crimeDates =
          List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 31));

      for (String office : List.of(forwardOffice, reverseOffice)) {
        stubSchedules(
            office, civilDate, List.of(schedule(LocalDate.of(2025, 1, 1), null, "CIVIL")), 1);
        for (LocalDate crimeDate : crimeDates) {
          stubSchedules(
              office,
              crimeDate,
              List.of(schedule(LocalDate.of(2025, 10, 1), null, "CRIME LOWER")),
              1);
        }
      }

      List<String> forwardOutcomes = new ArrayList<>();
      forwardOutcomes.add(
          ProviderDetailsServicePerClaimIntegrationTest.this
              .call(forwardOffice, civilDate)
              .map(ProviderDetailsServicePerClaimIntegrationTest.this::areaOfLaw)
              .block());
      for (LocalDate crimeDate : crimeDates) {
        forwardOutcomes.add(
            ProviderDetailsServicePerClaimIntegrationTest.this
                .call(forwardOffice, crimeDate)
                .map(ProviderDetailsServicePerClaimIntegrationTest.this::areaOfLaw)
                .block());
      }

      List<LocalDate> reverseDates = new ArrayList<>(crimeDates);
      Collections.reverse(reverseDates);
      List<String> reverseOutcomes = new ArrayList<>();
      for (LocalDate crimeDate : reverseDates) {
        reverseOutcomes.add(
            ProviderDetailsServicePerClaimIntegrationTest.this
                .call(reverseOffice, crimeDate)
                .map(ProviderDetailsServicePerClaimIntegrationTest.this::areaOfLaw)
                .block());
      }
      reverseOutcomes.add(
          ProviderDetailsServicePerClaimIntegrationTest.this
              .call(reverseOffice, civilDate)
              .map(ProviderDetailsServicePerClaimIntegrationTest.this::areaOfLaw)
              .block());
      Collections.reverse(reverseOutcomes);

      Assertions.assertThat(forwardOutcomes).containsExactlyElementsOf(reverseOutcomes);
      Assertions.assertThat(forwardOutcomes)
          .containsExactly("CIVIL", "CRIME LOWER", "CRIME LOWER", "CRIME LOWER");
    }
  }

  private void stubSchedules(
      String officeCode,
      LocalDate effectiveDate,
      List<FirmOfficeContractAndScheduleDetails> schedules,
      int times)
      throws Exception {
    ProviderFirmOfficeContractAndScheduleDto dto = new ProviderFirmOfficeContractAndScheduleDto();
    dto.setOffice(ProviderFirmOfficeSummary.builder().firmOfficeCode(officeCode).build());
    dto.setSchedules(schedules);
    mockServerClient
        .when(
            request()
                .withMethod("GET")
                .withPath("/api/v1/provider-offices/" + officeCode + "/schedules")
                .withQueryStringParameters(
                    new Parameter("effectiveDate", FORMATTER.format(effectiveDate))),
            Times.exactly(times))
        .respond(
            HttpResponse.response()
                .withStatusCode(HttpStatusCode.OK)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(dto)));
  }

  private void stubNegative(String officeCode, LocalDate effectiveDate, int times) {
    mockServerClient
        .when(
            request()
                .withMethod("GET")
                .withPath("/api/v1/provider-offices/" + officeCode + "/schedules")
                .withQueryStringParameters(
                    new Parameter("effectiveDate", FORMATTER.format(effectiveDate))),
            Times.exactly(times))
        .respond(HttpResponse.response().withStatusCode(HttpStatusCode.NO_CONTENT));
  }

  private void verifyCall(String officeCode, LocalDate effectiveDate, int times) {
    mockServerClient.verify(
        request()
            .withMethod("GET")
            .withPath("/api/v1/provider-offices/" + officeCode + "/schedules")
            .withQueryStringParameters(
                new Parameter("effectiveDate", FORMATTER.format(effectiveDate))),
        VerificationTimes.exactly(times));
  }

  private FirmOfficeContractAndScheduleDetails schedule(
      LocalDate start, LocalDate end, String areaOfLaw) {
    return FirmOfficeContractAndScheduleDetails.builder()
        .scheduleStartDate(start)
        .scheduleEndDate(end)
        .areaOfLaw(areaOfLaw)
        .build();
  }

  private boolean containsAreaOfLaw(
      ProviderFirmOfficeContractAndScheduleDto dto, String areaOfLaw) {
    return dto.getSchedules().stream()
        .anyMatch(schedule -> areaOfLaw.equals(schedule.getAreaOfLaw()));
  }

  private Mono<ProviderFirmOfficeContractAndScheduleDto> call(
      String officeCode, LocalDate effectiveDate) {
    return providerDetailsService.getProviderFirmSchedules(officeCode, effectiveDate);
  }

  private String areaOfLaw(ProviderFirmOfficeContractAndScheduleDto dto) {
    return dto.getSchedules().stream()
        .map(FirmOfficeContractAndScheduleDetails::getAreaOfLaw)
        .findFirst()
        .orElseThrow();
  }

  static Stream<LocalDate> laterCrimeDatedClaimDatesProvider() {
    return Stream.of(
        LocalDate.of(2026, 7, 1),
        LocalDate.of(2026, 7, 10),
        LocalDate.of(2026, 7, 20),
        LocalDate.of(2026, 7, 31));
  }
}
