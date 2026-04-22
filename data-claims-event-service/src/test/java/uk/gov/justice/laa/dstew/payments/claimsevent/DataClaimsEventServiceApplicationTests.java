package uk.gov.justice.laa.dstew.payments.claimsevent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
      "spring.cloud.aws.sqs.enabled=false", // Disable AWS SQS functionality
      "laa.bulk-claim-queue.name=not-used", // Dummy queue name to avoid initialization issues
    })
class DataClaimsEventServiceApplicationTests {

  @Test
  void contextLoads() {
    // empty due to only testing context load
  }
}
