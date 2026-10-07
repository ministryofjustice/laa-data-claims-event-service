package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    Retry noOpRetry = Retry.of("pdaRetry", RetryConfig.custom().maxAttempts(1).build());
    when(retryRegistry.retry("pdaRetry")).thenReturn(noOpRetry);
  }

  @Test
  void getProviderFirmSchedulesCallsPdaForEachDifferentEffectiveDate() {
    String officeCode = "Office1";
    LocalDate firstDate = LocalDate.of(2025, 6, 1);
    LocalDate secondDate = LocalDate.of(2026, 7, 1);
    ProviderFirmOfficeContractAndScheduleDto firstResponse = dto(officeCode, "CIVIL");
    ProviderFirmOfficeContractAndScheduleDto secondResponse = dto(officeCode, "CRIME LOWER");

    when(client.getProviderFirmSchedules(officeCode, firstDate))
        .thenReturn(Mono.just(firstResponse));
    when(client.getProviderFirmSchedules(officeCode, secondDate))
        .thenReturn(Mono.just(secondResponse));

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, firstDate))
        .expectNext(firstResponse)
        .verifyComplete();
    StepVerifier.create(service.getProviderFirmSchedules(officeCode, secondDate))
        .expectNext(secondResponse)
        .verifyComplete();

    verify(client).getProviderFirmSchedules(officeCode, firstDate);
    verify(client).getProviderFirmSchedules(officeCode, secondDate);
  }

  @Test
  void getProviderFirmSchedulesCallsPdaForEveryInvocationIncludingSameDate() {
    String officeCode = "Office1";
    LocalDate effectiveDate = LocalDate.of(2025, 6, 1);
    ProviderFirmOfficeContractAndScheduleDto firstResponse = dto(officeCode, "CIVIL");
    ProviderFirmOfficeContractAndScheduleDto secondResponse = dto(officeCode, "CRIME LOWER");

    when(client.getProviderFirmSchedules(officeCode, effectiveDate))
        .thenReturn(Mono.just(firstResponse))
        .thenReturn(Mono.just(secondResponse));

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, effectiveDate))
        .expectNext(firstResponse)
        .verifyComplete();
    StepVerifier.create(service.getProviderFirmSchedules(officeCode, effectiveDate))
        .expectNext(secondResponse)
        .verifyComplete();

    verify(client, times(2)).getProviderFirmSchedules(officeCode, effectiveDate);
  }

  @Test
  void getProviderFirmSchedulesReturnsEmptyPdaResponseWithoutCachingIt() {
    String officeCode = "Office2";
    LocalDate effectiveDate = LocalDate.of(2025, 6, 1);

    when(client.getProviderFirmSchedules(officeCode, effectiveDate))
        .thenReturn(Mono.empty())
        .thenReturn(Mono.just(dto(officeCode, "CIVIL")));

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, effectiveDate))
        .verifyComplete();
    StepVerifier.create(service.getProviderFirmSchedules(officeCode, effectiveDate))
        .expectNextCount(1)
        .verifyComplete();

    verify(client, times(2)).getProviderFirmSchedules(officeCode, effectiveDate);
  }

  @Test
  void getProviderFirmSchedulesReturnsThePdaResponseUnchanged() {
    String officeCode = "Office3";
    LocalDate effectiveDate = LocalDate.of(2025, 6, 1);
    ProviderFirmOfficeContractAndScheduleDto response = dto(officeCode, "CIVIL");

    when(client.getProviderFirmSchedules(officeCode, effectiveDate))
        .thenReturn(Mono.just(response));

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, effectiveDate))
        .assertNext(actual -> assertSame(response, actual))
        .verifyComplete();
  }

  @Test
  void getProviderFirmSchedulesWithErrorPropagatesError() {
    String officeCode = "OFF123";
    LocalDate effectiveDate = LocalDate.of(2025, 6, 1);
    RuntimeException failure = new RuntimeException("Temporary error");

    when(client.getProviderFirmSchedules(officeCode, effectiveDate))
        .thenReturn(Mono.error(failure));

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, effectiveDate))
        .expectErrorSatisfies(error -> assertEquals("Temporary error", error.getMessage()))
        .verify();

    verify(client).getProviderFirmSchedules(officeCode, effectiveDate);
  }

  private ProviderFirmOfficeContractAndScheduleDto dto(String officeCode, String areaOfLaw) {
    return ProviderFirmOfficeContractAndScheduleDto.builder()
        .office(ProviderFirmOfficeSummary.builder().firmOfficeCode(officeCode).build())
        .schedules(
            List.of(
                FirmOfficeContractAndScheduleDetails.builder()
                    .areaOfLaw(areaOfLaw)
                    .scheduleStartDate(LocalDate.of(2025, 1, 1))
                    .scheduleEndDate(LocalDate.of(2026, 12, 31))
                    .build()))
        .build();
  }
}
