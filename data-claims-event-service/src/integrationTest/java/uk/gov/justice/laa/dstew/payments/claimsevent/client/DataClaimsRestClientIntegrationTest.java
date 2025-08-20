package uk.gov.justice.laa.dstew.payments.claimsevent.client;

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
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockserver.model.Header;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.claims.model.ClaimDto;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.ClaimsApiClientException;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.ClaimsApiServerErrorException;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimMatterStarts;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimOffice;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimOutcome;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimSchedule;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimSubmission;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.dto.BulkSubmissionRequest;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.dto.BulkSubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.dto.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.dto.UpdateClaimRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DataClaimsRestClientIntegrationTest extends MockServerIntegrationTest {

  protected ClaimsService claimsService;

  public class BulkClaimsSubmissionApiClientIntegrationTest extends MockServerIntegrationTest {

    private ClaimsService claimsService;

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
      claimsService = new ClaimsRestService(createWebClient());
    }

    @Nested
    @DisplayName("GET: /api/claims/{claimId} tests")
    class GetClaimTests {

      @Test
      @DisplayName("Should handle 200 response")
      void shouldHandle200Response() throws Exception {
        // Given
        String claimId = "123456789";
        String expectedJson = readJsonFromFile("get-claim-openapi-200.json");
        mockServerClient
            .when(request().withMethod("GET").withPath("/api/claims/" + claimId))
            .respond(
                response()
                    .withStatusCode(200)
                    .withHeader("Content-Type", "application/json")
                    // TODO: Update this file to contain actual response when open API spec is
                    //  updated
                    .withBody(expectedJson));
        // When
        Mono<ClaimDto> result = claimsService.getClaim(claimId);
        // Then
        Optional<ClaimDto> claimDto = result.blockOptional();
        assertThat(claimDto).isNotNull();
        assertThat(claimDto).isPresent();
        String resultJson = objectMapper.writeValueAsString(claimDto.get());
        assertThatJsonMatches(expectedJson, resultJson);
      }

      @Test
      @DisplayName("Should handle 404 response")
      void shouldHandle404Response() {
        // Given
        String claimId = "123456789";
        mockServerClient
            .when(request().withMethod("GET").withPath("/api/claims/" + claimId))
            .respond(response().withStatusCode(404));
        // When
        Mono<ClaimDto> result = claimsService.getClaim(claimId);
        // Then
        Optional<ClaimDto> claimDto = result.blockOptional();
        assertThat(claimDto).isNotNull();
        assertThat(claimDto).isEmpty();
      }

      @Test
      @DisplayName("Should handle 401 response")
      void shouldHandle401Response() {
        // Given
        String claimId = "123456789";
        mockServerClient
            .when(request().withMethod("GET").withPath("/api/claims/" + claimId))
            .respond(response().withStatusCode(401));
        // When
        Mono<ClaimDto> result = claimsService.getClaim(claimId);
        // Then
        ClaimsApiServerErrorException exception =
            assertThrows(ClaimsApiServerErrorException.class, result::block);
        assertThat(exception.getMessage())
            .isEqualTo("Server error from Claims API: 401 UNAUTHORIZED");
      }

      @Test
      @DisplayName("Should handle 403 response")
      void shouldHandle403Response() {
        // Given
        String claimId = "123456789";
        mockServerClient
            .when(request().withMethod("GET").withPath("/api/claims/" + claimId))
            .respond(response().withStatusCode(403));
        // When
        Mono<ClaimDto> result = claimsService.getClaim(claimId);
        // Then
        ClaimsApiServerErrorException exception =
            assertThrows(ClaimsApiServerErrorException.class, result::block);
        assertThat(exception.getMessage()).isEqualTo("Server error from Claims API: 403 FORBIDDEN");
      }

      @Test
      @DisplayName("Should handle 500 response")
      void shouldHandle500Response() {
        // Given
        String claimId = "123456789";
        mockServerClient
            .when(request().withMethod("GET").withPath("/api/claims/" + claimId))
            .respond(response().withStatusCode(500).withBody("Server error"));
        // When
        Mono<ClaimDto> result = claimsService.getClaim(claimId);
        // Then
        ClaimsApiServerErrorException exception =
            assertThrows(ClaimsApiServerErrorException.class, result::block);
        assertThat(exception.getMessage()).isEqualTo("Server error from Claims API: Server error");
      }
    }

    @Nested
    @DisplayName("POST: /api/bulk-submissions tests")
    class SubmitBulkClaimTests {

      @Test
      @DisplayName("Should return 201 status and return bulk claims upload response ID.")
      void testSubmitBulkClaimReturns() {
        // setup
        String submissionId = "abc123";
        String locationHeader = "/api/bulk-submissions/" + submissionId;

        mockServerClient
            .when(request().withMethod("POST").withPath("/api/bulk-submissions"))
            .respond(
                response().withStatusCode(201).withHeader(new Header("Location", locationHeader)));

        BulkSubmissionRequest request = getBulkSubmissionRequest();
        // execute
        BulkSubmissionResponse response = claimsService.submitBulkClaim(request);

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
                ConstraintViolationException.class, () -> claimsService.submitBulkClaim(request));

        assertThat(clientException.getMessage()).contains("userId: must not be null");
        assertThat(clientException.getMessage()).contains("submissions: must not be empty");
      }

      @Test
      @DisplayName("Should return Client API 400 exception with error message.")
      void submitBulkClaimRequestFails() throws ClaimsApiServerErrorException {
        // setup
        String errorMessage = "failed with user error";

        mockServerClient
            .when(request().withMethod("POST").withPath("/api/bulk-submissions"))
            .respond(response().withStatusCode(400).withBody(errorMessage));

        BulkSubmissionRequest request = getBulkSubmissionRequest();

        // Assert
        ClaimsApiServerErrorException clientException =
            assertThrows(
                ClaimsApiServerErrorException.class, () -> claimsService.submitBulkClaim(request));

        assertThat(clientException).isNotNull();
        assertThat(clientException.getMessage())
            .isEqualTo("400 response from POST /api/bulk-submissions: failed with user error");
      }

      @Test
      @DisplayName("Should return Client API 500 exception with error message.")
      void submitBulkClaimRequestFailsWithSeverError() throws ClaimsApiServerErrorException {
        // setup
        String errorMessage = "failed with server error";

        mockServerClient
            .when(request().withMethod("POST").withPath("/api/bulk-submissions"))
            .respond(response().withStatusCode(500).withBody(errorMessage));

        BulkSubmissionRequest request = getBulkSubmissionRequest();

        // Assert
        ClaimsApiServerErrorException serverException =
            assertThrows(
                ClaimsApiServerErrorException.class, () -> claimsService.submitBulkClaim(request));

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

        ResponseEntity<Void> response = claimsService.updateClaimStatus(request).block();

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

        Mono<ResponseEntity<Void>> response = claimsService.updateClaimStatus(request);

        assertThatThrownBy(response::block)
            .isInstanceOf(ClaimsApiServerErrorException.class)
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

        Mono<ResponseEntity<Void>> response = claimsService.updateClaimStatus(request);

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

        assertThatThrownBy(() -> claimsService.updateClaimStatus(request).block())
            .isInstanceOf(ConstraintViolationException.class)
            .isNotNull()
            .hasMessageContaining("claimStatus: must not be null");
      }
    }
  }
}
