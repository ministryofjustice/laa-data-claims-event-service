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
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleDetails;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeSummary;

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
    when(client.getProviderFirmSchedules(anyString(), anyString(), eq(effectiveDate)))
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

    verify(client, times(1)).getProviderFirmSchedules(officeCode, "CIVIL", effectiveDate);
  }

  @Test
  void testGetProviderFirmSchedules_withError() {
    // Arrange
    String officeCode = "OFF123";
    String areaOfLaw = "Family";
    LocalDate effectiveDate = LocalDate.now();

    // Simulate all attempts fail
    when(client.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate))
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

    verify(client, times(1)).getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate);
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

    when(client.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate))
        .thenReturn(Mono.just(expectedDto));

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate))
        .expectNext(expectedDto)
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate))
        .expectNext(expectedDto)
        .verifyComplete();

    verify(client, times(1)).getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate);
  }

  @Test
  void testGetProviderFirmSchedules_negativeCache() {
    String officeCode = "Office2";
    String areaOfLaw = "CRIME";
    LocalDate effectiveDate = LocalDate.of(2024, 7, 1);

    when(client.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate))
        .thenReturn(Mono.empty());

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate))
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate))
        .verifyComplete();

    verify(client, times(1)).getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate);
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

    when(client.getProviderFirmSchedules(officeCode, areaOfLaw, firstDate))
        .thenReturn(Mono.just(firstDto));
    when(client.getProviderFirmSchedules(officeCode, areaOfLaw, gapDate)).thenReturn(Mono.empty());

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, firstDate))
        .expectNext(firstDto)
        .verifyComplete();

    StepVerifier.create(service.getProviderFirmSchedules(officeCode, areaOfLaw, gapDate))
        .verifyComplete();

    verify(client, times(1)).getProviderFirmSchedules(officeCode, areaOfLaw, firstDate);
    verify(client, times(1)).getProviderFirmSchedules(officeCode, areaOfLaw, gapDate);
  }
}
