package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

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
  @Profile({"test", "wiremock", "local"})
  public SqsClient sqsClientLocal(
      @Value("${spring.cloud.aws.region.static}") String region,
      @Value("${spring.cloud.aws.credentials.access-key}") String accessKey,
      @Value("${spring.cloud.aws.credentials.secret-key}") String secretKey,
      @Value("${spring.cloud.aws.sqs.endpoint}") String endpoint) {

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
}
