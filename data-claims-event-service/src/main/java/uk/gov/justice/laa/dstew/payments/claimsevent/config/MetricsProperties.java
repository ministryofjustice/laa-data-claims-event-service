package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalised configuration for the {@link
 * uk.gov.justice.laa.dstew.payments.claimsevent.metrics.MetricPublisher}.
 *
 * <p>Bound from the {@code laa.metrics} prefix in {@code application.yml}, allowing both the
 * Prometheus metric namespace and the slow-operation warn threshold to be adjusted per environment
 * without a code change or redeployment.
 *
 * <pre>{@code
 * laa:
 *   metrics:
 *     namespace: claims_event_service_
 *     warn-threshold-seconds: 2.0
 * }</pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "laa.metrics")
public class MetricsProperties {

  /**
   * Prometheus metric name prefix prepended to all metric short names. Defaults to {@code
   * claims_event_service_}.
   */
  private String namespace = "claims_event_service_";

  /**
   * Duration in seconds above which a timer completion is logged at WARN level. Defaults to {@code
   * 2.0} seconds.
   */
  private double warnThresholdSeconds = 2.0;
}
