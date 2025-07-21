package uk.gov.justice.laa.bulk.claim.service.provider;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.bulk.claim.service.provider.dto.ProviderFirmOfficeContractAndSchedule;

/**
 * REST service interface for fetching provider office details and schedules. This interface
 * communicates with the Provider Details API.
 *
 * @author Jamie Briggs
 */
@HttpExchange("/provider-offices")
public interface ProviderDetailsRestService {

  /**
   * Get all provider office schedule details based on the provider office code.
   *
   * @param officeCode The firm office code
   * @param areaOfLaw The area of law code
   * @return The provider firm summary
   */
  @GetExchange("/{officeCode}/schedules")
  Mono<ProviderFirmOfficeContractAndSchedule> getProviderFirmSchedules(
      @PathVariable String officeCode, @RequestParam String areaOfLaw);
}
