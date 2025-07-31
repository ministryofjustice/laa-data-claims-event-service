package uk.gov.justice.laa.bulk.claim.service.claim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.mockserver.model.Header;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.justice.laa.bulk.claim.data.client.dto.BulkSubmissionRequest;
import uk.gov.justice.laa.bulk.claim.data.client.dto.BulkSubmissionResponse;
import uk.gov.justice.laa.bulk.claim.data.client.exceptions.ClaimsApiBadRequestException;
import uk.gov.justice.laa.bulk.claim.data.client.exceptions.ClaimsApiClientException;
import uk.gov.justice.laa.bulk.claim.data.client.exceptions.ClaimsApiServerErrorException;
import uk.gov.justice.laa.bulk.claim.data.client.http.BulkClaimsSubmissionApiClient;
import uk.gov.justice.laa.bulk.claim.data.client.http.ClaimsApiClient;
import uk.gov.justice.laa.bulk.claim.helper.MockServerIntegrationTest;
import uk.gov.justice.laa.bulk.claim.model.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled
public class BulkClaimsSubmissionApiClientIntegrationTest extends MockServerIntegrationTest {
  protected BulkClaimsSubmissionApiClient bulkClaimsSubmissionApiClient;

  @AfterAll
  static void cleanup() {
    mockServerClient.stop();
  }

  private static @NotNull BulkSubmissionRequest getBulkSubmissionRequest() {
    List<BulkClaimOutcome> bulkClaimOutcomes = new java.util.ArrayList<>();
    List<BulkClaimMatterStarts> bulkClaimMatterStarts = new java.util.ArrayList<>();

    return new BulkSubmissionRequest(
        "123",
        null,
        List.of(
            new BulkClaimSubmission(
                new BulkClaimOffice("office_account"),
                new BulkClaimSchedule("submission_period", "area_of_law", "00"),
                bulkClaimOutcomes,
                bulkClaimMatterStarts)));
  }

  @BeforeEach
  void setUp() {
    String baseUrl =
        "http://" + mockServerContainer.getHost() + ":" + mockServerContainer.getServerPort();

    bulkClaimsSubmissionApiClient = new ClaimsApiClient(baseUrl);
  }

  @Test
  @DisplayName("Should return 201 status and return bulk claims upload response ID.")
  void testSubmitBulkClaimReturns() {
    // setup
    String submissionId = "abc123";
    String locationHeader = "/api/bulk-submissions/" + submissionId;

    mockServerClient
        .when(request().withMethod("POST").withPath("/api/bulk-submissions"))
        .respond(response().withStatusCode(201).withHeader(new Header("Location", locationHeader)));

    BulkSubmissionRequest request = getBulkSubmissionRequest();
    // execute
    BulkSubmissionResponse response = bulkClaimsSubmissionApiClient.submitBulkClaim(request);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.submissionId()).isEqualTo(submissionId);
  }

  @Test
  @DisplayName("Should return Client API exception with error message.")
  void testSubmitBulkClaimRequestValidationFails() throws ClaimsApiClientException {
    // setup
    BulkSubmissionRequest request = new BulkSubmissionRequest(null, null, null);

    // Assert
    ClaimsApiClientException clientException =
        assertThrows(
            ClaimsApiClientException.class,
            () -> bulkClaimsSubmissionApiClient.submitBulkClaim(request));

    assertThat(clientException.getMessage()).isEqualTo("Submission failed");
    assertThat(clientException.getCause().getMessage()).contains("userId: must not be null");
    assertThat(clientException.getCause().getMessage()).contains("submissions: must not be empty");
  }

  @Test
  @DisplayName("Should return Client API 400 exception with error message.")
  void testSubmitBulkClaimRequestFails() throws ClaimsApiBadRequestException {
    // setup
    String errorMessage = "failed with user error";

    mockServerClient
        .when(request().withMethod("POST").withPath("/api/bulk-submissions"))
        .respond(response().withStatusCode(400).withBody(errorMessage));

    BulkSubmissionRequest request = getBulkSubmissionRequest();
    ;

    // Assert
    ClaimsApiClientException clientException =
        assertThrows(
            ClaimsApiClientException.class,
            () -> bulkClaimsSubmissionApiClient.submitBulkClaim(request));

    assertThat(clientException.getMessage()).isEqualTo("Submission failed");
    assertThat(clientException).hasCauseInstanceOf(ClaimsApiBadRequestException.class);
    assertThat(clientException.getCause().getMessage()).contains(errorMessage);
  }

  @Test
  @DisplayName("Should return Client API 500 exception with error message.")
  void testSubmitBulkClaimRequestFailsWithSeverError() throws ClaimsApiBadRequestException {
    // setup
    String errorMessage = "failed with sever error";

    mockServerClient
        .when(request().withMethod("POST").withPath("/api/bulk-submissions"))
        .respond(response().withStatusCode(500).withBody(errorMessage));

    BulkSubmissionRequest request = getBulkSubmissionRequest();
    ;

    // Assert
    ClaimsApiClientException clientException =
        assertThrows(
            ClaimsApiClientException.class,
            () -> bulkClaimsSubmissionApiClient.submitBulkClaim(request));

    assertThat(clientException.getMessage()).isEqualTo("Submission failed");
    assertThat(clientException).hasCauseInstanceOf(ClaimsApiServerErrorException.class);
    assertThat(clientException.getCause().getMessage()).contains(errorMessage);
  }
}
