package uk.gov.justice.laa.dstew.payments.claimsevent.exception;

/** Exception for issues when attempting to read and map a bulk claim submission file. */
public class BulkClaimFileReadException extends RuntimeException {

  public BulkClaimFileReadException(String message) {
    super(message);
  }

  public BulkClaimFileReadException(String message, Exception cause) {
    super(message, cause);
  }
}
