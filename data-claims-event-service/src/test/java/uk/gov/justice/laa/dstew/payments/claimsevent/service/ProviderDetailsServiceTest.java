package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laadata.providers.model.FirmOfficeContractAndScheduleDetails;
import uk.gov.justice.laadata.providers.model.ProviderFirmOfficeContractAndScheduleDto;
import uk.gov.justice.laadata.providers.model.ProviderFirmOfficeSummary;

@ExtendWith(MockitoExtension.class)
class ProviderDetailsServiceTest {

  @Mock private ProviderDetailsRestClient client;

  @Mock private RetryRegistry retryRegistry;

  @InjectMocks private ProviderDetailsService service;

  @BeforeEach
  void setup() {
    Retry noOpRetry =
        Retry.of(
            "pdaRetry",
            RetryConfig.custom()
                .maxAttempts(1) // ✅ Only one attempt
                .build());
    when(retryRegistry.retry("pdaRetry")).thenReturn(noOpRetry);
  }

  @Test
  void testGetProviderFirmSchedules() {
    // Arrange
    String officeCode = "Office1";
    LocalDate effectiveDate = LocalDate.now();

    ProviderFirmOfficeContractAndScheduleDto expectedDto =
        ProviderFirmOfficeContractAndScheduleDto.builder()
            .office(ProviderFirmOfficeSummary.builder().firmOfficeCode(officeCode).build())
            .build();

    // Simulate first 2 calls fail, third succeeds
    when(client.getProviderFirmSchedules(anyString(), eq(effectiveDate)))
        .thenReturn(Mono.just(expectedDto));

    // Act
    Mono<ProviderFirmOfficeContractAndScheduleDto> result =
        service.getProviderFirmSchedules(officeCode, effectiveDate);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            dto -> {
              assertNotNull(dto.getOffice());
              assertNotNull(dto.getOffice().getFirmOfficeCode());
              return dto.getOffice().getFirmOfficeCode().equals(officeCode);
            })
        .verifyComplete();

