package uk.gov.justice.laa.dstew.payments.claimsevent.dataclaimsapi;

import java.util.List;
import java.util.UUID;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.listener.SubmissionListener;

abstract class AbstractPactTest {
  protected static final String CONSUMER = "laa-data-claims-event-service";
  protected static final String PROVIDER = "laa-data-claims-api";

  protected static final String UUID_REGEX =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
  protected static final String ANY_FORMAT_REGEX = "([a-zA-Z0-9_]+)";

  // Fixed example values for matchers. Pact generates a RANDOM example when only a
  // regex is supplied, which mutates the published contract on every run and causes the
  // broker to reject re-publishes for the same consumer version (HTTP 409). Always pass
  // these concrete examples to the 3-arg matchHeader/matchPath overloads.
  protected static final String EXAMPLE_AUTH_TOKEN = "00000000-0000-0000-0000-0000000000aa";
  protected static final String EXAMPLE_UUID = "00000000-0000-0000-0000-0000000000bb";

  // Any number, but not 0 alone. Maximum 8 digits
  protected static final String ANY_NUMBER_REGEX = "([1-9][0-9]{0,7})";

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
  protected static final UUID BULK_SUBMISSION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  protected static final UUID SUBMISSION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000002");
  protected static final UUID CLAIM_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
  protected static final UUID CLAIM_SUMMARY_FEE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000004");
  protected static final UUID MATTER_START_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000005");
  protected static final UUID PREVIOUS_SUBMISSION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000006");

  protected final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean SubmissionListener submissionListener;

  @MockitoBean SqsClient sqsClient;
}
