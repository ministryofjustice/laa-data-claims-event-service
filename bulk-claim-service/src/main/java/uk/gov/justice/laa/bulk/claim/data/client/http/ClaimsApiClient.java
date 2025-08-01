package uk.gov.justice.laa.bulk.claim.data.client.http;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.justice.laa.bulk.claim.data.client.dto.BulkSubmissionRequest;
import uk.gov.justice.laa.bulk.claim.data.client.dto.BulkSubmissionResponse;
import uk.gov.justice.laa.bulk.claim.data.client.exceptions.ClaimsApiBadRequestException;
import uk.gov.justice.laa.bulk.claim.data.client.exceptions.ClaimsApiClientException;
import uk.gov.justice.laa.bulk.claim.data.client.exceptions.ClaimsApiServerErrorException;
import uk.gov.justice.laa.bulk.claim.data.client.util.ValidationUtil;

/** laa-data-stewardship-claims-api Client. */
public class ClaimsApiClient implements BulkClaimsSubmissionApiClient {

  private final WebClient webClient;

  /**
   * Client constructor.
   *
   * @param webClient the webClient to use for requests to the claims api
   */
  public ClaimsApiClient(WebClient webClient) {
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
    try {
      ValidationUtil.validate(request);

      URI location =
          webClient
              .post()
              .uri("/api/bulk-submissions")
              .bodyValue(request)
              .retrieve()
              .onStatus(
                  HttpStatus.BAD_REQUEST::equals,
                  response ->
                      response.bodyToMono(String.class).map(ClaimsApiBadRequestException::new))
              .onStatus(
                  HttpStatus.INTERNAL_SERVER_ERROR::equals,
                  response ->
                      response.bodyToMono(String.class).map(ClaimsApiServerErrorException::new))
              .toBodilessEntity()
              .map(response -> response.getHeaders().getLocation())
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

    } catch (Exception e) {
      throw new ClaimsApiClientException("Submission failed", e);
    }
  }
}
