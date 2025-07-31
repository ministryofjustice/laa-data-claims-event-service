package uk.gov.justice.laa.bulk.claim.data.client.exceptions;

/** Claims Api Exception for 400 http status returned errors. */
public class ClaimsApiBadRequestException extends ClaimsApiClientException {
  public ClaimsApiBadRequestException(String errorMessage) {
    super("Bad request to /api/bulk-submissions: " + errorMessage);
  }
}
