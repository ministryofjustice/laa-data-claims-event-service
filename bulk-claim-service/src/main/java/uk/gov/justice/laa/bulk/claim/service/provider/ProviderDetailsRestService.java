package uk.gov.justice.laa.bulk.claim.service.provider;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.bulk.claim.service.provider.dto.ProviderFirmSummary;

@Component
@RequiredArgsConstructor
public class ProviderDetailsRestService implements ProviderDetailsService {

  private final WebClient providerDetailsWebClient;

  @Override
  public Mono<ProviderFirmSummary> getProviderFirmSummary(
      String firmNumber, String areaOfLaw, LocalDate effectiveDate) {
    return Mono.empty();
  }
}
