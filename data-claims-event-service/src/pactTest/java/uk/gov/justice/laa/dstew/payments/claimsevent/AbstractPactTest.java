package uk.gov.justice.laa.dstew.payments.claimsevent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public abstract class AbstractPactTest {
  public static final String CONSUMER = "laa-data-claims-event-service";
  public static final String PROVIDER = "laa-data-claims-api";

  protected static final String UUID_REGEX =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
  protected static final String ANY_FORMAT_REGEX = "(.*?)";
  protected static final String ANY_NUMBER_REGEX = "([0-9]+)";

  protected final List<String> userOffices = List.of("ABC123", "XYZ789");
  protected final UUID bulkSubmissionId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
  protected final UUID submissionId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
  protected final UUID claimId = UUID.fromString("d4e3fa24-7d1f-4710-b7a7-0debe88421aa");
  protected final UUID matterStartId = UUID.fromString("b2946085-44fd-477f-a827-1e12c44a89ab");
  protected final UUID messageId = UUID.fromString("1b60f94e-d29c-4522-9cb5-2e21dbe9a871");

  public static String readJsonFromFile(final String fileName) throws Exception {
    Path path = Paths.get("src/pactTest/resources/responses/", fileName);
    return Files.readString(path);
  }
}
