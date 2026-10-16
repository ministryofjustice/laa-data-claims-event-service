package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laadata.providers.model.ProviderFirmOfficeContractAndScheduleDto;

/**
 * Service layer for ProviderDetailsRestClient, in order to apply the retry backoff.
 *
 * @author Jose Carlos Arinero Adam
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ProviderDetailsService {

  private final ProviderDetailsRestClient providerDetailsRestClient;
  private final RetryRegistry retryRegistry;

  /**
   * Retrieves provider schedules for the supplied office and claim effective date.
   *
   * @param officeCode the unique code identifying the office
   * @param effectiveDate the individual claim's resolved effective date
   * @return the Provider Details response for the supplied office and date
   */
  public Mono<ProviderFirmOfficeContractAndScheduleDto> getProviderFirmSchedules(
      String officeCode, LocalDate effectiveDate) {
    Retry retry = retryRegistry.retry("pdaRetry");
    log.debug(
        "Calling PDA getProviderFirmSchedules for officeCode {}, effectiveDate {}",
        officeCode,
        effectiveDate);
    return providerDetailsRestClient
        .getProviderFirmSchedules(officeCode, effectiveDate)
        .transformDeferred(RetryOperator.of(retry));
  }
}
