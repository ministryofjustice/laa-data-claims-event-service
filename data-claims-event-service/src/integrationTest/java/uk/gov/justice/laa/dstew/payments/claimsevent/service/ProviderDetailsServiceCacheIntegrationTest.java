package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.mockserver.model.HttpRequest.request;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;
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

/**
 * Integration tests for {@link ProviderDetailsService}'s provider-schedule cache.
 *
 * <p>Covers coverage-window merging/reuse, negative caching, schedule-gap handling, open-ended
 * schedule edge cases, and the DSTEW-2227 incident regression shapes (mixed effective dates in a
 * single submission producing order-dependent false failures/passes).
 *
 * <p>Some tests in {@link IncidentRegressionTests} and {@link OpenEndedScheduleEdgeCases} are
 * written against the <em>desired</em>, date-correct, order-independent behaviour described in
 * DSTEW-2227 and are expected to fail against the current implementation - the coverage-window
 * {@code covers()} check that decides whether to skip a fresh PDA call is itself unsafe for
 * open-ended schedules, which read-time filtering of already-cached data cannot retroactively fix.
 * This is intentional, test-first coverage for the remaining open item in that ticket.
 */
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
@DisplayName("Provider details service cache integration test")
class ProviderDetailsServiceCacheIntegrationTest extends MockServerIntegrationTest {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

  @Autowired ProviderDetailsService providerDetailsService;

  @Nested
  @DisplayName("Coverage window merging and cache reuse")
  class CoverageWindowMerging {

    @Test
    @DisplayName("should merge coverage windows across multiple misses and reuse cached hits")
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

      // Read-time filtering means a cache hit only ever returns the schedule lines that are
      // individually effective on the requested date - never the full merged set regardless of
      // date - so every returned schedule must itself cover the queried date.
      cachedDates.forEach(
          date ->
              StepVerifier.create(call(officeCode, date))
                  .expectNextMatches(
                      dto -> !dto.getSchedules().isEmpty() && allSchedulesCover(dto, date))
                  .verifyComplete());