    verify(client, times(1)).getProviderFirmSchedules(officeCode, effectiveDate);
  }

  @Test
  void testGetProviderFirmSchedules_withError() {
    // Arrange
    String officeCode = "OFF123";
    LocalDate effectiveDate = LocalDate.now();

    // Simulate all attempts fail
    when(client.getProviderFirmSchedules(officeCode, effectiveDate))
        .thenReturn(Mono.error(new RuntimeException("Temporary error")));

    // Act
    Mono<ProviderFirmOfficeContractAndScheduleDto> result =
        service.getProviderFirmSchedules(officeCode, effectiveDate);

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().contains("Temporary error"))
        .verify();

    verify(client, times(1)).getProviderFirmSchedules(officeCode, effectiveDate);
  }

  @Test
  void testGetProviderFirmSchedules_usesCacheWhenDateCovered() {
    String officeCode = "Office1";
    LocalDate effectiveDate = LocalDate.of(2024, 5, 1);

    ProviderFirmOfficeContractAndScheduleDto expectedDto =
        ProviderFirmOfficeContractAndScheduleDto.builder()
            .office(ProviderFirmOfficeSummary.builder().firmOfficeCode(officeCode).build())
            .schedules(
                List.of(
                    FirmOfficeContractAndScheduleDetails.builder()
                        .scheduleStartDate(LocalDate.of(2024, 1, 1))
                        .scheduleEndDate(LocalDate.of(2024, 12, 31))
                        .build()))
            .build();

    when(client.getProviderFirmSchedules(officeCode, effectiveDate))
        .thenReturn(Mono.just(expectedDto));

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, effectiveDate))
        .expectNext(expectedDto)
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, effectiveDate))
        .expectNext(expectedDto)
        .verifyComplete();

    verify(client, times(1)).getProviderFirmSchedules(officeCode, effectiveDate);
  }

  @Test
  void testGetProviderFirmSchedules_negativeCache() {
    String officeCode = "Office2";
    LocalDate effectiveDate = LocalDate.of(2024, 7, 1);

    when(client.getProviderFirmSchedules(officeCode, effectiveDate)).thenReturn(Mono.empty());

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, effectiveDate))
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, effectiveDate))
        .verifyComplete();

    verify(client, times(1)).getProviderFirmSchedules(officeCode, effectiveDate);
  }

  @Test
  void negativeCacheIsCheckedBeforePositiveCacheForDifferentDates() {
    String officeCode = "Office4";
    LocalDate positiveDate = LocalDate.of(2024, 1, 15);
    LocalDate negativeDate = LocalDate.of(2024, 6, 1);

    ProviderFirmOfficeContractAndScheduleDto januaryDto =
        ProviderFirmOfficeContractAndScheduleDto.builder()
            .office(ProviderFirmOfficeSummary.builder().firmOfficeCode(officeCode).build())
            .schedules(
                List.of(
                    FirmOfficeContractAndScheduleDetails.builder()
                        .scheduleStartDate(LocalDate.of(2024, 1, 1))
                        .scheduleEndDate(LocalDate.of(2024, 1, 31))
                        .build()))
            .build();

    when(client.getProviderFirmSchedules(officeCode, negativeDate)).thenReturn(Mono.empty());
    when(client.getProviderFirmSchedules(officeCode, positiveDate))
        .thenReturn(Mono.just(januaryDto));

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, negativeDate))
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, positiveDate))
        .expectNext(januaryDto)
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, negativeDate))
        .verifyComplete();

    verify(client, times(1)).getProviderFirmSchedules(officeCode, negativeDate);
    verify(client, times(1)).getProviderFirmSchedules(officeCode, positiveDate);
  }

  @Test
  void testGetProviderFirmSchedules_gapRequiresNewCall() {
    String officeCode = "Office3";
    LocalDate firstDate = LocalDate.of(2024, 2, 1);
    LocalDate gapDate = LocalDate.of(2024, 4, 15); // Between two windows

    FirmOfficeContractAndScheduleDetails febWindowSchedule =
        FirmOfficeContractAndScheduleDetails.builder()
            .scheduleStartDate(LocalDate.of(2024, 1, 1))
            .scheduleEndDate(LocalDate.of(2024, 3, 31))
            .build();
    FirmOfficeContractAndScheduleDetails novWindowSchedule =
        FirmOfficeContractAndScheduleDetails.builder()
            .scheduleStartDate(LocalDate.of(2024, 5, 1))
            .scheduleEndDate(LocalDate.of(2024, 12, 31))
            .build();
    ProviderFirmOfficeContractAndScheduleDto firstDto =
        ProviderFirmOfficeContractAndScheduleDto.builder()
            .office(ProviderFirmOfficeSummary.builder().firmOfficeCode(officeCode).build())
            .schedules(List.of(febWindowSchedule, novWindowSchedule))
            .build();

    when(client.getProviderFirmSchedules(officeCode, firstDate)).thenReturn(Mono.just(firstDto));
    when(client.getProviderFirmSchedules(officeCode, gapDate)).thenReturn(Mono.empty());

    // Read-time filtering means only the schedule actually effective on firstDate is returned,
    // even though both schedules are cached/merged internally for coverage-window purposes.
    StepVerifier.create(service.getProviderFirmSchedules(officeCode, firstDate))
        .expectNextMatches(dto -> dto.getSchedules().equals(List.of(febWindowSchedule)))
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, gapDate)).verifyComplete();

    verify(client, times(1)).getProviderFirmSchedules(officeCode, firstDate);
    verify(client, times(1)).getProviderFirmSchedules(officeCode, gapDate);
  }

  @Test
  void cacheMergesWindowsAcrossMultipleMissesAndReusesHits() {
    String officeCode = "0P322F";

    LocalDate initial = LocalDate.of(2019, 8, 20);
    LocalDate covered = LocalDate.of(2018, 11, 21);
    LocalDate extendEnd = LocalDate.of(2019, 9, 26);
    LocalDate extendStart = LocalDate.of(2017, 6, 27);
    LocalDate gapFill = LocalDate.of(2018, 7, 30);

    ProviderFirmOfficeContractAndScheduleDto sept18ToAug19 =
        dtoWithWindow(officeCode, LocalDate.of(2018, 9, 1), LocalDate.of(2019, 8, 31));
    ProviderFirmOfficeContractAndScheduleDto sept18ToAug20 =
        dtoWithWindow(officeCode, LocalDate.of(2018, 9, 1), LocalDate.of(2020, 8, 31));
    ProviderFirmOfficeContractAndScheduleDto apr17ToMar18 =
        dtoWithWindow(officeCode, LocalDate.of(2017, 4, 1), LocalDate.of(2018, 3, 31));
    ProviderFirmOfficeContractAndScheduleDto apr17ToAug20 =
        dtoWithWindow(officeCode, LocalDate.of(2017, 4, 1), LocalDate.of(2020, 8, 31));

    when(client.getProviderFirmSchedules(officeCode, initial)).thenReturn(Mono.just(sept18ToAug19));
    when(client.getProviderFirmSchedules(officeCode, extendEnd))
        .thenReturn(Mono.just(sept18ToAug20));
    when(client.getProviderFirmSchedules(officeCode, extendStart))
        .thenReturn(Mono.just(apr17ToMar18));
    when(client.getProviderFirmSchedules(officeCode, gapFill)).thenReturn(Mono.just(apr17ToAug20));

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, initial))
        .expectNext(sept18ToAug19)
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, covered))
        .expectNext(sept18ToAug19)
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, extendEnd))
        .expectNext(sept18ToAug20)
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, extendStart))
        .expectNext(apr17ToMar18)
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, gapFill))
        .expectNext(apr17ToAug20)
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
            StepVerifier.create(service.getProviderFirmSchedules(officeCode, date))
                // Read-time filtering: the merged/cached entry may contain schedules from
                // several different fetches, but only those individually effective on `date`
                // should ever be returned - never the full merged set regardless of date.
                .expectNextMatches(
                    dto ->
                        !dto.getSchedules().isEmpty()
                            && dto.getSchedules().stream()
                                .allMatch(schedule -> scheduleCovers(schedule, date)))
                .verifyComplete());

    verify(client, times(1)).getProviderFirmSchedules(officeCode, initial);
    verify(client, times(1)).getProviderFirmSchedules(officeCode, extendEnd);
    verify(client, times(1)).getProviderFirmSchedules(officeCode, extendStart);
    verify(client, times(1)).getProviderFirmSchedules(officeCode, gapFill);
    verifyNoMoreInteractions(client);
  }

  /**
   * Regression test for the false-failure/false-pass incident: a response cached for one
   * effective date must not leak its categories/schedules into a later claim whose own
   * effective date is only covered by a *different* underlying schedule window.
   */
  @Test
  void cacheHitDoesNotLeakSchedulesFromAnUnrelatedEffectiveDate() {
    String officeCode = "2Q949Z";
    LocalDate civilDate = LocalDate.of(2025, 6, 1);
    LocalDate crimeDate = LocalDate.of(2026, 7, 1);

    FirmOfficeContractAndScheduleDetails civilSchedule =
        FirmOfficeContractAndScheduleDetails.builder()
            .scheduleStartDate(LocalDate.of(2025, 1, 1))
            .scheduleEndDate(LocalDate.of(2025, 12, 31))
            .areaOfLaw("CIVIL")
            .build();
    FirmOfficeContractAndScheduleDetails crimeSchedule =
        FirmOfficeContractAndScheduleDetails.builder()
            .scheduleStartDate(LocalDate.of(2025, 10, 1))
            // open-ended - this is exactly the shape that caused the incident
            .scheduleEndDate(null)
            .areaOfLaw("CRIME LOWER")
            .build();

    ProviderFirmOfficeContractAndScheduleDto civilOnlyResponse =
        ProviderFirmOfficeContractAndScheduleDto.builder()
            .office(ProviderFirmOfficeSummary.builder().firmOfficeCode(officeCode).build())
            .schedules(List.of(civilSchedule))
            .build();
    ProviderFirmOfficeContractAndScheduleDto crimeResponse =
        ProviderFirmOfficeContractAndScheduleDto.builder()
            .office(ProviderFirmOfficeSummary.builder().firmOfficeCode(officeCode).build())
            .schedules(List.of(crimeSchedule))
            .build();

    when(client.getProviderFirmSchedules(officeCode, civilDate))
        .thenReturn(Mono.just(civilOnlyResponse));
    when(client.getProviderFirmSchedules(officeCode, crimeDate))
        .thenReturn(Mono.just(crimeResponse));

    // First claim: 2025 Civil claim populates the office-level cache with an open-ended window.
    StepVerifier.create(service.getProviderFirmSchedules(officeCode, civilDate))
        .expectNextMatches(dto -> dto.getSchedules().equals(List.of(civilSchedule)))
        .verifyComplete();

    // Second claim: 2026 Crime claim must NOT be served the cached Civil schedule just because
    // its open-ended window happens to cover 2026 - it must reflect the Crime schedule.
    StepVerifier.create(service.getProviderFirmSchedules(officeCode, crimeDate))
        .expectNextMatches(
            dto ->
                dto.getSchedules().size() == 1
                    && "CRIME LOWER".equals(dto.getSchedules().get(0).getAreaOfLaw()))
        .verifyComplete();

    // Re-checking the original Civil date afterwards must still only yield the Civil schedule.
    StepVerifier.create(service.getProviderFirmSchedules(officeCode, civilDate))
        .expectNextMatches(dto -> dto.getSchedules().equals(List.of(civilSchedule)))
        .verifyComplete();
  }

  @Test
  void negativeCacheIsScopedToEffectiveDate() {
    String officeCode = "NEG_OFFICE";
    LocalDate missingDate = LocalDate.of(2020, 1, 1);
    LocalDate otherDate = LocalDate.of(2020, 2, 1);

    ProviderFirmOfficeContractAndScheduleDto dto =
        dtoWithWindow(officeCode, otherDate, otherDate.plusDays(1));

    when(client.getProviderFirmSchedules(officeCode, missingDate)).thenReturn(Mono.empty());
    when(client.getProviderFirmSchedules(officeCode, otherDate)).thenReturn(Mono.just(dto));

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, missingDate)).verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, missingDate)).verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, otherDate))
        .expectNext(dto)
        .verifyComplete();

    verify(client, times(1)).getProviderFirmSchedules(officeCode, missingDate);
    verify(client, times(1)).getProviderFirmSchedules(officeCode, otherDate);
    verifyNoMoreInteractions(client);
  }

  private ProviderFirmOfficeContractAndScheduleDto dtoWithWindow(
      String officeCode, LocalDate start, LocalDate end) {
    return ProviderFirmOfficeContractAndScheduleDto.builder()
        .office(ProviderFirmOfficeSummary.builder().firmOfficeCode(officeCode).build())
        .schedules(
            List.of(
                FirmOfficeContractAndScheduleDetails.builder()
                    .scheduleStartDate(start)
                    .scheduleEndDate(end)
                    .build()))
        .build();
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
}
