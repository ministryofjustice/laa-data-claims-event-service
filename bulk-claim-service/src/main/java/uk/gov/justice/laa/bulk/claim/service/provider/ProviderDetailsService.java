package uk.gov.justice.laa.bulk.claim.service.provider;

import java.time.LocalDate;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.bulk.claim.service.provider.dto.ProviderFirmSummary;

public interface ProviderDetailsService {

  Mono<ProviderFirmSummary> getProviderFirmSummary(
      String firmNumber, String areaOfLaw, LocalDate effectiveDate);
}
