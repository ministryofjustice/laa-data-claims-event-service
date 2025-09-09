package uk.gov.justice.laa.dstew.payments.claimsevent.client;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;

/**
 * REST client interface for fetching provider office details and schedules. This interface
 * communicates with the Provider Details API.
 *
 * @author Jamie Briggs
 */
@HttpExchange(value = "/api/v2/provider-offices", accept = MediaType.APPLICATION_JSON_VALUE)
public interface ProviderDetailsRestClient {

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
  Mono<ProviderFirmOfficeContractAndScheduleDto> getProviderFirmSchedules(
      final @PathVariable String officeCode,
      final @RequestParam(required = false) String areaOfLaw);

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
   * @param effectiveDate The contract effective date for testing on lower environments. Should not
   *     be used for production environments.
   * @return The provider firm summary
   */
  @GetExchange("/{officeCode}/schedules")
  Mono<ProviderFirmOfficeContractAndScheduleDto> getProviderFirmSchedules(
      final @PathVariable String officeCode,
      final @RequestParam(required = false) String areaOfLaw,
      final @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate
              effectiveDate);
}
