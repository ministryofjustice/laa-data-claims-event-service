package uk.gov.justice.laa.bulk.claim.service.provider;

import org.springframework.http.MediaType;
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
@HttpExchange(value = "/provider-offices", accept = MediaType.APPLICATION_JSON_VALUE)
public interface ProviderDetailsRestService {

  /**
   * Get all provider office schedule details based on the provider office code. Can return the
   * following HTTP statuses:
   *
   * <ul>
   *   <li>200 - Success
   *   <li>204 - No content (Happens when a firm has no schedules).
   *   <li>409 - Conflict - Ex Cache being Loaded.
   *   <li>500 - Internal Server Error.
   * </ul>
   *
   * @param officeCode The firm office code
   * @param areaOfLaw The area of law code
   * @return The provider firm summary
   */
  @GetExchange("/{officeCode}/schedules")
  Mono<ProviderFirmOfficeContractAndSchedule> getProviderFirmSchedules(
      final @PathVariable String officeCode,
      final @RequestParam(required = false) String areaOfLaw);
}
