package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import io.awspring.cloud.autoconfigure.sqs.SqsProperties;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.maintenance.AcknowledgementTrackingCallback;

/**
 * Configuration class for AWS SQS integration. This class defines a bean for creating and
 * configuring an SQS client using AWS SDK's {@link SqsClient}. It reads AWS configuration
 * properties from the application's property file and sets up the client with appropriate
 * credentials, region, and endpoint.
 *
 * <p>The SQS client created by this configuration can be used for interacting with AWS SQS
 * services, handling message queues, and managing queue operations.
 */
@Configuration
public class SqsConfig {
  /**
   * Creates and configures an {@link SqsClient} for interacting with AWS Simple Queue Service
   * (SQS). The method uses AWS configuration values for region, access key, secret key, and
   * endpoint provided as application properties.
   *
   * @param region the AWS region where the SQS service is located
   * @param accessKey the AWS access key used for authentication
   * @param secretKey the AWS secret key used for authentication
   * @param endpoint the AWS endpoint for SQS, typically used for specifying a custom SQS endpoint
   * @return an instance of {@link SqsClient} configured with the specified properties
   */
  @Bean
  @Profile({"test", "wiremock"})
  public SqsClient sqsClientLocal(
      @Value("${spring.cloud.aws.region.static}") String region,
      @Value("${spring.cloud.aws.credentials.access-key}") String accessKey,
      @Value("${spring.cloud.aws.credentials.secret-key}") String secretKey,
      @Value("${spring.cloud.aws.endpoint}") String endpoint) {

    return SqsClient.builder()
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .endpointOverride(URI.create(endpoint))
        .build();
  }

  /**
   * Creates and configures an {@link SqsClient} for interacting with AWS Simple Queue Service
   * (SQS). This method sets up the SQS client with a specified AWS region and default credentials
   * provider. The bean is active in non-test and non-wiremock profiles.
   *
   * @param region the AWS region where the SQS service is located, typically specified in the
   *     application configuration
   * @return an instance of {@link SqsClient} configured with the specified AWS region and default
   *     credentials provider
   */
  @Bean
  @Profile("!test & !wiremock")
  public SqsClient sqsClient(@Value("${spring.cloud.aws.region.static}") String region) {
    return SqsClient.builder()
        .region(Region.of(region))
        .credentialsProvider(DefaultCredentialsProvider.builder().build())
        .build();
  }

  /**
   * Creates the default {@link SqsMessageListenerContainerFactory} used by {@code @SqsListener}
   * methods, with an explicit, configurable shutdown budget.
   *
   * <p>On shutdown, the container stops polling for new messages immediately and gives in-flight
   * message processing up to {@code listenerShutdownTimeout} to finish. Acknowledgement mode is
   * explicitly {@link AcknowledgementMode#ON_SUCCESS}, meaning a message is only ever deleted from
   * the queue after its listener method completes successfully - a message still processing (or
   * interrupted) when the shutdown budget is exceeded is never acknowledged, and becomes visible
   * again for redelivery once its SQS visibility timeout expires.
   *
   * @param sqsAsyncClient the async SQS client used by the listener container
   * @param sqsProperties the auto-configured {@code spring.cloud.aws.sqs.*} properties, re-applied
   *     here (e.g. {@code queue-not-found-strategy: fail}) since replacing Spring Boot's
   *     auto-configured factory bean means those settings are no longer applied automatically
   * @param acknowledgementTrackingCallback settles {@link
   *     uk.gov.justice.laa.dstew.payments.claimsevent.maintenance.ListenerActivityTracker}'s
   *     in-flight count once a message's acknowledgement actually completes, so draining reflects
   *     acknowledgement, not just listener-method completion
   * @param listenerShutdownTimeoutSeconds how long to wait for in-flight message processing to
   *     finish on shutdown before stopping regardless
   * @param acknowledgementShutdownTimeoutSeconds how long to wait for pending acknowledgements to
   *     be flushed on shutdown; must be greater than or equal to {@code
   *     listenerShutdownTimeoutSeconds} so a message that finishes processing just before its
   *     deadline still has time to be acknowledged
   * @return the configured {@link SqsMessageListenerContainerFactory}
   */
  @Bean
  @ConditionalOnProperty(prefix = "spring.cloud.aws.sqs", name = "enabled", matchIfMissing = true)
  public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
      SqsAsyncClient sqsAsyncClient,
      SqsProperties sqsProperties,
      AcknowledgementTrackingCallback acknowledgementTrackingCallback,
      @Value("${laa.bulk-claim-queue.listener-shutdown-timeout-seconds:20}")
          int listenerShutdownTimeoutSeconds,
      @Value("${laa.bulk-claim-queue.acknowledgement-shutdown-timeout-seconds:20}")
          int acknowledgementShutdownTimeoutSeconds) {
    return SqsMessageListenerContainerFactory.builder()
        .configure(
            options -> {
              options
                  .acknowledgementMode(AcknowledgementMode.ON_SUCCESS)
                  .listenerShutdownTimeout(Duration.ofSeconds(listenerShutdownTimeoutSeconds))
                  .acknowledgementShutdownTimeout(
                      Duration.ofSeconds(acknowledgementShutdownTimeoutSeconds));
              if (sqsProperties.getQueueNotFoundStrategy() != null) {
                options.queueNotFoundStrategy(sqsProperties.getQueueNotFoundStrategy());
              }
            })
        .acknowledgementResultCallback(acknowledgementTrackingCallback)
        .sqsAsyncClient(sqsAsyncClient)
        .build();
  }
}
