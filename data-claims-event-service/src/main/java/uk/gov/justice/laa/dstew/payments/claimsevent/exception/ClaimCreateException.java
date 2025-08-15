package uk.gov.justice.laa.dstew.payments.claimsevent.exception;

public class ClaimCreateException extends RuntimeException {
  public ClaimCreateException(String message) {
    super(message);
  }
  public ClaimCreateException(String message, Throwable cause) {
    super(message, cause);
  }
}
