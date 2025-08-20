package uk.gov.justice.laa.dstew.payments.claimsevent.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;

class BulkSubmissionMapperTest {

  private BulkSubmissionMapper mapper;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mapper = new BulkSubmissionMapperImpl();
    objectMapper =
        new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Test
  void shouldMapBulkSubmissionResponseToSubmissionPost() throws IOException {
    // Arrange
    String json = Files.readString(Path.of("src/test/resources/bulk-submission-response.json"));

    GetBulkSubmission200Response bulkSubmission =
        objectMapper.readValue(json, GetBulkSubmission200Response.class);

    UUID submissionId = UUID.randomUUID();

    SubmissionPost result = mapper.mapToSubmissionPost(bulkSubmission, submissionId);

    assertThat(result).isNotNull();
    assertThat(result.getSubmissionId()).isEqualTo(submissionId);
    assertThat(result.getBulkSubmissionId())
        .isEqualTo(UUID.fromString("c4ef2c36-35dc-4627-b5d8-7db10d86be3d"));
    assertThat(result.getNumberOfClaims()).isEqualTo(1);
    assertThat(result.getIsNilSubmission()).isFalse();
    assertThat(result.getOfficeAccountNumber()).isEqualTo("2Q286D");
    assertThat(result.getStatus())
        .isEqualTo(SubmissionStatus.CREATED); // or enum value depending on type
  }

  @Test
  void shouldMapOutcomesToClaimPosts() throws IOException {
    String json = Files.readString(Path.of("src/test/resources/bulk-submission-response.json"));
    GetBulkSubmission200Response bulkSubmission =
        objectMapper.readValue(json, GetBulkSubmission200Response.class);

    var claims = mapper.mapToClaimPosts(bulkSubmission.getDetails().getOutcomes());

    assertThat(claims).hasSize(1);
    ClaimPost claim = claims.get(0);
    assertThat(claim.getStatus()).isEqualTo(ClaimStatus.READY_TO_PROCESS);
    assertThat(claim.getScheduleReference()).isEqualTo("01/2Q286D/2024/01");
    assertThat(claim.getCaseReferenceNumber()).isEqualTo("JI/OKUSU");
    assertThat(claim.getUniqueFileNumber()).isEqualTo("220422/013");
  }

  @Test
  void stringToIntegerHandlesInvalidValues() {
    assertThat(mapper.stringToInteger("abc")).isNull();
    assertThat(mapper.stringToInteger(null)).isNull();
    assertThat(mapper.stringToInteger("7")).isEqualTo(7);
  }
}
