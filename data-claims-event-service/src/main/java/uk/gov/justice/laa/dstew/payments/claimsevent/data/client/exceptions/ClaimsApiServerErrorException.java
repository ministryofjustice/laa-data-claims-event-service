package uk.gov.justice.laa.dstew.payments.claimsevent.data.client.exceptions;

import org.springframework.http.HttpStatusCode;

/** Claims API returned errors with 5XX http statuses. */
public class ClaimsApiServerErrorException extends ClaimsApiClientException {

  /**
   * Construct a {@code ClaimsApiServerErrorException} with information about the attempted request.
   *
   * @param errorMessage a message describing the error
   * @param method the method of the attempted request
   * @param endpoint the endpoint of the attempted request
   * @param httpStatus the http response from the claims data API
   */
  public ClaimsApiServerErrorException(
      String errorMessage, String method, String endpoint, HttpStatusCode httpStatus) {
    super(
        ERROR_MESSAGE_FORMAT.formatted(
            httpStatus == null ? "Server error" : httpStatus.value(),
            method,
            endpoint,
            errorMessage));
    this.httpStatus = httpStatus;
  }
}
