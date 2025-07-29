package uk.gov.justice.laa.bulk.claim.data.client.exceptions;

/** Claims Api Client exceptions wrapper. */
public class ClaimsApiClientException extends RuntimeException {
  public ClaimsApiClientException(String message) {
    super(message);
  }

  public ClaimsApiClientException(String message, Throwable cause) {
    super(message, cause);
  }
}
