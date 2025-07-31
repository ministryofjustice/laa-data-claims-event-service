package uk.gov.justice.laa.bulk.claim.data.client.exceptions;

/** Claims API returned errors with 500 http statuses. */
public class ClaimsApiServerErrorException extends ClaimsApiClientException {
  public ClaimsApiServerErrorException(String errorMessage) {
    super("Server error from /api/bulk-submissions: " + errorMessage);
  }
}
