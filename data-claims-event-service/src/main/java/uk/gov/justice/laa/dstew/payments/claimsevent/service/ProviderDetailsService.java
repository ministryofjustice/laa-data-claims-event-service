package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import io.github.resilience4j.retry.annotation.Retry;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;

/**
 * Service layer for ProviderDetailsRestClient, in order to apply the retry backoff.
 *
 * @author Jose Carlos Arinero Adam
 */
@Service
public class ProviderDetailsService {

  private final ProviderDetailsRestClient client;

  public ProviderDetailsService(ProviderDetailsRestClient client) {
    this.client = client;
  }

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
  @Retry(name = "pdaRetry")
  public Mono<ProviderFirmOfficeContractAndScheduleDto> getProviderFirmSchedules(
      String officeCode, String areaOfLaw, LocalDate effectiveDate) {
    return client.getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate);
  }
}
