package uk.gov.justice.laa.dstew.payments.claimsevent;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    properties = {
      "spring.cloud.aws.sqs.enabled=false", // Disable AWS SQS functionality
      "laa.bulk-claim-queue.name=not-used", // Dummy queue name to avoid initialization issues
    })
class DataClaimsEventServiceApplicationTests {

  @MockitoBean PrometheusRegistry prometheusRegistry;

  @Test
  void contextLoads() {
    // empty due to only testing context load
  }
}
