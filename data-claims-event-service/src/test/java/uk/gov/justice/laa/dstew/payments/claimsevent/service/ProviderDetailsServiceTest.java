package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleDetails;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeSummary;

@ExtendWith(MockitoExtension.class)
class ProviderDetailsServiceTest {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

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
    when(client.getProviderFirmSchedules(anyString(), anyString(), eq(formatted(effectiveDate))))
        .thenReturn(Mono.just(expectedDto));

    // Act
    Mono<ProviderFirmOfficeContractAndScheduleDto> result =
        service.getProviderFirmSchedules(officeCode, "CIVIL", effectiveDate);

    // Assert
    StepVerifier.create(result)
        .expectNextMatches(
            dto -> {
              assertNotNull(dto.getOffice());
              assertNotNull(dto.getOffice().getFirmOfficeCode());
              return dto.getOffice().getFirmOfficeCode().equals(officeCode);
            })
        .verifyComplete();

    verify(client, times(1))
        .getProviderFirmSchedules(officeCode, "CIVIL", formatted(effectiveDate));
  }

  @Test
  void testGetProviderFirmSchedules_withError() {
    // Arrange
    String officeCode = "OFF123";
    String areaOfLaw = "Family";
    LocalDate effectiveDate = LocalDate.now();

    // Simulate all attempts fail
    when(client.getProviderFirmSchedules(officeCode, areaOfLaw, formatted(effectiveDate)))
        .thenReturn(Mono.error(new RuntimeException("Temporary error")));

    // Act
    Mono<ProviderFirmOfficeContractAndScheduleDto> result =
        service.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate);

    // Assert
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().contains("Temporary error"))
        .verify();

    verify(client, times(1))
        .getProviderFirmSchedules(officeCode, areaOfLaw, formatted(effectiveDate));
  }

  @Test
  void testGetProviderFirmSchedules_usesCacheWhenDateCovered() {
    String officeCode = "Office1";
    String areaOfLaw = "CIVIL";
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

    when(client.getProviderFirmSchedules(officeCode, areaOfLaw, formatted(effectiveDate)))
        .thenReturn(Mono.just(expectedDto));

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate))
        .expectNext(expectedDto)
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate))
        .expectNext(expectedDto)
        .verifyComplete();

    verify(client, times(1))
        .getProviderFirmSchedules(officeCode, areaOfLaw, formatted(effectiveDate));
  }

  @Test
  void testGetProviderFirmSchedules_negativeCache() {
    String officeCode = "Office2";
    String areaOfLaw = "CRIME";
    LocalDate effectiveDate = LocalDate.of(2024, 7, 1);

    when(client.getProviderFirmSchedules(officeCode, areaOfLaw, formatted(effectiveDate)))
        .thenReturn(Mono.empty());

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate))
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate))
        .verifyComplete();

    verify(client, times(1))
        .getProviderFirmSchedules(officeCode, areaOfLaw, formatted(effectiveDate));
  }

  @Test
  void testGetProviderFirmSchedules_gapRequiresNewCall() {
    String officeCode = "Office3";
    String areaOfLaw = "CIVIL";
    LocalDate firstDate = LocalDate.of(2024, 2, 1);
    LocalDate gapDate = LocalDate.of(2024, 4, 15); // Between two windows

    ProviderFirmOfficeContractAndScheduleDto firstDto =
        ProviderFirmOfficeContractAndScheduleDto.builder()
            .office(ProviderFirmOfficeSummary.builder().firmOfficeCode(officeCode).build())
            .schedules(
                List.of(
                    FirmOfficeContractAndScheduleDetails.builder()
                        .scheduleStartDate(LocalDate.of(2024, 1, 1))
                        .scheduleEndDate(LocalDate.of(2024, 3, 31))
                        .build(),
                    FirmOfficeContractAndScheduleDetails.builder()
                        .scheduleStartDate(LocalDate.of(2024, 5, 1))
                        .scheduleEndDate(LocalDate.of(2024, 12, 31))
                        .build()))
            .build();

    when(client.getProviderFirmSchedules(officeCode, areaOfLaw, formatted(firstDate)))
        .thenReturn(Mono.just(firstDto));
    when(client.getProviderFirmSchedules(officeCode, areaOfLaw, formatted(gapDate)))
        .thenReturn(Mono.empty());

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, firstDate))
        .expectNext(firstDto)
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, gapDate))
        .verifyComplete();

    verify(client, times(1)).getProviderFirmSchedules(officeCode, areaOfLaw, formatted(firstDate));
    verify(client, times(1)).getProviderFirmSchedules(officeCode, areaOfLaw, formatted(gapDate));
  }

  @Test
  void cacheMergesWindowsAcrossMultipleMissesAndReusesHits() {
    String officeCode = "0P322F";
    String areaOfLaw = "LEGAL HELP";

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

    when(client.getProviderFirmSchedules(officeCode, areaOfLaw, formatted(initial)))
        .thenReturn(Mono.just(sept18ToAug19));
    when(client.getProviderFirmSchedules(officeCode, areaOfLaw, formatted(extendEnd)))
        .thenReturn(Mono.just(sept18ToAug20));
    when(client.getProviderFirmSchedules(officeCode, areaOfLaw, formatted(extendStart)))
        .thenReturn(Mono.just(apr17ToMar18));
    when(client.getProviderFirmSchedules(officeCode, areaOfLaw, formatted(gapFill)))
        .thenReturn(Mono.just(apr17ToAug20));

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, initial))
        .expectNext(sept18ToAug19)
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, covered))
        .expectNext(sept18ToAug19)
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, extendEnd))
        .expectNext(sept18ToAug20)
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, extendStart))
        .expectNext(apr17ToMar18)
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, gapFill))
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
            StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, date))
                .expectNextMatches(dto -> dto.getSchedules().size() == 4 && covers(dto, date))
                .verifyComplete());

    verify(client, times(1)).getProviderFirmSchedules(officeCode, areaOfLaw, formatted(initial));
    verify(client, times(1)).getProviderFirmSchedules(officeCode, areaOfLaw, formatted(extendEnd));
    verify(client, times(1))
        .getProviderFirmSchedules(officeCode, areaOfLaw, formatted(extendStart));
    verify(client, times(1)).getProviderFirmSchedules(officeCode, areaOfLaw, formatted(gapFill));
    verifyNoMoreInteractions(client);
  }

  @Test
  void negativeCacheIsScopedToEffectiveDate() {
    String officeCode = "NEG_OFFICE";
    String areaOfLaw = "CRIME";
    LocalDate missingDate = LocalDate.of(2020, 1, 1);
    LocalDate otherDate = LocalDate.of(2020, 2, 1);

    ProviderFirmOfficeContractAndScheduleDto dto =
        dtoWithWindow(officeCode, otherDate, otherDate.plusDays(1));

    when(client.getProviderFirmSchedules(officeCode, areaOfLaw, formatted(missingDate)))
        .thenReturn(Mono.empty());
    when(client.getProviderFirmSchedules(officeCode, areaOfLaw, formatted(otherDate)))
        .thenReturn(Mono.just(dto));

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, missingDate))
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, missingDate))
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, otherDate))
        .expectNext(dto)
        .verifyComplete();

    verify(client, times(1))
        .getProviderFirmSchedules(officeCode, areaOfLaw, formatted(missingDate));
    verify(client, times(1)).getProviderFirmSchedules(officeCode, areaOfLaw, formatted(otherDate));
    verifyNoMoreInteractions(client);
  }

  private String formatted(LocalDate date) {
    return FORMATTER.format(date);
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

  private boolean covers(ProviderFirmOfficeContractAndScheduleDto dto, LocalDate effectiveDate) {
    return dto.getSchedules().stream()
        .anyMatch(
            schedule ->
                !effectiveDate.isBefore(schedule.getScheduleStartDate())
                    && !effectiveDate.isAfter(schedule.getScheduleEndDate()));
  }
}
