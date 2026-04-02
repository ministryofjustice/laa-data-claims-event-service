package uk.gov.justice.laa.dstew.payments.claimsevent.feeschemeplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.UUID;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.listener.SubmissionListener;

abstract class AbstractPactTest {

  protected static final String CONSUMER = "laa-data-claims-event-service";
  protected static final String PROVIDER = "laa-fee-scheme-platform-api";

  protected static final String UUID_REGEX =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
  protected static final UUID CLAIM_ID = UUID.randomUUID();

  protected static final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule()) // Needed!
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  protected static final UUID SUBMISSION_ID = UUID.randomUUID();

  // Required Context Spring Beans
  @MockitoBean SubmissionListener submissionListener;

  @MockitoBean SqsClient sqsClient;
}
