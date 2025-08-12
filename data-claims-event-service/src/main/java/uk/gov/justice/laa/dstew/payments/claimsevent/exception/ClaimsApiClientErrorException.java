<<<<<<<< HEAD:data-claims-event-service/src/main/java/uk/gov/justice/laa/dstew/payments/claimsevent/data/client/exceptions/ClaimsApiClientErrorException.java
package uk.gov.justice.laa.dstew.payments.claimsevent.data.client.exceptions;
========
package uk.gov.justice.laa.bulk.claim.exception;
>>>>>>>> upstream/main:data-claims-event-service/src/main/java/uk/gov/justice/laa/dstew/payments/claimsevent/exception/ClaimsApiClientErrorException.java

import org.springframework.http.HttpStatusCode;

/** Claims Api Exception for 4XX http status returned errors. */
public class ClaimsApiClientErrorException extends ClaimsApiClientException {

  /**
   * Construct a {@code ClaimsApiClientErrorException} with information about the attempted request.
   *
   * @param errorMessage a message describing the error
   * @param method the method of the attempted request
   * @param endpoint the endpoint of the attempted request
   * @param httpStatus the http response from the claims data API
   */
  public ClaimsApiClientErrorException(
      String errorMessage, String method, String endpoint, HttpStatusCode httpStatus) {
    super(
        ERROR_MESSAGE_FORMAT.formatted(
            httpStatus == null ? "Client error" : httpStatus.value(),
            method,
            endpoint,
            errorMessage));
    this.httpStatus = httpStatus;
  }
}
