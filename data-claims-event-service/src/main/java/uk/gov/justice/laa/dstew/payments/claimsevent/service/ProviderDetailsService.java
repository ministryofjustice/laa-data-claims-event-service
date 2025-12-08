package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;

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
  private final RateLimiterRegistry rateLimiterRegistry;

  /**
   * Retrieves the provider firm office contract and schedule information for a given office, area
   * of law, and effective date.
   *
   * <p>This method delegates the call to the underlying client and applies a retry mechanism
   * defined by the {@code pdaRetry} configuration.
   *
   * @param officeCode the unique code identifying the office (must not be {@code null})
   * @param areaOfLaw the area of law for which schedules are requested (must not be {@code null})
   * @param effectiveDate the date from which the schedule should be effective (must not be {@code
   *     null})
   * @return a {@link Mono} emitting {@link ProviderFirmOfficeContractAndScheduleDto} containing the
   *     contract and schedule details for the specified parameters
   * @throws IllegalArgumentException if any of the parameters are invalid
   */
  public Mono<ProviderFirmOfficeContractAndScheduleDto> getProviderFirmSchedules(
      String officeCode, String areaOfLaw, LocalDate effectiveDate) {
    RateLimiter perSecondLimiter = rateLimiterRegistry.rateLimiter("pdaRateLimiterPerSecond");
    RateLimiter perMinuteLimiter = rateLimiterRegistry.rateLimiter("pdaRateLimiterPerMinute");
    Retry retry = retryRegistry.retry("pdaRetry");
    return providerDetailsRestClient
        .getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate)
        .transformDeferred(RateLimiterOperator.of(perSecondLimiter))
        .transformDeferred(RateLimiterOperator.of(perMinuteLimiter))
        .transformDeferred(RetryOperator.of(retry))
        .onErrorResume(
            e -> {
              log.info("Failed after retries: {}", e.getMessage());
              return Mono.error(e);
            });
  }
}
