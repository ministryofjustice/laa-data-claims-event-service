package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.mockserver.model.HttpRequest.request;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
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

/**
 * Legacy duplicate incident and effective-date tests retained temporarily while the active
 * per-claim integration coverage is consolidated. These tests are disabled and should not be used
 * as the primary source of regression coverage.
 */
@ActiveProfiles("test")
@ImportTestcontainers(MessageListenerBase.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(MockServerIntegrationTest.ClaimsConfiguration.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.cloud.aws.sqs.enabled=false", "laa.bulk-claim-queue.name=not-used"})
@DisplayName("Provider details service - legacy duplicate tests (disabled)")
@Disabled(
    "Legacy duplicate fixture; active coverage is maintained in ProviderDetailsServicePerClaimIntegrationTest")
class ProviderDetailsServiceLegacyDuplicateTests extends MockServerIntegrationTest {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

  @Autowired ProviderDetailsService providerDetailsService;

  @Nested
  @DisplayName("Open ended schedule edge cases (desired behaviour)")
  class OpenEndedScheduleEdgeCases {

    @Test
    @DisplayName(
        "should not use an open ended earlier schedule to answer a later date that genuinely has a different schedule")
    void shouldNotUseOpenEndedEarlierScheduleForLaterDateWithGenuinelyDifferentSchedule()
        throws Exception {
      String officeCode = "OPEN_ENDED_INCIDENT";
      LocalDate civilDate = LocalDate.of(2025, 6, 1);
      LocalDate crimeDate = LocalDate.of(2026, 7, 1);

      // Civil claim populates the cache first, with an open-ended window (matches incident
      // shape).
      stubSchedules(
          officeCode, civilDate, List.of(schedule(LocalDate.of(2025, 1, 1), null, "CIVIL")));
      // If PDA were genuinely asked for crimeDate it would return the Crime schedule - the bug
      // is that PDA is never asked, because the open-ended Civil window is (incorrectly) treated
      // as already covering crimeDate.
      stubSchedules(
          officeCode, crimeDate, List.of(schedule(LocalDate.of(2025, 10, 1), null, "CRIME LOWER")));

      StepVerifier.create(call(officeCode, civilDate))
          .expectNextMatches(dto -> containsAreaOfLaw(dto, "CIVIL"))
          .verifyComplete();

      StepVerifier.create(call(officeCode, crimeDate))
          .expectNextMatches(
              dto -> containsAreaOfLaw(dto, "CRIME LOWER") && !containsAreaOfLaw(dto, "CIVIL"))
          .verifyComplete();

      verifyCall(officeCode, crimeDate, 1);
    }

    @Test
    @DisplayName(
        "should not use an open ended earlier schedule to answer an earlier date before that schedule started")
    void shouldNotUseOpenEndedEarlierScheduleForDateBeforeItStarted() throws Exception {
      String officeCode = "OPEN_ENDED_BEFORE_START";
      LocalDate populatingDate = LocalDate.of(2025, 6, 1);
      LocalDate beforeStartDate = LocalDate.of(2024, 1, 1);

      stubSchedules(
          officeCode, populatingDate, List.of(schedule(LocalDate.of(2025, 1, 1), null, "CIVIL")));
      stubNegative(officeCode, beforeStartDate);

      StepVerifier.create(call(officeCode, populatingDate))
          .expectNextMatches(dto -> containsAreaOfLaw(dto, "CIVIL"))
          .verifyComplete();
      StepVerifier.create(call(officeCode, beforeStartDate)).verifyComplete();

      verifyCall(officeCode, populatingDate, 1);
      verifyCall(officeCode, beforeStartDate, 1);
    }
  }

  @Nested
  @DisplayName("DSTEW-2227 incident regression: mixed effective dates in one submission (desired)")
  class IncidentRegression {

    @ParameterizedTest(
        name =
            "[{index}] later claim dated {0} should be authorised under its own crime schedule, not the earlier cached civil schedule")
    @MethodSource(
        "uk.gov.justice.laa.dstew.payments.claimsevent.service.ProviderDetailsServicePerClaimIntegrationTest#laterCrimeDatedClaimDatesProvider")
    @DisplayName(
        "incident shape: later dated claims must resolve to their own effective schedule, not an earlier cached one")
    void shouldResolveIncidentShapeLaterClaimsToTheirOwnSchedule(LocalDate laterClaimDate)
        throws Exception {
      String officeCode = "INCIDENT_2Q949Z_" + laterClaimDate;
      LocalDate earlierClaimDate = LocalDate.of(2025, 6, 1);

      stubSchedules(
          officeCode, earlierClaimDate, List.of(schedule(LocalDate.of(2025, 1, 1), null, "CIVIL")));
      stubSchedules(
          officeCode,
          laterClaimDate,
          List.of(schedule(LocalDate.of(2025, 10, 1), null, "CRIME LOWER")));

      // Simulates the incident file's first five (Civil) rows being processed before this later
      // (Crime) claim.
      for (int i = 0; i < 5; i++) {
        StepVerifier.create(call(officeCode, earlierClaimDate))
            .expectNextMatches(dto -> containsAreaOfLaw(dto, "CIVIL"))
            .verifyComplete();
      }

      StepVerifier.create(call(officeCode, laterClaimDate))
          .expectNextMatches(dto -> containsAreaOfLaw(dto, "CRIME LOWER"))
          .verifyComplete();
    }

    @Test
    @DisplayName(
        "should give the same per date outcome regardless of whether claims are processed forwards or in reverse order")
    void shouldGiveSameOutcomeRegardlessOfProcessingOrder() throws Exception {
      String forwardOffice = "ORDER_FWD_2Q949Z";
      String reverseOffice = "ORDER_REV_2Q949Z";
      LocalDate civilDate = LocalDate.of(2025, 6, 1);
      List<LocalDate> crimeDates =
          List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 31));

      // Identical underlying PDA data behind both office variants - only processing order
      // differs between the two.
      for (String office : List.of(forwardOffice, reverseOffice)) {
        stubSchedules(
            office, civilDate, List.of(schedule(LocalDate.of(2025, 1, 1), null, "CIVIL")));
        for (LocalDate crimeDate : crimeDates) {
          stubSchedules(
              office, crimeDate, List.of(schedule(LocalDate.of(2025, 10, 1), null, "CRIME LOWER")));
        }
      }

      // Forward: earlier Civil claim first, then later Crime claims in date order (incident
      // shape).
      StepVerifier.create(call(forwardOffice, civilDate))
          .expectNextMatches(dto -> containsAreaOfLaw(dto, "CIVIL"))
          .verifyComplete();
      List<Boolean> forwardCrimeOutcomes = new ArrayList<>();
      for (LocalDate crimeDate : crimeDates) {
        forwardCrimeOutcomes.add(
            Boolean.TRUE.equals(
                call(forwardOffice, crimeDate)
                    .map(dto -> containsAreaOfLaw(dto, "CRIME LOWER"))
                    .block()));
      }

      // Reverse: later Crime claims first (in reverse date order), earlier Civil claim last.
      List<LocalDate> reversedCrimeDates = new ArrayList<>(crimeDates);
      Collections.reverse(reversedCrimeDates);
      List<Boolean> reverseCrimeOutcomes = new ArrayList<>();
      for (LocalDate crimeDate : reversedCrimeDates) {
        reverseCrimeOutcomes.add(
            Boolean.TRUE.equals(
                call(reverseOffice, crimeDate)
                    .map(dto -> containsAreaOfLaw(dto, "CRIME LOWER"))
                    .block()));
      }
      Boolean reverseCivilOutcome =
          call(reverseOffice, civilDate).map(dto -> containsAreaOfLaw(dto, "CIVIL")).block();

      // Every Crime claim must be authorised under the Crime schedule regardless of the order
      // claims were processed in - order-independence is the core acceptance criterion here.
      Assertions.assertThat(forwardCrimeOutcomes).containsOnly(true);
      Assertions.assertThat(reverseCrimeOutcomes).containsOnly(true);
      Assertions.assertThat(reverseCivilOutcome).isTrue();
    }
  }

  private Mono<ProviderFirmOfficeContractAndScheduleDto> call(
      String officeCode, LocalDate effectiveDate) {
    return providerDetailsService.getProviderFirmSchedules(officeCode, effectiveDate);
  }

  private void stubSchedules(
      String officeCode,
      LocalDate effectiveDate,
      List<FirmOfficeContractAndScheduleDetails> schedules)
      throws Exception {
    ProviderFirmOfficeContractAndScheduleDto dto = new ProviderFirmOfficeContractAndScheduleDto();
    dto.setOffice(
        uk.gov.justice.laadata.providers.model.ProviderFirmOfficeSummary.builder()
            .firmOfficeCode(officeCode)
            .build());
    dto.setSchedules(schedules);
    String body = objectMapper.writeValueAsString(dto);
    mockServerClient
        .when(
            request()
                .withMethod("GET")
                .withPath("/api/v1/provider-offices/" + officeCode + "/schedules")
                .withQueryStringParameters(
                    new Parameter("effectiveDate", FORMATTER.format(effectiveDate)),
                    new Parameter("requireOpenStatus", "false")),
            Times.exactly(1))
        .respond(
            HttpResponse.response()
                .withStatusCode(HttpStatusCode.OK)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(body));
  }

  private FirmOfficeContractAndScheduleDetails schedule(
      LocalDate start, LocalDate end, String areaOfLaw) {
    return FirmOfficeContractAndScheduleDetails.builder()
        .scheduleStartDate(start)
        .scheduleEndDate(end)
        .areaOfLaw(areaOfLaw)
        .build();
  }

  private void stubNegative(String officeCode, LocalDate effectiveDate) {
    HttpResponse negativeResponse =
        HttpResponse.response()
            .withStatusCode(HttpStatusCode.NO_CONTENT)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    // Strict expectation with formatted query params
    mockServerClient
        .when(
            request()
                .withMethod("GET")
                .withPath("/api/v1/provider-offices/" + officeCode + "/schedules")
                .withQueryStringParameters(
                    new Parameter("effectiveDate", FORMATTER.format(effectiveDate))),
            Times.exactly(1))
        .respond(negativeResponse);
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

  private boolean containsAreaOfLaw(
      ProviderFirmOfficeContractAndScheduleDto dto, String areaOfLaw) {
    return dto.getSchedules().stream()
        .anyMatch(schedule -> areaOfLaw.equals(schedule.getAreaOfLaw()));
  }

  static Stream<LocalDate> laterCrimeDatedClaimDatesProvider() {
    return Stream.of(
        LocalDate.of(2026, 7, 1),
        LocalDate.of(2026, 7, 10),
        LocalDate.of(2026, 7, 20),
        LocalDate.of(2026, 7, 31));
  }
}