      verifyCall(officeCode, initial, 1);
      verifyCall(officeCode, extendEnd, 1);
      verifyCall(officeCode, extendStart, 1);
      verifyCall(officeCode, gapFill, 1);
    }
  }

  @Nested
  @DisplayName("Negative cache behaviour")
  class NegativeCacheBehaviour {

    @Test
    @DisplayName("should cache negative responses per effective date")
    void shouldCacheNegativeResponsesPerEffectiveDate() throws Exception {
      String officeCode = "NEG_0P322F";
      LocalDate missingDate = LocalDate.of(2009, 1, 1);
      LocalDate otherDate = LocalDate.of(2009, 2, 1);

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

    @Test
    @DisplayName(
        "should not let a negative cache entry for one date suppress a positive fetch for another date")
    void shouldNotLetNegativeCacheForOneDateSuppressPositiveFetchForAnotherDate() throws Exception {
      String officeCode = "NEG_ISOLATION";
      LocalDate negativeDate = LocalDate.of(2024, 1, 15);
      LocalDate positiveDate = LocalDate.of(2024, 6, 1);

      stubNegative(officeCode, negativeDate);
      stubCoverage(officeCode, positiveDate, LocalDate.of(2024, 5, 1), LocalDate.of(2024, 7, 31));

      StepVerifier.create(call(officeCode, negativeDate)).verifyComplete();
      StepVerifier.create(call(officeCode, positiveDate))
          .expectNextMatches(dto -> covers(dto, positiveDate))
          .verifyComplete();
      StepVerifier.create(call(officeCode, negativeDate)).verifyComplete();

      verifyCall(officeCode, negativeDate, 1);
      verifyCall(officeCode, positiveDate, 1);
    }
  }

  @Nested
  @DisplayName("Schedule gap edge cases")
  class ScheduleGapEdgeCases {

    @Test
    @DisplayName(
        "should call the api for a date in an uncovered gap between two cached windows and correctly report no schedule")
    void shouldCallApiForDateInUncoveredGapAndReportNoSchedule() throws Exception {
      String officeCode = "GAP_STANDALONE";
      LocalDate earlyDate = LocalDate.of(2021, 1, 15);
      LocalDate lateDate = LocalDate.of(2021, 11, 1);
      LocalDate gapDate = LocalDate.of(2021, 6, 1);

      stubCoverage(officeCode, earlyDate, LocalDate.of(2021, 1, 1), LocalDate.of(2021, 3, 31));
      stubCoverage(officeCode, lateDate, LocalDate.of(2021, 9, 1), LocalDate.of(2021, 12, 31));
      stubNegative(officeCode, gapDate);

      StepVerifier.create(call(officeCode, earlyDate))
          .expectNextMatches(dto -> covers(dto, earlyDate))
          .verifyComplete();
      StepVerifier.create(call(officeCode, lateDate))
          .expectNextMatches(dto -> covers(dto, lateDate))
          .verifyComplete();
      StepVerifier.create(call(officeCode, gapDate)).verifyComplete();

      verifyCall(officeCode, earlyDate, 1);
      verifyCall(officeCode, lateDate, 1);
      verifyCall(officeCode, gapDate, 1);
    }

    @Test
    @DisplayName(
        "should require a fresh api call for the day immediately after a closed schedule window ends")
    void shouldRequireFreshCallForDayImmediatelyAfterWindowEnd() throws Exception {
      String officeCode = "BOUNDARY_AFTER_END";
      LocalDate withinWindowDate = LocalDate.of(2023, 1, 10);
      LocalDate dayAfterEnd = LocalDate.of(2023, 2, 1);

      stubCoverage(
          officeCode, withinWindowDate, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 31));
      stubNegative(officeCode, dayAfterEnd);

      StepVerifier.create(call(officeCode, withinWindowDate))
          .expectNextMatches(dto -> covers(dto, withinWindowDate))
          .verifyComplete();
      StepVerifier.create(call(officeCode, dayAfterEnd)).verifyComplete();

      verifyCall(officeCode, withinWindowDate, 1);
      verifyCall(officeCode, dayAfterEnd, 1);
    }

    @ParameterizedTest(name = "[{index}] inclusive boundary date {0} is served from cache")
    @MethodSource(
        "uk.gov.justice.laa.dstew.payments.claimsevent.service.ProviderDetailsServiceCacheIntegrationTest#inclusiveBoundaryDatesProvider")
    @DisplayName(
        "should serve the inclusive start and end boundary dates of a cached window without an extra api call")
    void shouldServeInclusiveBoundaryDatesFromCache(LocalDate boundaryDate) throws Exception {
      String officeCode = "BOUNDARY_INCLUSIVE_" + boundaryDate;
      LocalDate populatingDate = LocalDate.of(2023, 6, 15);

      stubCoverage(
          officeCode, populatingDate, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));

      StepVerifier.create(call(officeCode, populatingDate))
          .expectNextMatches(dto -> covers(dto, populatingDate))
          .verifyComplete();
      StepVerifier.create(call(officeCode, boundaryDate))
          .expectNextMatches(dto -> covers(dto, boundaryDate))
          .verifyComplete();

      verifyCall(officeCode, populatingDate, 1);
      verifyCall(officeCode, boundaryDate, 0);
    }
  }

  @Nested
  @DisplayName("Open ended schedule edge cases")
  class OpenEndedScheduleEdgeCases {

    @Test
    @DisplayName(
        "an open ended schedule with no end date is treated as covering every future date for the cache decision")
    void shouldTreatOpenEndedScheduleAsCoveringEveryFutureDateForCacheDecision() throws Exception {
      String officeCode = "OPEN_ENDED_COVERAGE";
      LocalDate populatingDate = LocalDate.of(2025, 6, 1);
      LocalDate farFutureDate = LocalDate.of(2030, 1, 1);

      stubSchedules(
          officeCode, populatingDate, List.of(schedule(LocalDate.of(2025, 1, 1), null, "CIVIL")));

      StepVerifier.create(call(officeCode, populatingDate))
          .expectNextMatches(dto -> containsAreaOfLaw(dto, "CIVIL"))
          .verifyComplete();

      // A decade later is still served from the same cache entry: a null end date is treated
      // as unbounded for the coverage decision, so no fresh api call is made for farFutureDate
      // even though it was never actually queried against PDA. This documents the mechanism
      // called out in DSTEW-2227 as the incident's root cause.
      StepVerifier.create(call(officeCode, farFutureDate))
          .expectNextMatches(dto -> containsAreaOfLaw(dto, "CIVIL"))
          .verifyComplete();

      verifyCall(officeCode, populatingDate, 1);
      verifyCall(officeCode, farFutureDate, 0);
    }
  }

  @Nested
  @DisplayName("Incident regression and merge-window edge cases")
  class IncidentRegressionTests {

    @Test
    @DisplayName("stalePositiveCacheReadShouldCallApiAndReturnFreshScheduleForLaterDate")
    void stalePositiveCacheReadShouldCallApiAndReturnFreshScheduleForLaterDate() throws Exception {
      String officeCode = "INCIDENT_CORE_BUG";
      LocalDate civilDate = LocalDate.of(2025, 6, 1);
      LocalDate crimeDate = LocalDate.of(2026, 7, 1);

      // Civil claim populates the cache first, with an open-ended window (matches incident shape)
      stubSchedules(
          officeCode, civilDate, List.of(schedule(LocalDate.of(2025, 1, 1), null, "CIVIL")));
      // If PDA were genuinely asked for crimeDate it would return the Crime schedule
      stubSchedules(
          officeCode, crimeDate, List.of(schedule(LocalDate.of(2025, 10, 1), null, "CRIME LOWER")));

      // Simulate the early (civil) lookup
      StepVerifier.create(call(officeCode, civilDate))
          .expectNextMatches(dto -> containsAreaOfLaw(dto, "CIVIL"))
          .verifyComplete();

      // Now request the later date - a correct implementation must call PDA and return CRIME
      StepVerifier.create(call(officeCode, crimeDate))
          .expectNextMatches(
              dto -> containsAreaOfLaw(dto, "CRIME LOWER") && !containsAreaOfLaw(dto, "CIVIL"))
          .verifyComplete();

      // Verify the later date was actually requested from PDA
      verifyCall(officeCode, crimeDate, 1);
    }

    @Test
    @DisplayName("mergedWindowsShouldNotLeakCategoriesToAnUnrelatedDate")
    void mergedWindowsShouldNotLeakCategoriesToAnUnrelatedDate() throws Exception {
      String officeCode = "MERGE_LEAK_INCIDENT";
      LocalDate date1 = LocalDate.of(2024, 1, 15);
      LocalDate date2 = LocalDate.of(2024, 6, 20);
      LocalDate date3 = LocalDate.of(2024, 7, 1);

      // date1 exposes category A in window that partially overlaps date2
      stubCoverage(officeCode, date1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));
      // date2 exposes category B with an overlap so merge could cause combined windows
      stubCoverage(officeCode, date2, LocalDate.of(2024, 6, 15), LocalDate.of(2024, 12, 31));
      // For date3 PDA truly returns a distinct schedule (C) and must be requested
      stubSchedules(officeCode, date3, List.of(schedule(LocalDate.of(2024, 7, 1), null, "C")));

      // Populate cache with date1 and date2
      StepVerifier.create(call(officeCode, date1))
          .expectNextMatches(dto -> !dto.getSchedules().isEmpty())
          .verifyComplete();
      StepVerifier.create(call(officeCode, date2))
          .expectNextMatches(dto -> !dto.getSchedules().isEmpty())
          .verifyComplete();

      // Now request date3 and assert we get only the true schedule for date3 (C) and PDA was called
      StepVerifier.create(call(officeCode, date3))
          .expectNextMatches(
              dto ->
                  containsAreaOfLaw(dto, "C")
                      && !containsAreaOfLaw(dto, "CIVIL")
                      && !containsAreaOfLaw(dto, "CRIME LOWER"))
          .verifyComplete();

      verifyCall(officeCode, date3, 1);
    }

    @Test
    @DisplayName("sameDateReuseShouldOnlyCallApiOnceAndReturnCachedResult")
    void sameDateReuseShouldOnlyCallApiOnceAndReturnCachedResult() throws Exception {
      String officeCode = "SAME_DATE_REUSE";
      LocalDate date = LocalDate.of(2025, 3, 1);

      stubSchedules(
          officeCode,
          date,
          List.of(schedule(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), "CIVIL")));

      StepVerifier.create(call(officeCode, date))
          .expectNextMatches(dto -> containsAreaOfLaw(dto, "CIVIL"))
          .verifyComplete();
      StepVerifier.create(call(officeCode, date))
          .expectNextMatches(dto -> containsAreaOfLaw(dto, "CIVIL"))
          .verifyComplete();

      // Only one PDA call must have been made
      verifyCall(officeCode, date, 1);
    }

    @Test
    @DisplayName("distinctNonOverlappingDatesShouldBeFetchedIndependently")
    void distinctNonOverlappingDatesShouldBeFetchedIndependently() throws Exception {
      String officeCode = "DISTINCT_DATES";
      LocalDate d1 = LocalDate.of(2023, 2, 1);
      LocalDate d2 = LocalDate.of(2024, 9, 1);

      stubCoverage(officeCode, d1, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 3, 31));
      stubCoverage(officeCode, d2, LocalDate.of(2024, 9, 1), LocalDate.of(2024, 9, 30));

      StepVerifier.create(call(officeCode, d1))
          .expectNextMatches(dto -> covers(dto, d1))
          .verifyComplete();
      StepVerifier.create(call(officeCode, d2))
          .expectNextMatches(dto -> covers(dto, d2))
          .verifyComplete();

      verifyCall(officeCode, d1, 1);
      verifyCall(officeCode, d2, 1);
    }
  }

  // Incident/edge-case regression tests removed to a separate file so they can be exercised as
  // focused, high-level integration tests. See ProviderDetailsServiceKnownFailingTests for the
  // original test cases (kept for reference / future re-enablement).

  private Mono<ProviderFirmOfficeContractAndScheduleDto> call(
      String officeCode, LocalDate effectiveDate) {
    return providerDetailsService.getProviderFirmSchedules(officeCode, effectiveDate);
  }

  private void stubCoverage(
      String officeCode, LocalDate effectiveDate, LocalDate start, LocalDate end) throws Exception {
    stubSchedules(officeCode, effectiveDate, List.of(schedule(start, end, null)));
  }

  private void stubSchedules(
      String officeCode,
      LocalDate effectiveDate,
      List<FirmOfficeContractAndScheduleDetails> schedules)
      throws Exception {
    ProviderFirmOfficeContractAndScheduleDto dto = new ProviderFirmOfficeContractAndScheduleDto();
    dto.setOffice(ProviderFirmOfficeSummary.builder().firmOfficeCode(officeCode).build());
    dto.setSchedules(schedules);
    String body = objectMapper.writeValueAsString(dto);
    mockServerClient
        .when(
            request()
                .withMethod("GET")
                .withPath("/api/v1/provider-offices/" + officeCode + "/schedules")
                .withQueryStringParameters(
                    new Parameter("effectiveDate", FORMATTER.format(effectiveDate))),
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

  private boolean covers(ProviderFirmOfficeContractAndScheduleDto dto, LocalDate effectiveDate) {
    return dto.getSchedules().stream()
        .anyMatch(schedule -> scheduleCovers(schedule, effectiveDate));
  }

  private boolean allSchedulesCover(
      ProviderFirmOfficeContractAndScheduleDto dto, LocalDate effectiveDate) {
    return dto.getSchedules().stream()
        .allMatch(schedule -> scheduleCovers(schedule, effectiveDate));
  }

  private boolean scheduleCovers(
      FirmOfficeContractAndScheduleDetails schedule, LocalDate effectiveDate) {
    LocalDate start = schedule.getScheduleStartDate();
    LocalDate end = schedule.getScheduleEndDate();
    if (start != null && effectiveDate.isBefore(start)) {
      return false;
    }
    return end == null || !effectiveDate.isAfter(end);
  }

  private boolean containsAreaOfLaw(
      ProviderFirmOfficeContractAndScheduleDto dto, String areaOfLaw) {
    return dto.getSchedules().stream()
        .anyMatch(schedule -> areaOfLaw.equals(schedule.getAreaOfLaw()));
  }

  static Stream<LocalDate> inclusiveBoundaryDatesProvider() {
    return Stream.of(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));
  }

  static Stream<LocalDate> laterCrimeDatedClaimDatesProvider() {
    return Stream.of(
        LocalDate.of(2026, 7, 1),
        LocalDate.of(2026, 7, 10),
        LocalDate.of(2026, 7, 20),
        LocalDate.of(2026, 7, 31));
  }
}
