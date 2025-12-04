package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
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
}
