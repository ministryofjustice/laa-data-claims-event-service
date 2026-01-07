package uk.gov.justice.laa.dstew.payments.claimsevent.helper;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;

@Slf4j
@TestInstance(Lifecycle.PER_CLASS)
public class LocalstackBaseIntegrationTest {

  @Value("${laa.bulk-claim-queue.name}")
  protected String queueName;

  protected static final DockerImageName SQS_IMAGE =
      DockerImageName.parse("localstack/localstack:3.4");

  private static final LocalStackContainer INSTANCE = createContainer();

  protected SqsClient sqsClient;

  protected LocalStackContainer localStackContainer;

  protected ObjectMapper objectMapper = new ObjectMapper();

  protected String queueUrl;

  @MockitoBean PrometheusRegistry prometheusRegistry;

  @DynamicPropertySource
  static void registerDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.cloud.aws.sqs.endpoint", () -> INSTANCE.getEndpointOverride(SNS).toString());
    registry.add("spring.cloud.aws.credentials.access-key", INSTANCE::getAccessKey);
    registry.add("spring.cloud.aws.credentials.secret-key", INSTANCE::getSecretKey);
    registry.add("spring.cloud.aws.region.static", INSTANCE::getRegion);
  }

  @BeforeAll
  void beforeEveryTest() {
    // Skip tests if Docker is unavailable
    Assumptions.assumeTrue(
        DockerClientFactory.instance().isDockerAvailable(),
        "Docker is not available, skipping the tests.");

    // Get instance to container
    localStackContainer = INSTANCE;

    // Initialize SQS client
    sqsClient = sqsClient(localStackContainer);
  }

  @BeforeEach
  void beforeEach() {
    // create the queue if it doesn't exist
    sqsClient.createQueue(builder -> builder.queueName(queueName));

    // Get the queue URL
    this.queueUrl =
        sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
  }

  private static LocalStackContainer createContainer() {
    LocalStackContainer container = new LocalStackContainer(SQS_IMAGE);
    container.withServices(SQS);
    container.start();
    // TODO: Add SQS queue to container
    log.info("Started LocalStack container on port: {}", container.getFirstMappedPort());
    return container;
  }

  private static SqsClient sqsClient(LocalStackContainer localStackContainer) {

    return SqsClient.builder()
        .endpointOverride(localStackContainer.getEndpointOverride(SQS))
        .region(Region.of(localStackContainer.getRegion()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    localStackContainer.getAccessKey(), localStackContainer.getSecretKey())))
        .build();
  }
}
