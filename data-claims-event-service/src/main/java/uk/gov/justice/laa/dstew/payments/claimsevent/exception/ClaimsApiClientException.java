package uk.gov.justice.laa.dstew.payments.claimsevent.exception;

import org.springframework.http.HttpStatusCode;

/** Claims Api Client exceptions wrapper. */
public class ClaimsApiClientException extends RuntimeException {

  protected HttpStatusCode httpStatus;
  protected static final String ERROR_MESSAGE_FORMAT = "%s response from %s %s: %s";

  public ClaimsApiClientException(String message) {
    super(message);
  }

  public ClaimsApiClientException(String message, Throwable cause) {
    super(message, cause);
  }

  public ClaimsApiClientException(String message, HttpStatusCode httpStatus) {
    super(message);
    this.httpStatus = httpStatus;
  }

  public HttpStatusCode getHttpStatus() {
    return httpStatus;
  }
}
