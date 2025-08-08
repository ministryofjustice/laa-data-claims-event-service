package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * The base class for API properties.
 *
 * @author Jamie Briggs
 */
@Getter
@Setter
@AllArgsConstructor
public class ApiProperties {

  private final String url;
  private final String host;
  private final int port;
  private final String accessToken;
}
