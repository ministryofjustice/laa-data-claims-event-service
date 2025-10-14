// package uk.gov.justice.laa.dstew.payments.claimsevent.helper;
//
// import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
//
// import java.io.IOException;
// import org.junit.jupiter.api.BeforeAll;
// import org.junit.jupiter.api.TestInstance;
// import org.springframework.test.context.DynamicPropertyRegistry;
// import org.springframework.test.context.DynamicPropertySource;
// import org.testcontainers.containers.localstack.LocalStackContainer;
// import org.testcontainers.junit.jupiter.Container;
// import org.testcontainers.junit.jupiter.Testcontainers;
// import org.testcontainers.utility.DockerImageName;
//
// @Testcontainers
// @TestInstance(TestInstance.Lifecycle.PER_CLASS)
// public class MessageListenerBase {
//  protected static final String queueName = "test-queue-name";
//
//  protected static final DockerImageName SQS_IMAGE =
//      DockerImageName.parse("localstack/localstack:3.4");
//
//  @Container
//  static LocalStackContainer localStack =
//      new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.4"));
//
//  @DynamicPropertySource
//  static void registerDynamicProperties(DynamicPropertyRegistry registry) {
//    registry.add("spring.cloud.aws.endpoint", () ->
// localStack.getEndpointOverride(SNS).toString());
//    registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
//    registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
//    registry.add("spring.cloud.aws.region.static", localStack::getRegion);
//    registry.add("laa.bulk-claim-queue.name", () -> queueName);
//  }
//
//  @BeforeAll
//  static void createQueue() throws IOException, InterruptedException {
//    localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", queueName);
//  }
// }
