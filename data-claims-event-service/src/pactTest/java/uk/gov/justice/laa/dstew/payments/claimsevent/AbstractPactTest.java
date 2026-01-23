package uk.gov.justice.laa.dstew.payments.claimsevent;

import java.util.List;
import java.util.UUID;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.listener.SubmissionListener;

public abstract class AbstractPactTest {
  public static final String CONSUMER = "laa-data-claims-event-service";
  public static final String PROVIDER = "laa-data-claims-api";

  protected static final String UUID_REGEX =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
  protected static final String ANY_FORMAT_REGEX = "([a-zA-Z0-9_]+)";
  protected static final String ANY_NUMBER_REGEX = "([0-9]+)";

  protected static <E extends Enum<E>> String enumValuesToRegex(Class<E> enumClass) {
    return "("
        + String.join(
            "|",
            java.util.Arrays.stream(enumClass.getEnumConstants())
                .map(e -> e.name().toUpperCase())
                .toArray(String[]::new))
        + ")";
  }

  protected static final List<String> USER_OFFICES = List.of("ABC123", "XYZ789");
  protected static final UUID BULK_SUBMISSION_ID = UUID.randomUUID();
  protected static final UUID SUBMISSION_ID = UUID.randomUUID();
  protected static final UUID CLAIM_ID = UUID.randomUUID();
  protected static final UUID CLAIM_SUMMARY_FEE_ID = UUID.randomUUID();
  protected static final UUID MATTER_START_ID = UUID.randomUUID();

  protected final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean SubmissionListener submissionListener;

  @MockitoBean SqsClient sqsClient;
}
