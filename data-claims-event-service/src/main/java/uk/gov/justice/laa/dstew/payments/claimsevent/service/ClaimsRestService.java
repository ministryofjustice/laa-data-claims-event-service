package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.net.URI;
import java.util.function.Function;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.claims.model.ClaimDto;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.dto.BulkSubmissionRequest;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.dto.BulkSubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.dto.UpdateClaimRequest;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.ClaimsApiClientErrorException;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.ClaimsApiClientException;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.ClaimsApiServerErrorException;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.ValidationUtil;

/** laa-data-stewardship-claims-api Client. */
public class ClaimsRestService implements ClaimsService {

  private final WebClient webClient;

  private static final String SUBMISSIONS_ENDPOINT = "/api/bulk-submissions";

  /**
   * Client constructor.
   *
   * @param webClient the webClient to use for requests to the claims api
   */
  public ClaimsRestService(WebClient webClient) {
    this.webClient = webClient;
  }

  /**
   * POST : /api/bulk-submissions internal request.
   *
   * @param request BulkSubmissionsRequest
   * @return BulkSubmissionResponse
   */
  @Override
  public BulkSubmissionResponse submitBulkClaim(BulkSubmissionRequest request) {
    ValidationUtil.validate(request);

    URI location =
        webClient
            .post()
            .uri(SUBMISSIONS_ENDPOINT)
            .bodyValue(request)
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                handleErrorResponse(HttpMethod.POST.name(), SUBMISSIONS_ENDPOINT))
            .toBodilessEntity()
            .mapNotNull(response -> response.getHeaders().getLocation())
            .block();

    if (location == null || !StringUtils.hasText(location.getPath())) {
      throw new ClaimsApiClientException("Location header missing or malformed");
    }

    String[] pathParts = location.getPath().split("/");
    String submissionId = pathParts[pathParts.length - 1];
    if (!StringUtils.hasText(submissionId)) {
      throw new ClaimsApiClientException("Submission ID missing from location header");
    }

    return new BulkSubmissionResponse(submissionId);
  }

  @Override
  public Mono<ResponseEntity<Void>> updateClaimStatus(UpdateClaimRequest request) {
    ValidationUtil.validate(request);
    return webClient
        .patch()
        .uri(SUBMISSIONS_ENDPOINT)
        .bodyValue(request)
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            handleErrorResponse(HttpMethod.PATCH.name(), SUBMISSIONS_ENDPOINT))
        .toBodilessEntity();
  }

  private Function<ClientResponse, Mono<? extends Throwable>> handleErrorResponse(
      String method, String endpoint) {
    return response ->
        response
            .bodyToMono(String.class)
            .defaultIfEmpty("Unknown error")
            .map(
                r -> {
                  if (response.statusCode().is4xxClientError()) {
                    return new ClaimsApiClientErrorException(
                        r, method, endpoint, response.statusCode());
                  } else if (response.statusCode().is5xxServerError()) {
                    return new ClaimsApiServerErrorException(
                        r, method, endpoint, response.statusCode());
                  } else {
                    return new ClaimsApiClientException(
                        "Unknown error response from %s %s: %s".formatted(method, endpoint, r),
                        response.statusCode());
                  }
                });
  }

  /**
   * Retrieves a claim by its unique identifier from the Claims API.
   *
   * @param claimId the unique identifier of the claim to be retrieved
   * @return a {@link Mono} containing the {@link ClaimDto} object representing the claim, or an
   *     empty {@link Mono} if the claim is not found.
   * @throws ClaimsApiServerErrorException if the Claims API returns a server error (5xx status
   *     code)
   */
  @Override
  public Mono<ClaimDto> getClaim(String claimId) {
    return webClient
        .get()
        .uri("/api/claims/{claimId}", claimId)
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            handleErrorResponse(HttpMethod.POST.name(), "/api/claims/{claimId}"))
        .bodyToMono(ClaimDto.class)
        // This ensures any error is directly propagated downstream in case of unexpected scenarios
        .onErrorResume(ClaimsApiServerErrorException.class, Mono::error);
  }
}
