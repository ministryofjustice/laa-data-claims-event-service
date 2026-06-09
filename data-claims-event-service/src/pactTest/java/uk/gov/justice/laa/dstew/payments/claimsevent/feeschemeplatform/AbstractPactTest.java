package uk.gov.justice.laa.dstew.payments.claimsevent.feeschemeplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.UUID;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.listener.SubmissionListener;

// Force a deterministic test method execution order so the order of interactions
// in the published pact JSON is stable across runs (avoids broker HTTP 409 on
// re-publish of the same consumer version).
@TestMethodOrder(MethodOrderer.MethodName.class)
abstract class AbstractPactTest {

  protected static final String CONSUMER = "laa-data-claims-event-service";
  protected static final String PROVIDER = "laa-fee-scheme-platform-api";

  protected static final String UUID_REGEX =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
  // Fixed UUIDs: random values mutate the published pact on every run and cause the
  // broker to reject re-publishes for the same consumer version (HTTP 409).
  protected static final UUID CLAIM_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

  protected static final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule()) // Needed!
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  protected static final UUID SUBMISSION_ID =
      UUID.fromString("00000000-0000-0000-0000-0000000000c2");

  // Required Context Spring Beans
  @MockitoBean SubmissionListener submissionListener;

  @MockitoBean SqsClient sqsClient;
}
