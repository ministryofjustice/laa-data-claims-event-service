package uk.gov.justice.laa.dstew.payments.claimsevent.service.claim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockserver.model.Header;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.ClaimsApiClientErrorException;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.ClaimsApiClientException;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.ClaimsApiServerErrorException;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimMatterStarts;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimOffice;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimOutcome;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimSchedule;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimSubmission;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.ClaimsRestService;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.dto.BulkSubmissionRequest;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.dto.BulkSubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.dto.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.dto.UpdateClaimRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BulkClaimsSubmissionApiClientIntegrationTest extends MockServerIntegrationTest {
  private ClaimsRestService claimsRestService;

  private static @NotNull BulkSubmissionRequest getBulkSubmissionRequest() {
    List<BulkClaimOutcome> bulkClaimOutcomes = new ArrayList<>();
    List<BulkClaimMatterStarts> bulkClaimMatterStarts = new ArrayList<>();

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
    claimsRestService = new ClaimsRestService(createWebClient());
  }

  @Nested
  @DisplayName("POST: /api/bulk-submissions")
  class PostBulkSubmissions {

    @Test
    @DisplayName("Should return 201 status and return bulk claims upload response ID.")
    void submitBulkClaimReturns() {
      // setup
      String submissionId = "abc123";
      String locationHeader = "/api/bulk-submissions/" + submissionId;

      mockServerClient
          .when(request().withMethod("POST").withPath("/api/bulk-submissions"))
          .respond(
              response().withStatusCode(201).withHeader(new Header("Location", locationHeader)));

      BulkSubmissionRequest request = getBulkSubmissionRequest();
      // execute
      BulkSubmissionResponse response = claimsRestService.submitBulkClaim(request);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.submissionId()).isEqualTo(submissionId);
    }

    @Test
    @DisplayName("Should return Client API exception with error message.")
    void submitBulkClaimRequestValidationFails() throws ClaimsApiClientException {
      // setup
      BulkSubmissionRequest request = new BulkSubmissionRequest(null, null, null);

      // Assert
      ConstraintViolationException clientException =
          assertThrows(
              ConstraintViolationException.class, () -> claimsRestService.submitBulkClaim(request));

      assertThat(clientException.getMessage()).contains("userId: must not be null");
      assertThat(clientException.getMessage()).contains("submissions: must not be empty");
    }

    @Test
    @DisplayName("Should return Client API 400 exception with error message.")
    void submitBulkClaimRequestFails() throws ClaimsApiClientErrorException {
      // setup
      String errorMessage = "failed with user error";

      mockServerClient
          .when(request().withMethod("POST").withPath("/api/bulk-submissions"))
          .respond(response().withStatusCode(400).withBody(errorMessage));

      BulkSubmissionRequest request = getBulkSubmissionRequest();

      // Assert
      ClaimsApiClientErrorException clientException =
          assertThrows(
              ClaimsApiClientErrorException.class,
              () -> claimsRestService.submitBulkClaim(request));

      assertThat(clientException).isNotNull();
      assertThat(clientException.getMessage())
          .isEqualTo("400 response from POST /api/bulk-submissions: failed with user error");
    }

    @Test
    @DisplayName("Should return Client API 500 exception with error message.")
    void submitBulkClaimRequestFailsWithSeverError() throws ClaimsApiClientErrorException {
      // setup
      String errorMessage = "failed with server error";

      mockServerClient
          .when(request().withMethod("POST").withPath("/api/bulk-submissions"))
          .respond(response().withStatusCode(500).withBody(errorMessage));

      BulkSubmissionRequest request = getBulkSubmissionRequest();

      // Assert
      ClaimsApiServerErrorException serverException =
          assertThrows(
              ClaimsApiServerErrorException.class,
              () -> claimsRestService.submitBulkClaim(request));

      assertThat(serverException).isNotNull();
      assertThat(serverException.getMessage())
          .isEqualTo("500 response from POST /api/bulk-submissions: failed with server error");
    }
  }

  @Nested
  @DisplayName("PATCH: /api/bulk-submissions")
  class PatchBulkSubmissions {

    @ParameterizedTest
    @ValueSource(ints = {200, 201, 202, 203, 204, 205})
    @DisplayName("Should return status code for successful requests")
    void returnsStatusCodeForSuccessfulRequests(int code) {
      UpdateClaimRequest request = new UpdateClaimRequest(ClaimStatus.VALID, List.of());

      mockServerClient
          .when(request().withMethod("PATCH").withPath("/api/bulk-submissions"))
          .respond(response().withStatusCode(code));

      ResponseEntity<Void> response = claimsRestService.updateClaimStatus(request).block();

      assertNotNull(response);
      assertEquals(code, response.getStatusCode().value());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 402, 403, 404, 405})
    @DisplayName("Should throw client error exception for 4XX responses")
    void throwsClientErrorExceptionFor4XXResponses(int code) {
      UpdateClaimRequest request = new UpdateClaimRequest(ClaimStatus.VALID, List.of());

      mockServerClient
          .when(request().withMethod("PATCH").withPath("/api/bulk-submissions"))
          .respond(response().withStatusCode(code).withBody("client error"));

      Mono<ResponseEntity<Void>> response = claimsRestService.updateClaimStatus(request);

      assertThatThrownBy(response::block)
          .isInstanceOf(ClaimsApiClientErrorException.class)
          .isNotNull()
          .hasMessageContaining(
              "%s response from PATCH /api/bulk-submissions: client error".formatted(code))
          .hasFieldOrPropertyWithValue("httpStatus", HttpStatusCode.valueOf(code));
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 501, 502, 503, 504, 505})
    @DisplayName("Should throw server error exception for 5XX responses")
    void throwsServerErrorExceptionFor5XXResponses(int code) {
      UpdateClaimRequest request = new UpdateClaimRequest(ClaimStatus.VALID, List.of());

      mockServerClient
          .when(request().withMethod("PATCH").withPath("/api/bulk-submissions"))
          .respond(response().withStatusCode(code).withBody("server error"));

      Mono<ResponseEntity<Void>> response = claimsRestService.updateClaimStatus(request);

      assertThatThrownBy(response::block)
          .isInstanceOf(ClaimsApiServerErrorException.class)
          .isNotNull()
          .hasMessageContaining(
              "%s response from PATCH /api/bulk-submissions: server error".formatted(code))
          .hasFieldOrPropertyWithValue("httpStatus", HttpStatusCode.valueOf(code));
    }

    @Test
    @DisplayName("Should throw validation exception for invalid requests")
    void throwsValidationExceptionForInvalidRequests() {
      UpdateClaimRequest request = new UpdateClaimRequest(null, List.of());

      assertThatThrownBy(() -> claimsRestService.updateClaimStatus(request).block())
          .isInstanceOf(ConstraintViolationException.class)
          .isNotNull()
          .hasMessageContaining("claimStatus: must not be null");
    }
  }
}
