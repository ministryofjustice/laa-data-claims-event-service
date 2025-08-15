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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkSubmissionResponse;

class BulkSubmissionMapperTest {

  private BulkSubmissionMapper mapper;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mapper = new BulkSubmissionMapperImpl();
    objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }


  @Test
  void shouldMapBulkSubmissionResponseToSubmissionPost() throws IOException {
    // Arrange
    String json = Files.readString(
        Path.of("src/test/resources/bulk-submission-response.json")
    );

    BulkSubmissionResponse bulkSubmission =
        objectMapper.readValue(json, BulkSubmissionResponse.class);

    UUID submissionId = UUID.randomUUID();


    SubmissionPost result = mapper.mapToSubmissionPost(bulkSubmission, submissionId);

    assertThat(result).isNotNull();
    assertThat(result.getSubmissionId()).isEqualTo(submissionId);
    assertThat(result.getBulkSubmissionId()).isEqualTo(UUID.fromString("c4ef2c36-35dc-4627-b5d8-7db10d86be3d"));
    assertThat(result.getNumberOfClaims()).isEqualTo(1);
    assertThat(result.getIsNilSubmission()).isFalse();
    assertThat(result.getOfficeAccountNumber()).isEqualTo("2Q286D");
    assertThat(result.getStatus()).isEqualTo(SubmissionStatus.CREATED); // or enum value depending on type
  }
}
