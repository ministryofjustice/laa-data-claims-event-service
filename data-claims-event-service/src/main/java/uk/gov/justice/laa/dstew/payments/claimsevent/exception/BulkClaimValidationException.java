package uk.gov.justice.laa.dstew.payments.claimsevent.exception;

/** Exception for issues when the uploaded file is the wrong file type. */
public class BulkClaimValidationException extends RuntimeException {

  public BulkClaimValidationException(String message) {
    super(message);
  }

  public BulkClaimValidationException(String message, Exception cause) {
    super(message, cause);
  }
}
